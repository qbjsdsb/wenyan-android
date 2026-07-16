package com.wenyan.app.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 苏格拉底式 AI 导师。
 *
 * Spec 第 344-388 行要求（增强设计文档 3.6 节 AI 助手，而非替代）：
 * - 苏格拉底式引导：不直接给答案，引导用户自己找到答案
 * - "解释我的答案"机制：答错后 AI 分析错误思路
 * - RAG 架构：基于用户资料库 + 权威教材库检索，引用可溯源
 * - 用户答案过短或离题时不强行分析论证漏洞
 *
 * 实现要点（阶段4）：
 * - 私有方法通过 [aiService].chat() 调用 LLM API
 * - [PromptTemplates] 统一管理所有 prompt
 * - RAG 无结果时降级为通用引导
 */
@Singleton
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
     * P1-6 修复：三阶段失败短路。
     * 原实现 [aiService].chat() 把所有异常吞为 emit(errorString)，调用方无法区分
     * "AI 真实回复" vs "错误提示字符串"。阶段1失败返回错误字符串时，阶段2/3 仍会继续，
     * 把错误字符串当作"分析结果"传给下一阶段 prompt，导致错误层层传播。
     * 现改用 [AiService.chatResult] 区分成功/失败：
     * - 阶段1失败 → emit 错误提示并 return，不执行阶段2/3
     * - 阶段2失败 → emit 阶段1结果 + 阶段2错误提示并 return，不执行阶段3
     * - 阶段3失败 → emit 阶段1+2结果 + 阶段3错误提示（最后阶段，仍 emit 给用户反馈）
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
        val ragResult = ragEngine.search(question).first()
        if (!ragResult.hasResults) {
            // RAG 无结果时不编造答案，告知用户
            emit(SocraticGuide(
                stage = SocraticStage.ANALYZE,
                content = ragResult.message + "，建议查阅相关教材后再尝试。",
                isSampleEssay = false,
                contentSource = CONTENT_SOURCE_AI,
            ))
            return@flow
        }

        val references = ragResult.references

        // 阶段1：分析论证漏洞
        val analysisResult = analyzeArguments(question, userAnswer, references)
        if (analysisResult.isFailure) {
            // P1-6 修复：阶段1失败短路，不执行阶段2/3
            emit(SocraticGuide(
                stage = SocraticStage.ANALYZE,
                content = "AI 分析失败：${analysisResult.exceptionOrNull()?.message ?: "未知错误"}",
                isSampleEssay = false,
                contentSource = CONTENT_SOURCE_AI,
            ))
            return@flow
        }
        val analysis = analysisResult.getOrThrow()
        emit(SocraticGuide(
            stage = SocraticStage.ANALYZE,
            content = analysis,
            isSampleEssay = false,
            contentSource = CONTENT_SOURCE_AI,
        ))

        // 阶段2：提供改进建议（而非标准答案）
        // NF-BB2: 传入阶段1分析结果作为上下文，使建议能针对具体问题
        val suggestionResult = suggestImprovements(question, userAnswer, references, analysis)
        if (suggestionResult.isFailure) {
            // P1-6 修复：阶段2失败短路，不执行阶段3
            emit(SocraticGuide(
                stage = SocraticStage.SUGGEST,
                content = "AI 建议生成失败：${suggestionResult.exceptionOrNull()?.message ?: "未知错误"}",
                isSampleEssay = false,
                contentSource = CONTENT_SOURCE_AI,
            ))
            return@flow
        }
        val suggestion = suggestionResult.getOrThrow()
        emit(SocraticGuide(
            stage = SocraticStage.SUGGEST,
            content = suggestion,
            isSampleEssay = false,
            contentSource = CONTENT_SOURCE_AI,
        ))

        // 阶段3：展示范文供对比（标注"范文，非标准答案"）
        // NF-BB2: 传入阶段1+2结果作为上下文，使范文能体现改进方向
        // P1-6 修复：阶段3是最后阶段，失败时仍 emit 错误提示给用户反馈
        val sampleResult = buildSampleEssay(question, references, analysis, suggestion)
        val sampleContent = if (sampleResult.isSuccess) {
            sampleResult.getOrThrow()
        } else {
            "范文生成失败：${sampleResult.exceptionOrNull()?.message ?: "未知错误"}"
        }
        emit(SocraticGuide(
            stage = SocraticStage.SHOW_SAMPLE,
            content = sampleContent,
            isSampleEssay = true,
            contentSource = CONTENT_SOURCE_AI,
        ))
    }

    /**
     * "解释我的答案"机制（Spec 第 366-370 行）。
     *
     * 用户答错后：
     * 1. AI 分析用户答案的错误思路
     * 2. 解释为什么错、正确思路是什么
     * 3. 引用用户资料库中的相关知识点作为依据（RAG 架构）
     *
     * P1-6 修复：两阶段失败短路。
     * - errorAnalysis 失败 → emit 错误提示并 return，不执行 correctApproach
     * - correctApproach 失败 → 仍 emit errorAnalysis + correctApproach 错误提示
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
        val ragResult = ragEngine.search(question).first()
        val references = if (ragResult.hasResults) ragResult.references else emptyList()

        // P1-6 修复：errorAnalysis 失败短路
        val errorAnalysisResult = analyzeErrorThinking(question, userAnswer, correctAnswer)
        if (errorAnalysisResult.isFailure) {
            emit(WrongAnswerExplanation(
                errorAnalysis = "AI 错误分析失败：${errorAnalysisResult.exceptionOrNull()?.message ?: "未知错误"}",
                correctApproach = "",
                references = references,
            ))
            return@flow
        }
        val errorAnalysis = errorAnalysisResult.getOrThrow()

        // correctApproach 失败时仍 emit（最后阶段，给用户反馈）
        val correctApproachResult = buildCorrectApproach(question, correctAnswer, references)
        val correctApproach = if (correctApproachResult.isSuccess) {
            correctApproachResult.getOrThrow()
        } else {
            "AI 正确思路生成失败：${correctApproachResult.exceptionOrNull()?.message ?: "未知错误"}"
        }

        emit(WrongAnswerExplanation(
            errorAnalysis = errorAnalysis,
            correctApproach = correctApproach,
            references = references,
        ))
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

        // 完全离题检测（简单启发式：答案中无中文字符时判定为离题）
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

    // ── 私有辅助方法（通过 AiService.chatResult 调用 LLM，P1-6 改造） ───

    /** 分析论证漏洞（苏格拉底式：指出问题而非直接给答案）。
     *  P1-6: 返回 Result<String> 供调用方短路 */
    private suspend fun analyzeArguments(
        question: String,
        userAnswer: String,
        references: List<RagReference>,
    ): Result<String> {
        val prompt = PromptTemplates.buildAnalyzePrompt(question, userAnswer, references)
        return aiService.chatResult(prompt).first()
    }

    /** 提供改进建议（而非标准答案）。
     *  NF-BB2: [previousAnalysis] 传入阶段1分析结果作为上下文。
     *  P1-6: 返回 Result<String> 供调用方短路 */
    private suspend fun suggestImprovements(
        question: String,
        userAnswer: String,
        references: List<RagReference>,
        previousAnalysis: String = "",
    ): Result<String> {
        val prompt = PromptTemplates.buildSuggestPrompt(question, userAnswer, references, previousAnalysis)
        return aiService.chatResult(prompt).first()
    }

    /** 构建范文（标注"范文，非标准答案"）。
     *  NF-BB2: [previousAnalysis] + [previousSuggestion] 传入前两阶段结果作为上下文。
     *  P1-6: 返回 Result<String> 供调用方短路 */
    private suspend fun buildSampleEssay(
        question: String,
        references: List<RagReference>,
        previousAnalysis: String = "",
        previousSuggestion: String = "",
    ): Result<String> {
        val prompt = PromptTemplates.buildSampleEssayPrompt(question, references, previousAnalysis, previousSuggestion)
        return aiService.chatResult(prompt).first()
    }

    /** 分析错误思路。
     *  P1-6: 返回 Result<String> 供调用方短路 */
    private suspend fun analyzeErrorThinking(
        question: String,
        userAnswer: String,
        correctAnswer: String,
    ): Result<String> {
        val prompt = PromptTemplates.buildErrorAnalysisPrompt(question, userAnswer, correctAnswer)
        return aiService.chatResult(prompt).first()
    }

    /** 构建正确思路。
     *  P1-6: 返回 Result<String> 供调用方短路 */
    private suspend fun buildCorrectApproach(
        question: String,
        correctAnswer: String,
        references: List<RagReference>,
    ): Result<String> {
        val prompt = PromptTemplates.buildCorrectApproachPrompt(question, correctAnswer, references)
        return aiService.chatResult(prompt).first()
    }

    /** 简单离题检测（启发式：无中文字符判定为离题） */
    private fun isOffTopic(answer: String): Boolean {
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
