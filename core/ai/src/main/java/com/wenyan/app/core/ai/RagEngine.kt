package com.wenyan.app.core.ai

import androidx.compose.runtime.Immutable
import com.wenyan.app.core.common.model.ContentSource
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG（检索增强生成）引擎。
 *
 * Spec 第 53-55 行、第 372-388 行要求：
 * - 基于用户资料库 + 权威教材库做 RAG 检索
 * - 回答中标注引用来源（如"据袁行霈《中国文学史》第二卷 P156"）
 * - 区分教材资料（含教材分歧标记）与 AI 生成内容
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
        // NF-BB10: 限制查询长度，防止超长输入导致 LIKE 搜索卡顿
        val truncatedQuery = query.take(MAX_QUERY_LENGTH)
        val keyword = extractKeyword(truncatedQuery)
        if (keyword.isBlank()) {
            emit(RagResult(
                hasResults = false,
                references = emptyList(),
                message = NO_RESULT_MESSAGE,
            ))
            return@flow
        }

        // v0.9.23 P2-1 修复：RAG 检索失败不应阻断主流程（AI 调用）。
        // 原实现 DAO 异常会沿 flow 抛出，导致 sendMessage/SocraticTutor 整个失败；
        // 现在 catch 降级为"无检索结果"，由调用方决定是否继续（无 RAG 也能回答）。
        val results = try {
            knowledgePointDao.searchByKeyword(escapeLikeWildcards(keyword), limit = MAX_RESULTS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "RAG searchByKeyword failed, degrade to no-results: ${e.message}")
            emptyList()
        }
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

        // 注意：v0.9.26 曾尝试剔除中文停用词（"的""了"等），后发现对 LIKE 匹配有害——
        // SQLite LIKE '%苏轼贡献%' 不匹配原文"苏轼的贡献"（中间有"的"），剔除反而降低命中率。
        // 正确方向是"多关键词 OR"（需 DAO 改造），本次不做（避免扩大改动面），保持单关键词整串匹配。

        return keyword.trim()
    }

    /**
     * 转义 SQLite LIKE 通配符（NF-BB1）。
     *
     * 将 `%` → `\%`，`_` → `\_`，`\\` → `\\\\`。
     * 配合 DAO 的 `ESCAPE '\\'` 子句，使搜索关键词中的 % 和 _ 被视为字面字符。
     * 例如搜索"100%"时，不再匹配"1000"等。
     */
    private fun escapeLikeWildcards(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    /** 将 [KnowledgePointEntity] 转换为 [RagReference] */
    private fun KnowledgePointEntity.toRagReference(): RagReference {
        val excerpt = buildExcerpt()
        return RagReference(
            sourceFile = sourceFile?.takeIf { it.isNotBlank() } ?: "资料库知识点：$title",
            sourcePage = sourcePage?.takeIf { it > 0 },
            contentSource = contentSource ?: ContentSource.TEXTBOOK_NATIVE,
            excerpt = excerpt,
        )
    }

    /** 构造引用摘录（coreConclusion 优先，否则取 fullContent 前 200 字） */
    private fun KnowledgePointEntity.buildExcerpt(): String {
        val cc = coreConclusion
        if (!cc.isNullOrBlank()) return cc.take(MAX_EXCERPT_LENGTH)
        val fc = fullContent
        if (!fc.isNullOrBlank()) return fc.take(MAX_EXCERPT_LENGTH)
        val st = studyText
        if (!st.isNullOrBlank()) return st.take(MAX_EXCERPT_LENGTH)
        return title
    }

    companion object {
        /** RAG 检索无相关结果时的提示语（Spec 第 386 行） */
        const val NO_RESULT_MESSAGE = "该问题不在当前资料库覆盖范围内"

        /** 最大返回结果数 */
        private const val MAX_RESULTS = 5

        /** 摘录最大长度 */
        private const val MAX_EXCERPT_LENGTH = 200

        /** 查询最大长度（NF-BB10：防超长输入导致 LIKE 搜索卡顿） */
        private const val MAX_QUERY_LENGTH = 500
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
 * @param sourcePage 来源页码，如 156；未知时为 null，禁止用 0 伪装成精确页码
 * @param contentSource 内容来源类型：TEXTBOOK_NATIVE / TEXTBOOK_OCR / TEXTBOOK_CONFLICT 等
 * @param excerpt 引用原文片段
 */
@Immutable
@Serializable
data class RagReference(
    val sourceFile: String,
    val sourcePage: Int? = null,
    val contentSource: String,
    val excerpt: String,
)

/** 用户可见的来源文本；仅在页码真实存在且大于 0 时显示页码。 */
fun RagReference.sourceLabel(): String = buildString {
    append(sourceFile)
    sourcePage?.takeIf { it > 0 }?.let { append(" P$it") }
}

/** 将内部来源代码转换为给模型和用户看的中文说明。 */
fun RagReference.contentSourceLabel(): String = when (contentSource) {
    ContentSource.TEXTBOOK_NATIVE -> "教材资料"
    ContentSource.TEXTBOOK_OCR -> "教材 OCR"
    ContentSource.TEXTBOOK_CONFLICT -> "不同教材表述存在分歧，需结合原教材复核"
    ContentSource.AI_GENERATED -> "AI 生成"
    ContentSource.HYBRID -> "资料与 AI 混合"
    ContentSource.USER_CREATED -> "用户资料"
    ContentSource.MISSING -> "来源缺失"
    else -> contentSource
}
