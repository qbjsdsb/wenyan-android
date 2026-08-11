package com.wenyan.app.core.data.repository

import androidx.room.withTransaction
import com.wenyan.app.core.data.mapper.MemoRecordMapper
import com.wenyan.app.core.data.mapper.LearningUnitRecordMapper
import com.wenyan.app.core.data.mapper.WrongAnswerSchedulingMapper
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.dao.WrongAnswerDao
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.LearningUnitRecordEntity
import com.wenyan.app.core.database.entity.ReviewLogEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.fsrs.FsrsWrapper
import com.wenyan.app.core.fsrs.MemoryTier
import com.wenyan.app.core.fsrs.Rating
import com.wenyan.app.core.fsrs.ReviewLog
import com.wenyan.app.core.fsrs.TIER_CONFIGS
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调度仓库（阶段3：FSRS调度接通）。
 *
 * 桥接 UI 卡片评分与 FSRS 调度算法，是评分调度的核心入口：
 *
 * 流程：UI 评分 → 读 MemoRecord → 转 FlashCard → 按 tier 构造 FsrsWrapper
 *      → schedule() → 转 MemoRecord → upsert 回数据库
 *
 * 设计决策（阶段3）：
 * - FsrsWrapper 不通过 Hilt DI 注入，而是按 tier 动态构造（无状态纯算法，构造成本极低）
 * - tier 由 CardTemplateType 推断（6种卡片类型 → 3档记忆强度）
 * - enableFuzz 在 TIER_EXACT 档关闭（精确到天），其他档开启（避免间隔过于规律）
 *
 * NF-B / P0-E4 修复：注入 [ClockGuard] 检测时钟回拨。
 * - [rateCard] 用 [ClockGuard.effectiveNowMillis] 替代 `LocalDateTime.now()` / `System.currentTimeMillis()`
 * - 用户改系统时间（回拨 > 1 分钟）时，ClockGuard 返回 lastKnown 而非 current，
 *   避免 FSRS 误判卡片"刚复习过"（间隔异常短）或"已过期很久"（间隔异常长）。
 *
 * P0-2 修复：注入 [WenyanDatabase] 用 [withTransaction] 包裹 memo_records 与 review_logs
 * 两步写入。原实现两步独立写入，memo upsert 成功后 review_log insert 失败（磁盘满 /
 * SQLite 锁竞争 / 约束错误）会导致数据不一致，AntiRoteMemorization 读取 review_logs
 * 计算连续正确次数会拿到不完整数据，功能失效。
 *
 * NF-PP4 修复：移除 memo_records.history JSON 双写，复习历史统一由 review_logs 表维护。
 *
 * @property memoRecordDao 背诵记录 DAO（读写 memo_records 表）
 * @property clockGuard 时钟守卫（检测回拨，返回单调不减的有效时间戳）
 */
/**
 * 调度仓库接口(NF-PP5 Wave 3.2 提取,便于测试替换)。
 *
 * 桥接 UI 卡片评分与 FSRS 算法。生产实现见 [SchedulingRepositoryImpl]。
 *
 * v0.8.6 新增 [previewIntervals]:
 * - 用于 UI 评分按钮显示"1分钟 / 6天 / 12天"预期间隔(Anki 标配)
 * - 不写入数据库,纯计算,可频繁调用
 *
 * @see SchedulingRepositoryImpl
 */
interface SchedulingRepository {

    /**
     * 评分调度:根据用户评分更新知识点的 FSRS 调度状态。
     *
     * @param pointId  知识点 ID(对应 memo_records.point_id 主键)
     * @param rating   用户评分(AGAIN/HARD/GOOD/EASY)
     * @param cardType 卡片模板类型(用于推断 tier,决定 FSRS 参数)
     *
     * @return 更新后的 MemoRecordEntity(已写入数据库),调用方可读取 nextReviewAt 等
     */
    suspend fun rateCard(
        pointId: String,
        rating: Rating,
        cardType: CardTemplateType,
    ): MemoRecordEntity?

    /**
     * 预览 4 种评分(Again/Hard/Good/Easy)的调度结果,不写入数据库。
     *
     * v0.8.6 新增:用于 UI 评分按钮显示预期间隔(参考 Anki "10m / 4d / 8d" 设计),
     * 让用户在评分前理解每个评分的后果,建立 FSRS 心智模型。
     *
     * 实现要点:
     * - 读取当前知识点的 MemoRecord(不存在则用默认 NEW 状态)
     * - 按 cardType 推断 tier,构造 FsrsWrapper
     * - 调用 FsrsWrapper.repeat() 一次性得到 4 种评分的 FlashCard
     * - 从 FlashCard.scheduledDays + dueDate 提取间隔并格式化为友好文本
     *
     * 性能:每次进入新卡片调用一次,只读不写,DB 查询 1 次,可接受。
     *
     * @param pointId  知识点 ID
     * @param cardType 卡片模板类型(用于推断 tier)
     * @return 4 种评分对应的预览信息(键为 Rating)
     */
    suspend fun previewIntervals(
        pointId: String,
        cardType: CardTemplateType,
    ): Map<Rating, IntervalPreview>

    suspend fun rateLearningUnit(
        pointId: String,
        unitId: String,
        rating: Rating,
        cardType: CardTemplateType,
    ): UnitRatingReceipt? = null

    suspend fun previewLearningUnitIntervals(
        unitId: String,
        cardType: CardTemplateType,
    ): Map<Rating, IntervalPreview> = emptyMap()

    suspend fun undoLearningUnitRating(receipt: UnitRatingReceipt): Boolean = false

    /**
     * 错题评分调度：根据用户评分更新错题的 FSRS 调度状态（v0.9.4 新增）。
     *
     * 与 [rateCard] 类似，但操作对象是 wrong_answers 表的 sched_* 字段，
     * 而非 memo_records 表。使用 TIER_FRAMEWORK 档位（R_target=0.90）。
     *
     * 流程：
     * 1. 读取 WrongAnswerEntity（含 sched_* 字段）
     * 2. 转 FlashCard（用 [WrongAnswerSchedulingMapper]）
     * 3. 构造 FsrsWrapper（TIER_FRAMEWORK，enableFuzz=true）
     * 4. schedule() → 转回 sched_* 字段 → updateScheduling 写回 DB
     *
     * @param wrongAnswerId 错题 ID
     * @param rating        用户评分（AGAIN/HARD/GOOD/EASY）
     * @return 更新后的 WrongAnswerEntity（sched_* 字段已更新），或 null（错题不存在）
     */
    suspend fun rateWrongAnswer(
        wrongAnswerId: String,
        rating: Rating,
    ): WrongAnswerEntity?
}

data class UnitRatingReceipt(
    val pointId: String,
    val unitId: String,
    val before: LearningUnitRecordEntity,
    val updated: LearningUnitRecordEntity,
    val reviewLogId: String,
)

/**
 * 评分预览信息(v0.8.6 新增)。
 *
 * 表示"如果用户评 X 档,卡片将在多久后再次出现"。
 *
 * @property rating 评分等级
 * @property scheduledDays 调度间隔(天)。0 表示学习阶段(分钟级),>0 表示 N 天后
 * @property intervalMillis 实际间隔(毫秒,学习阶段精确到分钟)
 * @property displayText 友好显示文本,如"1分钟" / "6天" / "1个月" / "明天"
 */
data class IntervalPreview(
    val rating: Rating,
    val scheduledDays: Int,
    val intervalMillis: Long,
    val displayText: String,
)

@Singleton
class SchedulingRepositoryImpl @Inject constructor(
    private val database: WenyanDatabase,
    private val memoRecordDao: MemoRecordDao,
    private val reviewLogDao: ReviewLogDao,
    private val clockGuard: ClockGuard,
    private val wrongAnswerDao: WrongAnswerDao,
) : SchedulingRepository {
    /**
     * 评分调度：根据用户评分更新知识点的 FSRS 调度状态。
     *
     * @param pointId  知识点 ID（对应 memo_records.point_id 主键）
     * @param rating   用户评分（AGAIN/HARD/GOOD/EASY）
     * @param cardType 卡片模板类型（用于推断 tier，决定 FSRS 参数）
     *
     * @return 更新后的 MemoRecordEntity（已写入数据库），调用方可读取 nextReviewAt 等
     */
    override suspend fun rateCard(
        pointId: String,
        rating: Rating,
        cardType: CardTemplateType,
    ): MemoRecordEntity? {
        if (pointId.isBlank()) return null

        // 1. 读取现有 MemoRecord（不存在则创建默认记录）
        val existingMemo = memoRecordDao.getById(pointId) ?: createDefaultMemoRecord(pointId)

        // 2. 按 tier 构造 FsrsWrapper（无状态，每次按需构造）
        val tier = mapCardTypeToTier(cardType)
        // P0-T3 修正：原 `TIER_CONFIGS[MemoryTier.TIER_FRAMEWORK]!!` 用 !!，
        // 改用 getValue 抛 IllegalArgumentException（含 key 名）而非 NPE，便于排查。
        val config = TIER_CONFIGS[tier] ?: TIER_CONFIGS.getValue(MemoryTier.TIER_FRAMEWORK)
        val wrapper = FsrsWrapper(
            requestRetention = config.targetRetention,
            maximumInterval = config.maxInterval,
            enableFuzz = tier != MemoryTier.TIER_EXACT,
            stabilityGrowthFactor = config.stabilityGrowthFactor,
            easyBonus = config.easyBonus,
            againPenalty = config.againPenalty,
        )

        // 3. MemoRecord → FlashCard（调度前状态）
        // NF-B 修复：用 ClockGuard.effectiveNowMillis() 替代 LocalDateTime.now()，
        // 检测时钟回拨避免 FSRS 误判。LocalDateTime 由有效 millis 转换。
        val nowMillis = clockGuard.effectiveNowMillis()
        val now = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowMillis),
            ZoneId.systemDefault(),
        )
        val flashCardBefore = MemoRecordMapper.toFlashCard(existingMemo)

        // 4. FSRS 调度
        val flashCardAfter = wrapper.schedule(flashCardBefore, rating, now)

        // 5. 构造 ReviewLog（调度前状态 + 调度后 scheduledDays）
        val reviewLog = ReviewLog(
            rating = rating,
            state = flashCardBefore.state,
            dueDate = flashCardBefore.dueDate,
            stability = flashCardBefore.stability,
            difficulty = flashCardBefore.difficulty,
            // P0-AUDIT-1 修正：原用 flashCardBefore.elapsedDays（数据库旧值，即上次评分距上上次的间隔），
            // 应取 flashCardAfter.elapsedDays（FsrsWrapper.scheduleInternal 本次计算的真实间隔：
            // ChronoUnit.DAYS.between(lastReview, now)）。
            // 影响：review_logs.elapsed_days 记录错误，未来 FSRS 参数优化训练数据不可信。
            elapsedDays = flashCardAfter.elapsedDays,
            lastElapsedDays = flashCardBefore.scheduledDays,
            scheduledDays = flashCardAfter.scheduledDays,
            reviewTime = now,
        )

        // 6. FlashCard → MemoRecord（调度后状态）+ 持久化
        // NF-PP4 修复：不再写 history JSON，复习历史统一由 review_logs 表维护。
        val updatedMemo = MemoRecordMapper.toMemoRecord(
            flashCard = flashCardAfter,
            pointId = pointId,
            inPriorityQueue = existingMemo.inPriorityQueue != 0,
        )

        // P0-2 修复：memo_records 与 review_logs 两步写入包进 withTransaction，
        // 要么全成要么全败。事务失败时向上抛异常，UI 层可提示用户重试。
        database.withTransaction {
            memoRecordDao.upsert(updatedMemo)
            reviewLogDao.insert(
                ReviewLogEntity(
                    id = UUID.randomUUID().toString(),
                    pointId = pointId,
                    rating = rating.name,
                    elapsedDays = reviewLog.elapsedDays,
                    scheduledDays = reviewLog.scheduledDays,
                    state = reviewLog.state.name,
                    stability = reviewLog.stability,
                    difficulty = reviewLog.difficulty,
                    reps = flashCardAfter.reps,
                    createdAt = nowMillis,
                ),
            )
        }

        return updatedMemo
    }

    /**
     * 预览 4 种评分的调度结果(v0.8.6 新增)。
     *
     * 不写入数据库,纯计算。复用 [FsrsWrapper.repeat] 一次性得到 4 种评分的 FlashCard,
     * 从 dueDate 与 now 的差值提取实际间隔(精确到分钟,学习阶段),或从 scheduledDays 提取天数。
     *
     * 格式化规则(参考 Anki "10m / 4d / 8d" 简洁风格):
     * - scheduledDays == 0 且 intervalMillis < 1 小时 → "N分钟"
     * - scheduledDays == 0 且 intervalMillis < 1 天 → "N小时"
     * - scheduledDays == 1 → "明天"
     * - scheduledDays in 2..6 → "N天"
     * - scheduledDays in 7..29 → "N周" (scheduledDays / 7)
     * - scheduledDays in 30..364 → "N月" (scheduledDays / 30)
     * - scheduledDays >= 365 → "N年" (scheduledDays / 365)
     *
     * 边界:pointId 为空或 cardType 无效时返回空 Map,UI 不显示预览。
     */
    override suspend fun previewIntervals(
        pointId: String,
        cardType: CardTemplateType,
    ): Map<Rating, IntervalPreview> {
        if (pointId.isBlank()) return emptyMap()

        // 1. 读取现有 MemoRecord(不存在则用默认 NEW)
        val existingMemo = memoRecordDao.getById(pointId) ?: createDefaultMemoRecord(pointId)

        // 2. 按 tier 构造 FsrsWrapper(与 rateCard 完全一致,确保预览=实际)
        val tier = mapCardTypeToTier(cardType)
        val config = TIER_CONFIGS[tier] ?: TIER_CONFIGS.getValue(MemoryTier.TIER_FRAMEWORK)
        val wrapper = FsrsWrapper(
            requestRetention = config.targetRetention,
            maximumInterval = config.maxInterval,
            enableFuzz = tier != MemoryTier.TIER_EXACT,
            stabilityGrowthFactor = config.stabilityGrowthFactor,
            easyBonus = config.easyBonus,
            againPenalty = config.againPenalty,
        )

        // 3. 转 FlashCard 并预览 4 档
        val nowMillis = clockGuard.effectiveNowMillis()
        val now = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowMillis),
            ZoneId.systemDefault(),
        )
        val flashCardBefore = MemoRecordMapper.toFlashCard(existingMemo)
        val previews = wrapper.repeat(flashCardBefore, now)

        // 4. 格式化为 IntervalPreview
        return previews.mapValues { (rating, scheduled) ->
            val intervalMillis = java.time.Duration.between(now, scheduled.dueDate).toMillis()
            IntervalPreview(
                rating = rating,
                scheduledDays = scheduled.scheduledDays,
                intervalMillis = intervalMillis,
                displayText = formatInterval(scheduled.scheduledDays, intervalMillis),
            )
        }
    }

    override suspend fun rateLearningUnit(
        pointId: String,
        unitId: String,
        rating: Rating,
        cardType: CardTemplateType,
    ): UnitRatingReceipt? {
        if (pointId.isBlank() || unitId.isBlank()) return null
        val unit = database.learningUnitDao().getById(unitId)
            ?.takeIf { it.pointId == pointId && it.active } ?: return null
        val recordDao = database.learningUnitRecordDao()
        val before = recordDao.getById(unit.id) ?: return null
        val nowMillis = clockGuard.effectiveNowMillis()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        val flashBefore = LearningUnitRecordMapper.toFlashCard(before)
        val flashAfter = wrapperFor(cardType).schedule(flashBefore, rating, now)
        val updated = LearningUnitRecordMapper.fromFlashCard(
            flashAfter,
            unit.id,
            before.inPriorityQueue != 0,
        )
        val logId = UUID.randomUUID().toString()
        database.withTransaction {
            recordDao.upsert(updated)
            reviewLogDao.insert(
                ReviewLogEntity(
                    id = logId,
                    pointId = pointId,
                    learningUnitId = unit.id,
                    rating = rating.name,
                    elapsedDays = flashAfter.elapsedDays,
                    scheduledDays = flashAfter.scheduledDays,
                    state = flashBefore.state.name,
                    stability = flashBefore.stability,
                    difficulty = flashBefore.difficulty,
                    reps = flashAfter.reps,
                    createdAt = nowMillis,
                ),
            )
        }
        return UnitRatingReceipt(pointId, unit.id, before, updated, logId)
    }

    override suspend fun previewLearningUnitIntervals(
        unitId: String,
        cardType: CardTemplateType,
    ): Map<Rating, IntervalPreview> {
        if (unitId.isBlank()) return emptyMap()
        val record = database.learningUnitRecordDao().getById(unitId) ?: return emptyMap()
        val nowMillis = clockGuard.effectiveNowMillis()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        return wrapperFor(cardType).repeat(LearningUnitRecordMapper.toFlashCard(record), now)
            .mapValues { (rating, scheduled) ->
                val intervalMillis = java.time.Duration.between(now, scheduled.dueDate).toMillis()
                IntervalPreview(rating, scheduled.scheduledDays, intervalMillis, formatInterval(scheduled.scheduledDays, intervalMillis))
            }
    }

    override suspend fun undoLearningUnitRating(receipt: UnitRatingReceipt): Boolean = database.withTransaction {
        val recordDao = database.learningUnitRecordDao()
        if (recordDao.getById(receipt.unitId) != receipt.updated) return@withTransaction false
        val log = reviewLogDao.getById(receipt.reviewLogId)
        if (log?.learningUnitId != receipt.unitId) return@withTransaction false
        recordDao.upsert(receipt.before)
        reviewLogDao.deleteById(receipt.reviewLogId)
        true
    }

    private fun wrapperFor(cardType: CardTemplateType): FsrsWrapper {
        val tier = mapCardTypeToTier(cardType)
        val config = TIER_CONFIGS[tier] ?: TIER_CONFIGS.getValue(MemoryTier.TIER_FRAMEWORK)
        return FsrsWrapper(
            requestRetention = config.targetRetention,
            maximumInterval = config.maxInterval,
            enableFuzz = tier != MemoryTier.TIER_EXACT,
            stabilityGrowthFactor = config.stabilityGrowthFactor,
            easyBonus = config.easyBonus,
            againPenalty = config.againPenalty,
        )
    }

    /**
     * 格式化间隔为友好文本(参考 Anki "10m / 4d / 8d" 简洁风格)。
     *
     * 学习阶段(scheduledDays==0)用分钟/小时,复习阶段用天/周/月/年。
     * 边界值用 coerceAtLeast(1) 防止"0分钟"。
     */
    private fun formatInterval(scheduledDays: Int, intervalMillis: Long): String {
        return when {
            scheduledDays == 0 && intervalMillis < 3_600_000L -> {
                "${(intervalMillis / 60_000L).coerceAtLeast(1L)}分钟"
            }
            scheduledDays == 0 && intervalMillis < 86_400_000L -> {
                "${(intervalMillis / 3_600_000L).coerceAtLeast(1L)}小时"
            }
            scheduledDays == 1 -> "明天"
            scheduledDays in 2..6 -> "${scheduledDays}天"
            scheduledDays in 7..29 -> "${scheduledDays / 7}周"
            scheduledDays in 30..364 -> "${scheduledDays / 30}个月"
            scheduledDays >= 365 -> "${scheduledDays / 365}年"
            else -> "${scheduledDays}天"
        }
    }

    /**
     * 卡片模板类型 → 记忆档位映射
     *
     * 设计依据（spec 第301-305行 + 设计文档3.3.4节）：
     * - TIER_EXACT（精确档，R_target=0.95）：名句填空，需逐字精确
     * - TIER_FRAMEWORK（框架档，R_target=0.90）：名词解释、作品作者、流派对照、区分卡
     * - TIER_UNDERSTAND（理解档，R_target=0.85）：论述要点，重在思路而非原文
     */
    private fun mapCardTypeToTier(cardType: CardTemplateType): MemoryTier = when (cardType) {
        CardTemplateType.CLOZE_QUOTE -> MemoryTier.TIER_EXACT
        CardTemplateType.TERM_EXPLANATION -> MemoryTier.TIER_FRAMEWORK
        CardTemplateType.WORK_AUTHOR_BIDIRECTIONAL -> MemoryTier.TIER_FRAMEWORK
        CardTemplateType.SCHOOL_COMPARISON -> MemoryTier.TIER_FRAMEWORK
        CardTemplateType.DISTINCTION -> MemoryTier.TIER_FRAMEWORK
        CardTemplateType.ESSAY_POINTS -> MemoryTier.TIER_UNDERSTAND
    }

    /**
     * 创建默认 MemoRecord（兜底：知识点无对应记录时自动创建）
     *
     * 状态 NEW，stability=0，difficulty=5.0，nextReviewAt=now（立即到期）
     *
     * NF-B 修复：用 [ClockGuard.effectiveNowMillis] 替代 System.currentTimeMillis()，
     * 检测时钟回拨避免新卡 nextReviewAt 异常。
     *
     * P2-AUDIT-1 修正：lastReviewAt 改为 0L（表示"从未复习"），原为 now 语义错误。
     * scheduleInternal 对 lastReview==null/0 走 elapsedDays=0 分支，行为正确。
     */
    private suspend fun createDefaultMemoRecord(pointId: String): MemoRecordEntity {
        val now = clockGuard.effectiveNowMillis()
        return MemoRecordEntity(
            pointId = pointId,
            state = "NEW",
            stability = 0f,
            difficulty = 5f,
            lastReviewAt = 0L,
            nextReviewAt = now,
            reviewCount = 0,
            failCount = 0,
            elapsedDays = 0,
            scheduledDays = 0,
            reps = 0,
            inPriorityQueue = 0,
        )
    }

    /**
     * 错题评分调度：根据用户评分更新错题的 FSRS 调度状态（v0.9.4 新增）。
     *
     * 使用 TIER_FRAMEWORK 档位（R_target=0.90，maxInterval=365，enableFuzz=true），
     * 与名词解释/作品作者等卡片同档，适合错题的中等精度复习需求。
     *
     * 流程：
     * 1. 读取 WrongAnswerEntity（不存在返回 null）
     * 2. sched_* 字段 → FlashCard（[WrongAnswerSchedulingMapper.toFlashCard]）
     * 3. 构造 FsrsWrapper（TIER_FRAMEWORK）+ schedule()
     * 4. FlashCard → sched_* 字段（[WrongAnswerSchedulingMapper.toSchedulingUpdate]）
     * 5. [WrongAnswerDao.updateScheduling] 写回 DB（仅更新 sched_* 字段，不影响 wrongCount 等）
     *
     * 不写 review_logs：错题调度日志暂不入 review_logs 表（避免与知识点复习日志混淆，
     * 后续可扩展独立的 wrong_answer_review_logs 表）。
     *
     * NF-B 修复：用 [ClockGuard.effectiveNowMillis] 替代 LocalDateTime.now()，
     * 检测时钟回拨避免 FSRS 误判。
     */
    override suspend fun rateWrongAnswer(
        wrongAnswerId: String,
        rating: Rating,
    ): WrongAnswerEntity? {
        if (wrongAnswerId.isBlank()) return null

        // 1. 读取错题（含 sched_* 字段）
        val existing = wrongAnswerDao.getById(wrongAnswerId) ?: return null

        // 2. 构造 FsrsWrapper（TIER_FRAMEWORK，与名词解释同档）
        val config = TIER_CONFIGS.getValue(MemoryTier.TIER_FRAMEWORK)
        val wrapper = FsrsWrapper(
            requestRetention = config.targetRetention,
            maximumInterval = config.maxInterval,
            enableFuzz = true,
            stabilityGrowthFactor = config.stabilityGrowthFactor,
            easyBonus = config.easyBonus,
            againPenalty = config.againPenalty,
        )

        // 3. sched_* → FlashCard → FSRS 调度
        val nowMillis = clockGuard.effectiveNowMillis()
        val now = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowMillis),
            ZoneId.systemDefault(),
        )
        val flashCardBefore = WrongAnswerSchedulingMapper.toFlashCard(existing)
        val flashCardAfter = wrapper.schedule(flashCardBefore, rating, now)

        // 4. FlashCard → sched_* 更新参数 → 写回 DB
        val update = WrongAnswerSchedulingMapper.toSchedulingUpdate(flashCardAfter)
        wrongAnswerDao.updateScheduling(
            id = wrongAnswerId,
            state = update.state,
            stability = update.stability,
            difficulty = update.difficulty,
            lastReviewAt = update.lastReviewAt,
            nextReviewAt = update.nextReviewAt,
            reviewCount = update.reviewCount,
            lapses = update.lapses,
            elapsedDays = update.elapsedDays,
            scheduledDays = update.scheduledDays,
            reps = update.reps,
        )

        // 5. 返回更新后的 Entity（重新读取，确保 sched_* 字段一致）
        return wrongAnswerDao.getById(wrongAnswerId)
    }
}
