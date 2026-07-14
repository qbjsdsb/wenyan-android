package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.util.ExamCodeResolver
import com.wenyan.app.core.data.util.SubjectResolution
import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.ExamCodeHistoryDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.ExamCodeHistoryEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真题仓库（Task 16 + Task 26 合并）。
 *
 * 职责：
 * - 查询真题并联合 exam_code_history 表判定科目信息（Task 26.6）
 * - 使用 [ExamCodeResolver] 处理 610/801 语义翻转问题（Task 26）
 * - 年份代码缺失时显示"年份待核实"，不猜测科目名称（Task 26）
 * - 真题练习中过滤 ocr_status='PENDING' 的关联知识点（Task 16.2）
 *
 * 示例：
 * - 2022年610真题 → "610 文学基础（2022年代码）"
 * - 2026年610真题 → "610 专业写作（2026年代码）"
 *
 * P1 审计修复：combine/observe 链加 .catchAndLog，DAO 异常时降级为空列表，
 * 避免 ViewModel collect 崩溃导致 UI 永久 failed。
 */
@Singleton
class ExamRepository @Inject constructor(
    private val examQuestionDao: ExamQuestionDao,
    private val examCodeHistoryDao: ExamCodeHistoryDao,
    private val knowledgePointDao: KnowledgePointDao,
) {

    private companion object {
        private const val TAG = "ExamRepository"
    }

    /**
     * 查询某年所有真题，附带科目判定信息（Task 26.6）。
     *
     * 联合 exam_code_history 表，使用 [ExamCodeResolver.resolveSubject] 判定每道题的科目：
     * - 真题的 examPaperCode + year → 科目判定结果
     * - 若真题缺少 examPaperCode，返回"试卷代码未知（年份待核实）"
     *
     * @param year 年份
     * @return 真题列表（含科目判定信息）
     */
    fun getExamQuestionsWithSubjectInfo(year: Int): Flow<List<ExamQuestionWithSubject>> {
        return combine(
            examQuestionDao.observeByYear(year),
            examCodeHistoryDao.observeAll(),
        ) { questions, history ->
            questions.map { question ->
                val resolution = resolveQuestionSubject(question, year, history)
                ExamQuestionWithSubject(question, resolution)
            }
        }.catchAndLog(TAG, "getExamQuestionsWithSubjectInfo") { emptyList() }
    }

    /**
     * 获取某年所有真题（Task 16.2）。
     *
     * @param year 年份
     * @return 真题列表
     */
    fun getExamQuestionsByYear(year: Int): Flow<List<ExamQuestionEntity>> =
        examQuestionDao.observeByYear(year)
            .catchAndLog(TAG, "getExamQuestionsByYear") { emptyList() }

    /**
     * 获取所有可用年份列表（降序）。
     *
     * 用于真题练习模块的年份选择器。
     */
    fun getAvailableYears(): Flow<List<Int>> =
        examQuestionDao.observeYears()
            .catchAndLog(TAG, "getAvailableYears") { emptyList() }

    /**
     * 获取真题的关联知识点，仅返回 ocr_status='VERIFIED' 的知识点（Task 16.2）。
     *
     * PENDING 状态的知识点（OCR待校对）不在真题练习中展示，
     * 避免用户基于未校对内容练习。
     *
     * 实现方式：先查询真题获取 relatedPointIds，再从 VERIFIED 知识点中筛选关联的。
     *
     * @param questionId 真题ID
     * @return 已VERIFIED的关联知识点列表（PENDING被过滤）
     */
    fun getRelatedKnowledgePoints(questionId: String): Flow<List<KnowledgePointEntity>> =
        combine(
            examQuestionDao.observeById(questionId),
            knowledgePointDao.observeVerifiedForReview(),
        ) { question, verifiedPoints ->
            val relatedIds = question?.relatedPointIds
            if (relatedIds.isNullOrEmpty()) {
                emptyList()
            } else {
                // NF-BB5: 转 Set 去 + O(1) 查找，原 List 的 `in` 是 O(n)
                val relatedIdSet = relatedIds.toSet()
                verifiedPoints.filter { it.id in relatedIdSet }
            }
        }.catchAndLog(TAG, "getRelatedKnowledgePoints") { emptyList() }

    /**
     * 判定单道真题的科目信息。
     *
     * - 若真题有 examPaperCode，调用 [ExamCodeResolver.resolveSubject] 判定
     * - 若真题缺少 examPaperCode，返回"试卷代码未知（年份待核实）"
     */
    private fun resolveQuestionSubject(
        question: ExamQuestionEntity,
        year: Int,
        history: List<ExamCodeHistoryEntity>,
    ): SubjectResolution {
        val code = question.examPaperCode
        return if (code != null) {
            ExamCodeResolver.resolveSubject(code, year, history)
        } else {
            SubjectResolution(
                displayName = "试卷代码未知（年份待核实）",
                subjectName = "",
                direction = null,
                isVerified = false,
                warningMessage = "该真题缺少试卷代码，请以官方招生目录为准",
            )
        }
    }
}

/**
 * 真题及科目判定信息（Task 26.6）。
 *
 * @property question 真题实体
 * @property subjectResolution 科目判定结果
 */
data class ExamQuestionWithSubject(
    val question: ExamQuestionEntity,
    val subjectResolution: SubjectResolution,
)
