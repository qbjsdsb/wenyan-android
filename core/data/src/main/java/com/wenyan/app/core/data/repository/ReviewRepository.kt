package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
 */
@Singleton
class ReviewRepository @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao,
    private val memoRecordDao: MemoRecordDao,
) {

    private companion object {
        private const val TAG = "ReviewRepository"
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
     */
    fun getReviewQueue(): Flow<List<KnowledgePointEntity>> = combine(
        knowledgePointDao.observeVerifiedForReview(),
        memoRecordDao.observeDue(),
    ) { verifiedPoints, dueRecords ->
        val dueIds = dueRecords.map { it.pointId }.toSet()
        verifiedPoints.filter { it.id in dueIds }
    }.catchAndLog(TAG, "getReviewQueue") { emptyList() }

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
     */
    fun getPendingReviewCount(): Flow<Int> = combine(
        knowledgePointDao.observeVerifiedForReview(),
        memoRecordDao.observeDue(),
    ) { verifiedPoints, dueRecords ->
        val verifiedIds = verifiedPoints.map { it.id }.toSet()
        dueRecords.count { it.pointId in verifiedIds }
    }.catchAndLog(TAG, "getPendingReviewCount") { 0 }

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
