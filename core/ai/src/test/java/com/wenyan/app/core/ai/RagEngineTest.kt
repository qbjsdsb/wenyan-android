package com.wenyan.app.core.ai

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 31 - 知识图谱与AI助手测试
 * RagEngine 单元测试
 *
 * 验证 checklist 项：
 * - C5.6: 验证RAG架构（search方法返回Flow）
 * - C5.7: 验证无结果时返回NO_RESULT_MESSAGE
 * - 验证无结果时hasResults=false
 * - 验证无结果时references为空列表
 *
 * Spec 第 53-55 行、第 372-388 行要求：
 * - 基于用户资料库 + 权威教材库做 RAG 检索
 * - 无相关结果时不编造答案，明确告知用户"该问题不在当前资料库覆盖范围内"
 */

class RagEngineTest {

    private val ragEngine = RagEngine()

    // C5.6: 验证RAG架构（search方法返回Flow）
    @Test
    fun c5_6_search_returnsFlow() = runBlocking {
        val result = ragEngine.search("江西诗派的特点").first()

        assertNotNull("search 方法应返回 Flow<RagResult>", result)
    }

    // C5.6 补充：search 方法对不同查询均返回结果
    @Test
    fun c5_6_search_acceptsVariousQueries() = runBlocking {
        val queries = listOf("建安风骨", "黄庭坚", "宋诗特点", "")

        for (query in queries) {
            val result = ragEngine.search(query).first()
            assertNotNull("对查询'$query'应返回结果", result)
        }
    }

    // C5.7: 验证无结果时返回NO_RESULT_MESSAGE
    @Test
    fun c5_7_noResults_returnsNoResultMessage() = runBlocking {
        val result = ragEngine.search("任意查询").first()

        assertEquals(
            "无结果时应返回 NO_RESULT_MESSAGE",
            RagEngine.NO_RESULT_MESSAGE,
            result.message,
        )
    }

    // 验证无结果时hasResults=false
    @Test
    fun noResults_hasResultsIsFalse() = runBlocking {
        val result = ragEngine.search("任意查询").first()

        assertFalse("无结果时 hasResults 应为 false", result.hasResults)
    }

    // 验证无结果时references为空列表
    @Test
    fun noResults_referencesIsEmpty() = runBlocking {
        val result = ragEngine.search("任意查询").first()

        assertTrue("无结果时 references 应为空列表", result.references.isEmpty())
    }

    // 验证 NO_RESULT_MESSAGE 常量值
    @Test
    fun noResultMessage_constantIsCorrect() {
        assertEquals(
            "该问题不在当前资料库覆盖范围内",
            RagEngine.NO_RESULT_MESSAGE,
        )
    }

    // 验证 RagResult 数据类结构
    @Test
    fun ragResult_dataClassStructure() {
        val result = RagResult(
            hasResults = false,
            references = emptyList(),
            message = RagEngine.NO_RESULT_MESSAGE,
        )

        assertFalse(result.hasResults)
        assertTrue(result.references.isEmpty())
        assertEquals(RagEngine.NO_RESULT_MESSAGE, result.message)
    }

    // 验证 RagReference 数据类结构
    @Test
    fun ragReference_dataClassStructure() {
        val reference = RagReference(
            sourceFile = "袁行霈《中国文学史》第二卷",
            sourcePage = 156,
            contentSource = "TEXTBOOK_NATIVE",
            excerpt = "江西诗派是宋代文学流派...",
        )

        assertEquals("袁行霈《中国文学史》第二卷", reference.sourceFile)
        assertEquals(156, reference.sourcePage)
        assertEquals("TEXTBOOK_NATIVE", reference.contentSource)
        assertTrue(reference.excerpt.isNotBlank())
    }
}
