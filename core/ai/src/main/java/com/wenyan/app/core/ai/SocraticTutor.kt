package com.wenyan.app.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 苏格拉底式 AI 导师。
 *
 * Spec 第 344-388 行要求（增强设计文档 3.6 节 AI 助手，而非替代）：
 * - 苏格拉底式引导：不直接给答案，引导用户自己找到答案
 * - "解释我的答案"机制：答错后 AI 分析错误思路
 * - RAG 架构：基于用户资料库 + 权威教材库检索，引用可溯源
 * - 用户答案过短或离题时不强行分析论证漏洞
 *
 * 与设计文档 3.6 节的关系：
 * - 设计文档 AI 直接批改 → 本 Spec 增强为"先引导用户作答再批改"
 * - 设计文档 AI 基于上下文 prompt → 本 Spec 增强为 RAG 检索引用可溯源
 * - 设计文档无"解释我的答案"机制 → 本 Spec 新增
 */
class SocraticTutor @Inject constructor(
    private val ragEngine: RagEngine,
    private val aiService: AiService,
) {

    /**
     * 苏格拉底式引导论述题作答（Spec 第 358-364 行）。
     *
     * 流程：
     * 1. AI 不直接给出完整答案
     * 2. AI 先让用户尝试作答
     * 3. AI 分析用户答案的论证漏洞（[SocraticStage.ANALYZE]）
     * 4. AI 提供改进建议而非标准答案（[SocraticStage.SUGGEST]）
     * 5. 最后展示范文供对比，标注"范文，非标准答案"（[SocraticStage.SHOW_SAMPLE]）
     *
     * @param question 论述题题目
     * @param userAnswer 用户已提交的答案
     * @return 按阶段流式输出的苏格拉底引导内容
     */
    fun guideEssayAnswer(question: String, userAnswer: String): Flow<SocraticGuide> = flow {
        // 先验证用户答案是否有效
        val validation = validateUserAnswer(userAnswer)
        if (!validation.isValid) {
            // 答案过短或离题时不强行分析论证漏洞，引导用户先回顾知识点
            emit(SocraticGuide(
                stage = SocraticStage.ANALYZE,
                content = validation.suggestion ?: "答案内容不足，建议先回顾相关知识点后再作答。",
                isSampleEssay = false,
                contentSource = CONTENT_SOURCE_AI,
            ))
            return@flow
        }

        // 通过 RAG 检索相关资料
        ragEngine.search(question).collect { ragResult ->
            if (!ragResult.hasResults) {
                // RAG 无结果时不编造答案，告知用户
                emit(SocraticGuide(
                    stage = SocraticStage.ANALYZE,
                    content = ragResult.message + "，建议查阅相关教材后再尝试。",
                    isSampleEssay = false,
                    contentSource = CONTENT_SOURCE_AI,
                ))
                return@collect
            }

            // 阶段1：分析论证漏洞
            emit(SocraticGuide(
                stage = SocraticStage.ANALYZE,
                content = analyzeArguments(question, userAnswer, ragResult.references),
                isSampleEssay = false,
                contentSource = CONTENT_SOURCE_AI,
            ))

            // 阶段2：提供改进建议（而非标准答案）
            emit(SocraticGuide(
                stage = SocraticStage.SUGGEST,
                content = suggestImprovements(question, userAnswer, ragResult.references),
                isSampleEssay = false,
                contentSource = CONTENT_SOURCE_AI,
            ))

            // 阶段3：展示范文供对比（标注"范文，非标准答案"）
            emit(SocraticGuide(
                stage = SocraticStage.SHOW_SAMPLE,
                content = buildSampleEssay(question, ragResult.references),
                isSampleEssay = true,
                contentSource = CONTENT_SOURCE_AI,
            ))
        }
    }

    /**
     * "解释我的答案"机制（Spec 第 366-370 行）。
     *
     * 用户答错后：
     * 1. AI 分析用户答案的错误思路
     * 2. 解释为什么错、正确思路是什么
     * 3. 引用用户资料库中的相关知识点作为依据（RAG 架构）
     *
     * @param question 题目
     * @param userAnswer 用户的错误答案
     * @param correctAnswer 正确答案
     * @return 流式输出的错误分析
     */
    fun explainWrongAnswer(
        question: String,
        userAnswer: String,
        correctAnswer: String,
    ): Flow<WrongAnswerExplanation> = flow {
        // 通过 RAG 检索相关知识点
        ragEngine.search(question).collect { ragResult ->
            emit(WrongAnswerExplanation(
                errorAnalysis = analyzeErrorThinking(question, userAnswer, correctAnswer),
                correctApproach = buildCorrectApproach(question, correctAnswer, ragResult.references),
                references = ragResult.references,
            ))
        }
    }

    /**
     * 验证用户答案是否有效（Spec 第 378-382 行）。
     *
     * 答案过短（<50字）或完全离题时：
     * - 不强行分析论证漏洞
     * - 提示"答案内容不足/偏离题目，建议先回顾相关知识点"
     * - 引导用户查看相关知识点后再作答
     *
     * @param userAnswer 用户答案
     * @return 验证结果
     */
    fun validateUserAnswer(userAnswer: String): AnswerValidation {
        val trimmed = userAnswer.trim()

        // 答案过短（<50字）
        if (trimmed.length < MIN_ANSWER_LENGTH) {
            return AnswerValidation(
                isValid = false,
                issue = "答案内容不足",
                suggestion = "答案内容不足，建议先回顾相关知识点后再作答。",
            )
        }

        // 完全离题检测（简单启发式：答案与题目无关键词重叠时判定为离题）
        // TODO: 后续可结合 L2 语义相似度做更准确的离题判断
        if (isOffTopic(trimmed)) {
            return AnswerValidation(
                isValid = false,
                issue = "偏离题目",
                suggestion = "答案偏离题目，建议先回顾相关知识点后再作答。",
            )
        }

        return AnswerValidation(
            isValid = true,
            issue = null,
            suggestion = null,
        )
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────

    /** 分析论证漏洞（苏格拉底式：指出问题而非直接给答案） */
    private fun analyzeArguments(
        question: String,
        userAnswer: String,
        references: List<RagReference>,
    ): String {
        // TODO: 调用 AI 服务分析论证漏洞
        // 苏格拉底式：指出论证中的薄弱环节，引导用户思考而非直接给出正确答案
        val refHint = if (references.isNotEmpty()) {
            "可参考：${references.joinToString("；") { "${it.sourceFile}P${it.sourcePage}" }}"
        } else {
            ""
        }
        return "你的回答中有以下论证环节可以进一步思考：论点的展开深度、论据的充分性、论证逻辑的严密性。$refHint"
    }

    /** 提供改进建议（而非标准答案） */
    private fun suggestImprovements(
        question: String,
        userAnswer: String,
        references: List<RagReference>,
    ): String {
        // TODO: 调用 AI 服务生成改进建议
        // 苏格拉底式：建议方向而非给出完整答案
        return "建议从以下角度改进：补充时代背景、增加具体作品例证、梳理文学流派的承继关系。注意这不是标准答案，而是改进方向。"
    }

    /** 构建范文（标注"范文，非标准答案"） */
    private fun buildSampleEssay(
        question: String,
        references: List<RagReference>,
    ): String {
        // TODO: 调用 AI 服务生成范文
        val refCitations = references.joinToString("\n") { ref ->
            "— ${ref.sourceFile} P${ref.sourcePage}"
        }
        return "【范文，非标准答案】\n\n（此处为基于资料库生成的参考范文，供对比学习使用。）\n\n参考来源：\n$refCitations"
    }

    /** 分析错误思路 */
    private fun analyzeErrorThinking(
        question: String,
        userAnswer: String,
        correctAnswer: String,
    ): String {
        // TODO: 调用 AI 服务分析错误思路
        return "你的答案在以下思路上存在偏差：知识记忆不完整、概念混淆、论证方向偏离。"
    }

    /** 构建正确思路 */
    private fun buildCorrectApproach(
        question: String,
        correctAnswer: String,
        references: List<RagReference>,
    ): String {
        val refHint = if (references.isNotEmpty()) {
            "\n\n依据：${references.joinToString("；") { "${it.sourceFile}P${it.sourcePage}" }}"
        } else {
            ""
        }
        return "正确思路：$correctAnswer$refHint"
    }

    /** 简单离题检测（启发式） */
    private fun isOffTopic(answer: String): Boolean {
        // TODO: 后续结合 L2 语义相似度做准确判断
        // 当前启发式：答案中若完全不含任何中文字符或仅含标点，判定为离题
        val hasChinese = answer.any { it.code in 0x4E00..0x9FFF }
        return !hasChinese
    }

    companion object {
        /** 最小有效答案长度（Spec 第 379 行：<50字） */
        private const val MIN_ANSWER_LENGTH = 50

        /** AI 生成内容来源标识 */
        private const val CONTENT_SOURCE_AI = "AI_GENERATED"
    }
}

/**
 * 苏格拉底式引导阶段。
 *
 * @property ANALYZE 分析论证漏洞
 * @property SUGGEST 提供改进建议（而非标准答案）
 * @property SHOW_SAMPLE 展示范文供对比（标注"范文，非标准答案"）
 */
enum class SocraticStage {
    ANALYZE,
    SUGGEST,
    SHOW_SAMPLE,
}

/**
 * 苏格拉底式引导内容。
 *
 * @param stage 引导阶段
 * @param content 引导内容
 * @param isSampleEssay 是否为范文（true 时标注"范文，非标准答案"）
 * @param contentSource 内容来源，苏格拉底引导为 AI_GENERATED
 */
data class SocraticGuide(
    val stage: SocraticStage,
    val content: String,
    val isSampleEssay: Boolean,
    val contentSource: String,
)

/**
 * 答错后的错误分析（"解释我的答案"机制）。
 *
 * @param errorAnalysis 错误思路分析
 * @param correctApproach 正确思路
 * @param references RAG 引用（用户资料库），可溯源
 */
data class WrongAnswerExplanation(
    val errorAnalysis: String,
    val correctApproach: String,
    val references: List<RagReference>,
)

/**
 * 用户答案验证结果。
 *
 * @param isValid 答案是否有效
 * @param issue 问题类型（"答案内容不足" / "偏离题目"），有效时为 null
 * @param suggestion 建议（"建议先回顾相关知识点"），有效时为 null
 */
data class AnswerValidation(
    val isValid: Boolean,
    val issue: String?,
    val suggestion: String?,
)
