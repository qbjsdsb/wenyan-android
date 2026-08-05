package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import com.wenyan.app.core.database.entity.MemoRecordEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 今日学习队列（v0.9.29 卡片备考系统）。
 *
 * 由两部分组成：
 * - [duePoints]：到期复习知识点（FSRS 调度，现有逻辑）
 * - [newPoints]：每日新卡知识点（VERIFIED 且从未学习，按考频/科目筛选 + 每日限额）
 *
 * UI 据此展示"今日任务"：新卡 X 张 · 复习 Y 张。
 */
data class TodayStudyQueue(
    val duePoints: List<KnowledgePointEntity>,
    val newPoints: List<KnowledgePointEntity>,
) {
    val totalPoints: Int get() = duePoints.size + newPoints.size
}

/** 学习进度（v0.9.29 卡片备考系统）：已学/总知识点数。 */
data class StudyProgress(
    val learnedPoints: Int,
    val totalVerifiedPoints: Int,
)

/**
 * 尚未发生过真实评分的初始记录。
 *
 * 不能只检查 `state == NEW` 或 `reps == 0`：早期版本曾出现 reps 未回填。状态与两种
 * 计数都保持初始值时，才认定为未学习。这里有意不要求 `lastReviewAt == 0`，因为旧版
 * SeedDataLoader 曾给未学习记录写入安装时间；升级后仍应把它们当作新卡。
 */
internal fun MemoRecordEntity.isPristineNew(): Boolean =
    state == "NEW" && reps == 0 && reviewCount == 0

/** 从记忆记录中提取真正已有学习痕迹的知识点 ID。 */
internal fun learnedPointIds(records: List<MemoRecordEntity>): Set<String> =
    records.asSequence()
        .filterNot { it.isPristineNew() }
        .map { it.pointId }
        .toSet()

/**
 * 对 DAO 的到期结果再做一层领域规则保护。
 *
 * DAO 已在 SQL 层排除 pristine NEW；此处防止 Fake DAO、旧查询实现或未来重构重新把新卡
 * 混入复习队列。
 */
internal fun duePointIds(records: List<MemoRecordEntity>): Set<String> =
    records.asSequence()
        .filterNot { it.isPristineNew() }
        .map { it.pointId }
        .toSet()

/**
 * 距考试天数（v0.9.29，纯函数可测）。
 *
 * @param examDateMillis 考试日期（毫秒时间戳）
 * @param nowMillis 当前时间（默认系统时间；测试可注入）
 * @return 剩余天数（不足 1 天按 0，已过按 0）
 */
fun daysUntilExam(
    examDateMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): Int {
    val millis = examDateMillis - nowMillis
    return (millis / 86_400_000L).toInt().coerceAtLeast(0)
}

/**
 * 复习/新卡比例保护（v0.9.29 打磨，纯函数可测）。
 *
 * 当今日复习知识点较多时减少新卡，避免"复习 + 新卡"总量过大导致堆积焦虑：
 * - duePointCount ≤ 10（约 60 张复习）：新卡按用户限额（默认 60）
 * - duePointCount ≤ 20（约 60-120 张复习）：新卡减半（30）
 * - duePointCount > 20（超过 120 张复习）：暂停新卡（0）
 *
 * @param duePointCount 今日到期复习知识点数
 * @param dailyNewLimit 用户设置的每日新卡限额
 */
internal fun computeEffectiveNewLimit(duePointCount: Int, dailyNewLimit: Int): Int = when {
    duePointCount <= 10 -> dailyNewLimit
    duePointCount <= 20 -> dailyNewLimit / 2
    else -> 0
}

/**
 * 从全部 VERIFIED 知识点中挑选每日新卡（v0.9.29，纯函数可测）。
 *
 * 规则：
 * 1. 排除已学（memo_record 已有真实评分痕迹）的知识点；pristine NEW 仍属于新卡
 * 2. 按科目筛选（subject_name 为 null 时保留，避免漏卡）
 * 3. 按考频筛选（HIGH / HIGH+MEDIUM / ALL）
 * 4. 排序：考频 HIGH → MEDIUM → LOW，同频按 updated_at（新内容优先）
 * 5. 按卡片数限额取整到知识点（默认 60 张 ≈ 10 个知识点），保证 sibling 完整
 */
internal fun selectNewPoints(
    verifiedWithSubject: List<KnowledgePointWithSubject>,
    learnedIds: Set<String>,
    settings: CardSettings,
    dailyNewLimit: Int,
): List<KnowledgePointEntity> {
    val candidates = verifiedWithSubject
        .filter { it.point.id !in learnedIds }
        .filter { it.subjectName == null || it.subjectName in settings.subjectFilters }
        .filter { matchesFrequency(it.point.examFrequency, settings.frequencyFilter) }
        .sortedWith(
            // v0.9.35 审计修复：同考频下 updated_at 应降序（新内容优先，与注释及
            // KnowledgePointDao ORDER BY updated_at DESC 一致）；原 compareBy 升序
            // 导致每日新卡永远先推最旧内容，新内容可能长期不出现
            compareBy<KnowledgePointWithSubject> { frequencyRank(it.point.examFrequency) }
                .thenByDescending { it.point.updatedAt },
        )
    return takeNewPointsByCardLimit(candidates.map { it.point }, dailyNewLimit)
}

/** 考频匹配（纯函数）。 */
internal fun matchesFrequency(frequency: String, filter: CardFrequencyFilter): Boolean =
    when (filter) {
        CardFrequencyFilter.HIGH -> frequency == "HIGH"
        CardFrequencyFilter.HIGH_MEDIUM -> frequency == "HIGH" || frequency == "MEDIUM"
        CardFrequencyFilter.ALL -> true
    }

/** 考频排序权重（HIGH=0 优先）。 */
internal fun frequencyRank(frequency: String): Int = when (frequency) {
    "HIGH" -> 0
    "MEDIUM" -> 1
    else -> 2
}

/**
 * 按卡片数限额取整到知识点（纯函数）。
 *
 * 每取一个知识点累加 [cardsPerPoint] 张卡，累计达到限额即停止。
 * 60 张 ≈ 10 个知识点（每个约 6 张卡），保证 sibling 卡完整不拆散。
 */
internal fun takeNewPointsByCardLimit(
    candidates: List<KnowledgePointEntity>,
    dailyNewLimit: Int,
    cardsPerPoint: Int = CARDS_PER_POINT_ESTIMATE,
): List<KnowledgePointEntity> {
    if (dailyNewLimit <= 0) return emptyList()
    var count = 0
    val result = mutableListOf<KnowledgePointEntity>()
    for (point in candidates) {
        result.add(point)
        count += cardsPerPoint
        if (count >= dailyNewLimit) break
    }
    return result
}

/** 每个知识点约生成 6 张卡（名词解释 5-6 + 论述要点 1）的估算值。 */
internal const val CARDS_PER_POINT_ESTIMATE = 6

/**
 * 复习仓库（Task 16）。
 *
 * 职责：
 * - 提供 FSRS 复习队列，仅包含 ocr_status='VERIFIED' 的知识点（PENDING 不进复习队列）
 * - 提供今日待复习数量（已 VERIFIED 且到期）
 * - v0.9.29：提供"今日学习队列"（到期复习 ∪ 每日新卡，按考频/科目筛选 + 每日限额）
 * - 提供"待校对"区数据：所有 ocr_status='PENDING' 的知识点及其数量
 * - 提供用户校对后标记 VERIFIED 激活的能力
 *
 * 通过构造函数注入 [KnowledgePointDao] 和 [MemoRecordDao]（Hilt @Inject）。
 * v0.9.29 新增 [CardSettingsRepository]（每日新卡限额/考频/科目/考试日期）。
 *
 * P1 审计修复：combine/map 链加 .catchAndLog，DAO 异常时降级为空列表/0，
 * 避免 ViewModel collect 崩溃导致 UI 永久 failed。
 *
 * P1-1 修复：[getReviewQueue] / [getPendingReviewCount] 加 [tickFlow] + [flatMapLatest]
 * 周期刷新。Room @Query 返回的 Flow 仅在表数据变化时重新查询，即使 SQL 内用
 * `strftime('%s','now')` 也不会随时间推移自动触发。每 60s 重新订阅 [MemoRecordDao.observeDue]
 * 让新到期的卡片自动进入复习队列，无需用户手动刷新或评分触发。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ReviewRepository @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao,
    private val memoRecordDao: MemoRecordDao,
    private val cardSettingsRepository: CardSettingsRepository,
    @com.wenyan.app.core.data.di.ApplicationScope private val externalScope: CoroutineScope,
) {

    private companion object {
        private const val TAG = "ReviewRepository"

        /**
         * 复习队列自动刷新间隔（P1-1 修复）。
         *
         * 60s 足以让用户感知"卡片到期"的及时性（FSRS 最小间隔为分钟级学习步），
         * 同时避免过于频繁的数据库查询消耗电量。
         */
        private const val REFRESH_INTERVAL_MS = 60_000L
    }

    /**
     * 周期性 tick 流（P1-1 修复）。
     *
     * 每 [REFRESH_INTERVAL_MS] 发射一次，用于触发 [flatMapLatest] 重新订阅
     * 依赖时间的 DAO Flow（[MemoRecordDao.observeDue]）。
     *
     * 注意：tickFlow 是冷流，仅在有订阅者时才发射，无订阅者时不消耗资源。
     * 订阅者取消时（如 ViewModel onCleared）自动停止 delay。
     */
    private val tickFlow: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(REFRESH_INTERVAL_MS)
        }
    }

    /**
     * FSRS 复习队列：仅返回已 VERIFIED 且到期（next_review_at <= 当前时间）的知识点。
     *
     * PENDING 状态的知识点（OCR 待校对）不进入正常复习流程，
     * 需用户校对后调用 [markAsVerified] 激活才会出现于此队列。
     *
     * 到期判断通过 [MemoRecordDao.observeDue]（使用 SQLite 内置时间）实现，
     * 与 [getPendingReviewCount] 保持语义一致：队列长度 = 待复习数量。
     *
     * 如需获取全部 VERIFIED 知识点（含未到期，附科目名），使用
     * [com.wenyan.app.core.data.repository.KnowledgeRepository.getVerifiedWithSubject]
     * (v0.8.19 已从本仓库迁移到 KnowledgeRepository)。
     *
     * P1-1 修复：原实现直接 `combine(observeVerifiedForReview, observeDue)`，
     * Room Flow 仅在表数据变化时重新查询，observeDue 内的 `strftime('%s','now')`
     * 不会随时间推移自动触发刷新。用户长时间不操作时，新到期的卡片不会进入队列。
     * 现用 [tickFlow] + [flatMapLatest] 每 60s 重新订阅 observeDue，
     * 让到期的卡片自动进入复习队列。
     *
     * [distinctUntilChanged] 过滤重复值，避免 tick 触发但内容未变时 UI 无谓重组。
     */
    fun getReviewQueue(): Flow<List<KnowledgePointEntity>> = tickFlow
        .flatMapLatest {
            combine(
                knowledgePointDao.observeVerifiedForReview(),
                memoRecordDao.observeDue(),
            ) { verifiedPoints, dueRecords ->
                val dueIds = duePointIds(dueRecords)
                verifiedPoints.filter { it.id in dueIds }
            }
        }
        .distinctUntilChanged()
        .catchAndLog(TAG, "getReviewQueue") { emptyList() }

    /**
     * 今日学习队列共享热流（v0.9.37 P0-2）。
     *
     * 原实现每次 collect 都独立执行整条 tickFlow + Room 订阅链：卡片页
     * todayPlan 横幅（仅计数）与 [CardRepositoryImpl.getCardsForReview]
     * （拆卡）各订阅一次，每 60s tick 触发**两套**全表查询 + selectNewPoints
     * 计算。`stateIn` 共享后仅一个上游在跑，多 UI 流复用同一结果。
     *
     * [SharingStarted.WhileSubscribed]：无订阅者 5s 后停止上游（tick 停止），
     * 避免后台空转；首个订阅立即拿到空初始值，随后被真实队列替换。
     */
    private val sharedTodayStudyQueue: Flow<TodayStudyQueue> =
        buildTodayStudyQueue().stateIn(
            scope = externalScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayStudyQueue(emptyList(), emptyList()),
        )

    /**
     * 今日学习队列（v0.9.29 卡片备考系统）。
     *
     * = 到期复习知识点 ∪ 每日新卡知识点（按考频/科目筛选 + 每日限额）。
     *
     * - [TodayStudyQueue.duePoints]：现有 FSRS 到期复习队列
     * - [TodayStudyQueue.newPoints]：新卡候选按 [CardSettings] 筛选后，按卡片数限额取整到知识点
     *   （默认 60 张 ≈ 10 个知识点；新卡首次评分仍走 FSRS，与到期复习行为一致）
     *
     * 依赖数据源：
     * - [KnowledgePointDao.observeVerifiedForReview]：VERIFIED 知识点（到期判断用）
     * - [KnowledgePointDao.observeVerifiedWithSubject]：VERIFIED 知识点 + 科目名（新卡候选用）
     * - [MemoRecordDao.observeDue]：到期记忆记录
     * - [MemoRecordDao.observeAll]：全部记录（新卡 = 无记录或仅有 pristine NEW 记录）
     * - [CardSettingsRepository.cardSettings]：考频/科目/每日限额
     *
     * 全部用 tickFlow + flatMapLatest 周期刷新，保证 60s 内新到期/新卡自动进入。
     *
     * v0.9.37 P0-2：返回 [sharedTodayStudyQueue] 共享热流，多订阅不重复查询。
     */
    private fun buildTodayStudyQueue(): Flow<TodayStudyQueue> = tickFlow
        .flatMapLatest {
            combine(
                knowledgePointDao.observeVerifiedForReview(),
                memoRecordDao.observeDue(),
                memoRecordDao.observeAll(),
                knowledgePointDao.observeVerifiedWithSubject(),
                cardSettingsRepository.cardSettings,
            ) { verifiedPoints, dueRecords, allRecords, verifiedWithSubject, settings ->
                val dueIds = duePointIds(dueRecords)
                val duePoints = verifiedPoints.filter { it.id in dueIds }
                val learnedIds = learnedPointIds(allRecords)
                // v0.9.29 打磨：复习/新卡比例保护——复习量大时自动减少/暂停新卡
                val effectiveNewLimit = computeEffectiveNewLimit(
                    duePointCount = duePoints.size,
                    dailyNewLimit = settings.dailyNewLimit,
                )
                val newPoints = selectNewPoints(
                    verifiedWithSubject = verifiedWithSubject,
                    learnedIds = learnedIds,
                    settings = settings,
                    dailyNewLimit = effectiveNewLimit,
                )
                TodayStudyQueue(duePoints = duePoints, newPoints = newPoints)
            }
        }
        .distinctUntilChanged()
        .catchAndLog(TAG, "getTodayStudyQueue") { TodayStudyQueue(emptyList(), emptyList()) }

    /** 对外暴露共享热流（v0.9.37 P0-2）。 */
    fun getTodayStudyQueue(): Flow<TodayStudyQueue> = sharedTodayStudyQueue

    /**
     * 学习进度（v0.9.29）：已学知识点数 / 总 VERIFIED 知识点数。
     *
     * 用于卡片页"今日任务"进度条：
     * - [StudyProgress.learnedPoints]：memo_record 已有真实评分痕迹的 VERIFIED 知识点
     * - [StudyProgress.totalVerifiedPoints]：VERIFIED 总数
     */
    fun getStudyProgress(): Flow<StudyProgress> = combine(
        memoRecordDao.observeAll(),
        knowledgePointDao.observeVerifiedForReview(),
    ) { records, verifiedPoints ->
        val learnedIds = learnedPointIds(records)
        val verifiedIds = verifiedPoints.mapTo(mutableSetOf()) { it.id }
        StudyProgress(
            learnedPoints = verifiedIds.count { it in learnedIds },
            totalVerifiedPoints = verifiedIds.size,
        )
    }.distinctUntilChanged()
        .catchAndLog(TAG, "getStudyProgress") { StudyProgress(0, 0) }

    /**
     * 今日待复习数量：已 VERIFIED 且到期（next_review_at <= 当前时间）的知识点数。
     *
     * 合并 VERIFIED 知识点流与到期记忆记录流，过滤出 VERIFIED 中到期的数量。
     * 到期判断使用 SQLite 内置时间（strftime），避免 System.currentTimeMillis()
     * 在 Flow 构建时固定导致长时间运行后计数失效。
     *
     * P1-1 修复：同 [getReviewQueue]，用 [tickFlow] + [flatMapLatest] 周期刷新。
     * [distinctUntilChanged] 过滤重复值，避免 tick 触发但数量未变时 UI 无谓重组。
     */
    fun getPendingReviewCount(): Flow<Int> = tickFlow
        .flatMapLatest {
            combine(
                knowledgePointDao.observeVerifiedForReview(),
                memoRecordDao.observeDue(),
            ) { verifiedPoints, dueRecords ->
                val verifiedIds = verifiedPoints.map { it.id }.toSet()
                duePointIds(dueRecords).count { it in verifiedIds }
            }
        }
        .distinctUntilChanged()
        .catchAndLog(TAG, "getPendingReviewCount") { 0 }

    /**
     * "待校对"区：返回所有 ocr_status='PENDING' 的知识点。
     *
     * 这些知识点 OCR 结果尚未经用户校对，不进入复习队列，
     * 在独立入口供用户审阅与激活。
     */
    fun getPendingOcrKnowledgePoints(): Flow<List<KnowledgePointEntity>> =
        knowledgePointDao.observeByOcrStatus("PENDING")
            .catchAndLog(TAG, "getPendingOcrKnowledgePoints") { emptyList() }

    /** "待校对"区数量：PENDING 状态知识点数 */
    fun getPendingOcrCount(): Flow<Int> =
        knowledgePointDao.observeByOcrStatus("PENDING")
            .map { it.size }
            .catchAndLog(TAG, "getPendingOcrCount") { 0 }

    /**
     * 将指定知识点标记为 VERIFIED（激活）。
     *
     * 用户校对 OCR 内容后调用，将 ocr_status 从 PENDING 改为 VERIFIED，
     * 激活后该知识点进入正常 FSRS 复习流程。
     *
     * 为 suspend 函数，确保 Room 在非主线程执行（主线程安全）。
     */
    suspend fun markAsVerified(knowledgePointId: String) {
        knowledgePointDao.updateOcrStatus(knowledgePointId, "VERIFIED")
    }
}
