package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 复习仓库（Task 16）。
 *
 * 职责：
 * - 提供 FSRS 复习队列，仅包含 ocr_status='VERIFIED' 的知识点（PENDING 不进复习队列）
 * - 提供今日待复习数量（已 VERIFIED 且到期）
 * - 提供"待校对"区数据：所有 ocr_status='PENDING' 的知识点及其数量
 * - 提供用户校对后标记 VERIFIED 激活的能力
 *
 * 通过构造函数注入 [KnowledgePointDao] 和 [MemoRecordDao]（Hilt @Inject）。
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
     * 如需获取全部 VERIFIED 知识点（含未到期），使用 [getAllVerifiedKnowledgePoints]。
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
                val dueIds = dueRecords.map { it.pointId }.toSet()
                verifiedPoints.filter { it.id in dueIds }
            }
        }
        .distinctUntilChanged()
        .catchAndLog(TAG, "getReviewQueue") { emptyList() }

    /**
     * 获取所有已 VERIFIED 的知识点（不过滤到期状态）。
     *
     * 供知识点浏览界面使用（如 [com.wenyan.app.feature.knowledge.KnowledgeViewModel]），
     * 与 [getReviewQueue] 区别：此方法返回全部已验证知识点，不论是否到期。
     */
    fun getAllVerifiedKnowledgePoints(): Flow<List<KnowledgePointEntity>> =
        knowledgePointDao.observeVerifiedForReview()
            .catchAndLog(TAG, "getAllVerifiedKnowledgePoints") { emptyList() }

    /**
     * 获取所有已 VERIFIED 的知识点，附带科目名（P1 修复）。
     *
     * 供知识点浏览界面的分类筛选使用（如 [com.wenyan.app.feature.knowledge.KnowledgeViewModel]），
     * 与 [getAllVerifiedKnowledgePoints] 区别：此方法返回科目名，支持按科目过滤。
     */
    fun getVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>> =
        knowledgePointDao.observeVerifiedWithSubject()
            .catchAndLog(TAG, "getVerifiedWithSubject") { emptyList() }

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
                dueRecords.count { it.pointId in verifiedIds }
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
