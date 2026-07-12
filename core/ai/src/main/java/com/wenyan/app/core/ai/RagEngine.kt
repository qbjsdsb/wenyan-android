package com.wenyan.app.core.ai

import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG（检索增强生成）引擎。
 *
 * Spec 第 53-55 行、第 372-388 行要求：
 * - 基于用户资料库 + 权威教材库做 RAG 检索
 * - 回答中标注引用来源（如"据袁行霈《中国文学史》第二卷 P156"）
 * - 区分资料原文（TEXTBOOK_NATIVE / TEXTBOOK_OCR）与 AI 生成内容
 * - 无相关结果时不编造答案，明确告知用户"该问题不在当前资料库覆盖范围内"
 *
 * 实现方案（阶段4）：
 * - 关键词检索（SQLite LIKE），在 title / core_conclusion / full_content / study_text 四字段搜索
 * - 后续阶段5可升级为向量检索（需嵌入模型）
 *
 * @property knowledgePointDao 知识点 DAO（用于关键词搜索）
 */
@Singleton
class RagEngine @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao,
) {

    /**
     * 检索用户资料库 + 权威教材库。
     *
     * 流程：
     * 1. 从用户提问中提取关键词（去掉疑问句式）
     * 2. 用关键词在知识点表中做 LIKE 搜索
     * 3. 将匹配的知识点转换为 [RagReference]（含来源文件/页码/内容类型/摘录）
     * 4. 无结果时返回 [NO_RESULT_MESSAGE]
     *
     * @param query 用户提问或检索关键词
     * @return 检索结果，无相关结果时 [RagResult.hasResults] 为 false
     */
    fun search(query: String): Flow<RagResult> = flow {
        val keyword = extractKeyword(query)
        if (keyword.isBlank()) {
            emit(RagResult(
                hasResults = false,
                references = emptyList(),
                message = NO_RESULT_MESSAGE,
            ))
            return@flow
        }

        val results = knowledgePointDao.searchByKeyword(keyword, limit = MAX_RESULTS)
        if (results.isEmpty()) {
            emit(RagResult(
                hasResults = false,
                references = emptyList(),
                message = NO_RESULT_MESSAGE,
            ))
            return@flow
        }

        val references = results.map { it.toRagReference() }
        emit(RagResult(
            hasResults = true,
            references = references,
            message = "找到 ${references.size} 条相关资料",
        ))
    }

    /**
     * 从用户提问中提取搜索关键词。
     *
     * 处理步骤：
     * 1. 去掉常见疑问句式前缀（"什么是"、"请简述"等）
     * 2. 去掉常见疑问句式后缀（"是什么"、"的含义"等）
     * 3. 如果关键词中仍包含疑问标记词（如"苏轼是什么人"），截取标记词前的部分
     */
    private fun extractKeyword(query: String): String {
        var keyword = query.trim()

        // 去掉常见疑问句式前缀
        val prefixes = listOf("什么是", "什么叫", "请简述", "简述", "请论述", "论述", "请分析", "分析", "请说明", "说明", "请解释", "解释", "如何理解", "谈谈对", "谈谈")
        for (prefix in prefixes) {
            if (keyword.startsWith(prefix)) {
                keyword = keyword.removePrefix(prefix)
                break
            }
        }

        // 去掉常见疑问句式后缀
        val suffixes = listOf("是什么", "是什么意思", "的含义", "的意义", "的概念", "的特点", "的特征", "的影响", "的作用", "的关系", "的区别", "？", "?")
        for (suffix in suffixes) {
            if (keyword.endsWith(suffix)) {
                keyword = keyword.removeSuffix(suffix)
                break
            }
        }

        // 如果关键词中仍包含疑问标记词，截取标记词前的部分
        // 例如"苏轼是什么人" → "苏轼"
        val questionMarkers = listOf("是什么", "是怎样的", "怎么样", "为什么", "为何", "何为", "哪些", "哪个")
        for (marker in questionMarkers) {
            val index = keyword.indexOf(marker)
            if (index > 0) {
                keyword = keyword.substring(0, index)
                break
            }
        }

        return keyword.trim()
    }

    /** 将 [KnowledgePointEntity] 转换为 [RagReference] */
    private fun KnowledgePointEntity.toRagReference(): RagReference {
        val excerpt = buildExcerpt()
        return RagReference(
            sourceFile = sourceFile ?: title,
            sourcePage = sourcePage ?: 0,
            contentSource = contentSource ?: "TEXTBOOK_NATIVE",
            excerpt = excerpt,
        )
    }

    /** 构造引用摘录（coreConclusion 优先，否则取 fullContent 前 200 字） */
    private fun KnowledgePointEntity.buildExcerpt(): String {
        if (!coreConclusion.isNullOrBlank()) return coreConclusion.take(MAX_EXCERPT_LENGTH)
        if (!fullContent.isNullOrBlank()) return fullContent.take(MAX_EXCERPT_LENGTH)
        if (!studyText.isNullOrBlank()) return studyText.take(MAX_EXCERPT_LENGTH)
        return title
    }

    companion object {
        /** RAG 检索无相关结果时的提示语（Spec 第 386 行） */
        const val NO_RESULT_MESSAGE = "该问题不在当前资料库覆盖范围内"

        /** 最大返回结果数 */
        private const val MAX_RESULTS = 5

        /** 摘录最大长度 */
        private const val MAX_EXCERPT_LENGTH = 200
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
