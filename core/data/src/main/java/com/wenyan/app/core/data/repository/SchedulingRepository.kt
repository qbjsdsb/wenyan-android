package com.wenyan.app.core.data.repository

import androidx.room.withTransaction
import com.wenyan.app.core.data.mapper.MemoRecordMapper
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.ReviewLogEntity
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
 * 桥接 UI 卡片评分与 FSRS 调度算法。生产实现见 [SchedulingRepositoryImpl]。
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
}

@Singleton
class SchedulingRepositoryImpl @Inject constructor(
    private val database: WenyanDatabase,
    private val memoRecordDao: MemoRecordDao,
    private val reviewLogDao: ReviewLogDao,
    private val clockGuard: ClockGuard,
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
}
