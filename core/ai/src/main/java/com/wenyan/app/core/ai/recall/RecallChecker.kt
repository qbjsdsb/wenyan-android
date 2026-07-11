package com.wenyan.app.core.ai.recall

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 主动回忆检测引擎（三层渐进式方案）。
 *
 * Spec 第 359-385 行、设计文档 3.3.5 节（第 849-918 行）：
 *
 * 三层检测机制（阈值对齐设计文档）：
 * - L1 关键词匹配 + 同义词词典（名词解释/术语，本地 <10ms）
 *     覆盖率 < 30% → Again（设计文档第 864 行）
 *     覆盖率 30-60% → Hard
 *     覆盖率 60-85% → Good
 *     覆盖率 ≥ 85% → Easy
 * - L2 语义相似度 BGE-small-zh 模型（论述题/分析题，本地 <100ms）
 *     覆盖率 < 60% → Hard（设计文档第 872 行）
 *     覆盖率 60-85% → 部分正确（触发 L3）
 *     覆盖率 ≥ 85% → Easy
 * - L3 LLM 异步评估（L2 判定"部分正确"时触发，在线 3-5 秒）
 *     输出 0-100 分及理由
 *     不阻塞用户复习流程
 *     分数 < 60 → Again, 60-75 → Hard, 75-90 → Good, ≥ 90 → Easy
 *
 * 注意：阈值严格对齐设计文档，非 Spec 错误版本：
 * - L1 是 <30%→Again（非 Spec 错误的"≥70%判正确"）
 * - L2 是 <60%→Hard（非 Spec 错误的">0.85判正确"）
 */
class RecallChecker @Inject constructor(
    private val aiService: com.wenyan.app.core.ai.AiService,
) {

    /**
     * 检测用户主动回忆质量。
     *
     * @param userAnswer 用户答案
     * @param correctAnswer 正确答案
     * @param questionType 题目类型（名词解释走 L1，论述题走 L2/L3）
     * @return 检测结果，包含层级、覆盖率、评分和理由
     */
    fun checkRecall(
        userAnswer: String,
        correctAnswer: String,
        questionType: QuestionType,
    ): Flow<RecallResult> = flow {
        when (questionType) {
            QuestionType.TERM_EXPLANATION -> {
                // 名词解释/术语：走 L1 关键词匹配
                emit(checkL1Keyword(userAnswer, correctAnswer))
            }
            QuestionType.ESSAY -> {
                // 论述题/分析题：走 L2 语义相似度
                val l2Result = checkL2Semantic(userAnswer, correctAnswer)
                if (l2Result.rating == RecallRating.HARD && l2Result.coverage in PARTIAL_CORRECT_RANGE) {
                    // L2 判定"部分正确"（覆盖率 60-85%）时触发 L3 异步评估
                    // L3 不阻塞用户复习流程：先返回 L2 结果，L3 结果后续更新
                    emit(l2Result)
                    // 异步触发 L3，后续可通过单独 Flow 获取 L3 结果
                    // emit(checkL3Llm(userAnswer, correctAnswer))
                } else {
                    emit(l2Result)
                }
            }
        }
    }

    // ── L1: 关键词匹配 + 同义词词典 ───────────────────────────────

    /**
     * L1 关键词匹配检测（设计文档第 859-864 行）。
     *
     * 逻辑：
     * - 提取正确答案中的关键词 + 同义词词典扩展
     * - 检测用户答案中关键词覆盖率
     * - 阈值对齐设计文档第 864 行：
     *   覆盖率 < 30% → Again
     *   覆盖率 30-60% → Hard
     *   覆盖率 60-85% → Good
     *   覆盖率 ≥ 85% → Easy
     */
    private fun checkL1Keyword(userAnswer: String, correctAnswer: String): RecallResult {
        val keywords = extractKeywords(correctAnswer)
        val expandedKeywords = expandWithSynonyms(keywords)
        val coverage = calculateKeywordCoverage(userAnswer, expandedKeywords)

        val rating = when {
            coverage < L1_THRESHOLD_AGAIN -> RecallRating.AGAIN   // <30%
            coverage < L1_THRESHOLD_HARD -> RecallRating.HARD     // 30-60%
            coverage < L1_THRESHOLD_GOOD -> RecallRating.GOOD     // 60-85%
            else -> RecallRating.EASY                               // ≥85%
        }

        return RecallResult(
            level = RecallLevel.L1,
            coverage = coverage,
            rating = rating,
            score = null,
            reason = null,
        )
    }

    // ── L2: 语义相似度 BGE-small-zh ───────────────────────────────

    /**
     * L2 语义相似度检测（设计文档第 866-872 行 + Spec BGE-small-zh 补充）。
     *
     * 逻辑：
     * - 使用 BGE-small-zh 模型计算用户答案与正确答案的语义相似度
     * - 阈值对齐设计文档第 872 行：
     *   覆盖率 < 60% → Hard
     *   覆盖率 60-85% → 部分正确（触发 L3）
     *   覆盖率 ≥ 85% → Easy
     */
    private fun checkL2Semantic(userAnswer: String, correctAnswer: String): RecallResult {
        val similarity = calculateSemanticSimilarity(userAnswer, correctAnswer)

        val rating = when {
            similarity < L2_THRESHOLD_HARD -> RecallRating.HARD              // <60%
            similarity < L2_THRESHOLD_PARTIAL -> RecallRating.HARD           // 60-85% 部分正确，触发L3
            else -> RecallRating.EASY                                        // ≥85%
        }

        return RecallResult(
            level = RecallLevel.L2,
            coverage = similarity,
            rating = rating,
            score = null,
            reason = null,
        )
    }

    // ── L3: LLM 异步评估 ─────────────────────────────────────────

    /**
     * L3 LLM 异步评估（设计文档第 874-881 行）。
     *
     * 逻辑：
     * - 调用大模型 API，输入参考答案 + 用户复述
     * - 输出 0-100 分及理由
     * - 不阻塞用户复习流程
     * - 阈值映射：
     *   分数 < 60 → Again
     *   分数 60-75 → Hard
     *   分数 75-90 → Good
     *   分数 ≥ 90 → Easy
     */
    private suspend fun checkL3Llm(userAnswer: String, correctAnswer: String): RecallResult {
        // TODO: 调用 AI 服务进行 LLM 评估
        // 当前为骨架实现，后续接入实际 LLM API
        val score = 0
        val reason = "LLM 评估待接入"

        val rating = when {
            score < L3_THRESHOLD_AGAIN -> RecallRating.AGAIN   // <60
            score < L3_THRESHOLD_HARD -> RecallRating.HARD     // 60-75
            score < L3_THRESHOLD_GOOD -> RecallRating.GOOD     // 75-90
            else -> RecallRating.EASY                           // ≥90
        }

        return RecallResult(
            level = RecallLevel.L3,
            coverage = score / 100f,
            rating = rating,
            score = score,
            reason = reason,
        )
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────

    /** 提取正确答案中的关键词（TODO: 接入分词 + 关键词提取） */
    private fun extractKeywords(correctAnswer: String): List<String> {
        // TODO: 使用分词工具提取关键词
        return correctAnswer.split("，", "。", "、", "；").filter { it.isNotBlank() }
    }

    /** 同义词词典扩展（TODO: 接入同义词词典） */
    private fun expandWithSynonyms(keywords: List<String>): List<String> {
        // TODO: 查同义词词典，扩展关键词列表
        return keywords
    }

    /** 计算关键词覆盖率 */
    private fun calculateKeywordCoverage(userAnswer: String, keywords: List<String>): Float {
        if (keywords.isEmpty()) return 0f
        val matched = keywords.count { keyword ->
            userAnswer.contains(keyword.trim(), ignoreCase = true)
        }
        return matched.toFloat() / keywords.size
    }

    /** BGE-small-zh 语义相似度计算（TODO: 接入 BGE-small-zh 模型） */
    private fun calculateSemanticSimilarity(userAnswer: String, correctAnswer: String): Float {
        // TODO: 接入 BGE-small-zh-v1.5 模型计算语义相似度
        // 当前返回 0f 作为骨架实现
        return 0f
    }

    companion object {
        // L1 阈值（对齐设计文档第 864 行）
        private const val L1_THRESHOLD_AGAIN = 0.30f   // <30% → Again
        private const val L1_THRESHOLD_HARD = 0.60f    // 30-60% → Hard
        private const val L1_THRESHOLD_GOOD = 0.85f    // 60-85% → Good

        // L2 阈值（对齐设计文档第 872 行）
        private const val L2_THRESHOLD_HARD = 0.60f    // <60% → Hard
        private const val L2_THRESHOLD_PARTIAL = 0.85f // 60-85% → 部分正确（触发L3）

        // L3 阈值（设计文档第 881 行：L3评分映射为Good/Easy）
        private const val L3_THRESHOLD_AGAIN = 60      // <60 → Again
        private const val L3_THRESHOLD_HARD = 75       // 60-75 → Hard
        private const val L3_THRESHOLD_GOOD = 90       // 75-90 → Good

        /** L2 部分正确范围（触发 L3） */
        private val PARTIAL_CORRECT_RANGE = 0.60f..0.85f
    }
}

/**
 * 题目类型。
 *
 * @property TERM_EXPLANATION 名词解释/术语（走 L1 关键词匹配）
 * @property ESSAY 论述题/分析题（走 L2 语义相似度 + L3 LLM 评估）
 */
enum class QuestionType {
    TERM_EXPLANATION,
    ESSAY,
}

/**
 * 检测层级。
 *
 * @property L1 关键词匹配 + 同义词词典（本地 <10ms）
 * @property L2 语义相似度 BGE-small-zh（本地 <100ms）
 * @property L3 LLM 异步评估（在线 3-5 秒）
 */
enum class RecallLevel {
    L1,
    L2,
    L3,
}

/**
 * 回忆评分（对接 FSRS Rating）。
 *
 * @property AGAIN 完全忘记（L1 覆盖率 <30% / L3 分数 <60）
 * @property HARD 困难（L1 覆盖率 30-60% / L2 覆盖率 <60% / L3 分数 60-75）
 * @property GOOD 良好（L1 覆盖率 60-85% / L3 分数 75-90）
 * @property EASY 轻松（L1 覆盖率 ≥85% / L2 覆盖率 ≥85% / L3 分数 ≥90）
 */
enum class RecallRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}

/**
 * 回忆检测结果。
 *
 * @param level 检测层级（L1/L2/L3）
 * @param coverage 覆盖率 0-1（L1 关键词覆盖率 / L2 语义相似度 / L3 分数百分比）
 * @param rating 评分（对接 FSRS Rating）
 * @param score L3 时为 0-100 分，L1/L2 时为 null
 * @param reason L3 时为评估理由，L1/L2 时为 null
 */
data class RecallResult(
    val level: RecallLevel,
    val coverage: Float,
    val rating: RecallRating,
    val score: Int?,
    val reason: String?,
)
