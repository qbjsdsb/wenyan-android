package com.wenyan.app.core.ai

/**
 * Prompt 模板管理（阶段4新增）。
 *
 * 统一管理苏格拉底式引导和"解释我的答案"机制的所有 prompt。
 * 设计原则：
 * - 苏格拉底式：不直接给答案，引导思考
 * - RAG 引用：将检索结果作为上下文注入
 * - 可溯源：要求 AI 标注引用来源
 */
object PromptTemplates {

    /**
     * 构建普通问答的 prompt（RAG 上下文 + 用户提问）。
     *
     * 用于 AiAssistantViewModel.sendMessage 场景：
     * - 将 RAG 检索结果作为参考资料注入
     * - 要求 AI 标注引用来源
     * - RAG 无结果时明确告知用户该问题不在资料库覆盖范围
     */
    fun buildChatPrompt(query: String, references: List<RagReference>): String {
        val refContext = formatReferences(references)
        return """请基于参考资料回答用户问题。

【用户问题】
$query

【参考资料】
$refContext

要求：
1. 优先基于参考资料回答，引用时标注来源（如"据《中国文学史》P156"）
2. 资料不足部分可补充常识，但需注明"以下为补充内容，非教材原文"
3. 若参考资料为"（无相关资料）"，请明确告知用户该问题不在当前资料库覆盖范围
4. 回答简洁清晰，适合移动端阅读"""
    }

    /**
     * 构建分析论证漏洞的 prompt（苏格拉底式：指出问题而非直接给答案）。
     */
    fun buildAnalyzePrompt(
        question: String,
        userAnswer: String,
        references: List<RagReference>,
    ): String {
        val refContext = formatReferences(references)
        return """请分析以下论述题答案的论证漏洞。

【题目】$question

【用户答案】
$userAnswer

【参考资料】
$refContext

请从以下角度分析（不要直接给出标准答案）：
1. 论点的展开是否充分
2. 论据是否具体、有说服力
3. 论证逻辑是否严密
4. 是否遗漏了重要知识点

请用引导性语言指出问题，帮助学生自己发现问题。"""
    }

    /**
     * 构建改进建议的 prompt（方向性建议，非标准答案）。
     */
    fun buildSuggestPrompt(
        question: String,
        userAnswer: String,
        references: List<RagReference>,
    ): String {
        val refContext = formatReferences(references)
        return """请为以下论述题答案提供改进建议。

【题目】$question

【用户答案】
$userAnswer

【参考资料】
$refContext

请提供方向性建议（不要给出完整标准答案）：
1. 可以补充哪些角度的内容
2. 哪些论据可以更加具体
3. 论证结构如何调整
4. 注意：这是改进方向，不是标准答案

请用鼓励性语言，帮助学生找到改进方向。"""
    }

    /**
     * 构建范文的 prompt（标注"范文，非标准答案"）。
     */
    fun buildSampleEssayPrompt(
        question: String,
        references: List<RagReference>,
    ): String {
        val refContext = formatReferences(references)
        return """请基于参考资料生成一篇参考范文。

【题目】$question

【参考资料】
$refContext

要求：
1. 开头标注"【范文，非标准答案】"
2. 结构清晰，论点明确，论据具体
3. 引用参考资料中的内容时标注来源
4. 篇幅控制在 500-800 字
5. 注意：这是供对比学习的参考范文，不是唯一正确答案"""
    }

    /**
     * 构建错误思路分析的 prompt（"解释我的答案"机制）。
     */
    fun buildErrorAnalysisPrompt(
        question: String,
        userAnswer: String,
        correctAnswer: String,
    ): String {
        return """请分析用户答案的错误思路。

【题目】$question

【用户答案】
$userAnswer

【正确答案】
$correctAnswer

请分析：
1. 用户答案在哪些方面存在偏差
2. 错误思路的可能成因（知识记忆不完整？概念混淆？论证方向偏离？）
3. 如何避免类似错误

请帮助学生理解自己的错误，而非简单批评。"""
    }

    /**
     * 构建正确思路的 prompt。
     */
    fun buildCorrectApproachPrompt(
        question: String,
        correctAnswer: String,
        references: List<RagReference>,
    ): String {
        val refContext = formatReferences(references)
        return """请构建这道题的正确思路。

【题目】$question

【正确答案】
$correctAnswer

【参考资料】
$refContext

请说明：
1. 正确的解题思路是什么
2. 应该从哪些角度切入
3. 引用参考资料中的依据

请标注引用来源，确保可溯源。"""
    }

    /** 格式化 RAG 引用为上下文文本 */
    private fun formatReferences(references: List<RagReference>): String {
        if (references.isEmpty()) return "（无相关资料）"
        return references.joinToString("\n\n") { ref ->
            "来源：${ref.sourceFile} P${ref.sourcePage}\n" +
                "类型：${ref.contentSource}\n" +
                "摘录：${ref.excerpt}"
        }
    }
}
