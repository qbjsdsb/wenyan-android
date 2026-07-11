package com.wenyan.app.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * RAG（检索增强生成）引擎。
 *
 * Spec 第 53-55 行、第 372-388 行要求：
 * - 基于用户资料库 + 权威教材库做 RAG 检索
 * - 回答中标注引用来源（如"据袁行霈《中国文学史》第二卷 P156"）
 * - 区分资料原文（TEXTBOOK_NATIVE / TEXTBOOK_OCR）与 AI 生成内容
 * - 无相关结果时不编造答案，明确告知用户"该问题不在当前资料库覆盖范围内"
 *
 * 内容来源五级标注（Spec 第 43-47 行、第 201-205 行）：
 * - TEXTBOOK_NATIVE：原生电子文本
 * - TEXTBOOK_OCR：扫描 OCR 文本
 * - AI_GENERATED：AI 生成内容
 * - HYBRID：混合（资料 + AI）
 * - USER_CREATED：用户创建
 * - MISSING：OCR 失败 / 资料缺失（特殊状态）
 */
class RagEngine @Inject constructor(

) {

    /**
     * 检索用户资料库 + 权威教材库。
     *
     * @param query 用户提问或检索关键词
     * @return 检索结果，无相关结果时 [RagResult.hasResults] 为 false
     */
    fun search(query: String): Flow<RagResult> = flow {
        // TODO: 实现实际检索逻辑（向量检索 / 关键词检索）
        // 当前为骨架实现，后续接入向量数据库或全文索引
        emit(RagResult(
            hasResults = false,
            references = emptyList(),
            message = NO_RESULT_MESSAGE,
        ))
    }

    companion object {
        /** RAG 检索无相关结果时的提示语（Spec 第 386 行） */
        const val NO_RESULT_MESSAGE = "该问题不在当前资料库覆盖范围内"
    }
}

/**
 * RAG 检索结果。
 *
 * @param hasResults 是否有相关检索结果
 * @param references 引用来源列表（可溯源）
 * @param message 提示信息；无结果时为"该问题不在当前资料库覆盖范围内"
 */
data class RagResult(
    val hasResults: Boolean,
    val references: List<RagReference>,
    val message: String,
)

/**
 * RAG 引用来源（可溯源）。
 *
 * @param sourceFile 来源文件，如"袁行霈《中国文学史》第二卷"
 * @param sourcePage 来源页码，如 156
 * @param contentSource 内容来源类型：TEXTBOOK_NATIVE / TEXTBOOK_OCR
 * @param excerpt 引用原文片段
 */
data class RagReference(
    val sourceFile: String,
    val sourcePage: Int,
    val contentSource: String,
    val excerpt: String,
)
