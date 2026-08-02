package com.wenyan.app.core.ai

import app.cash.turbine.test
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [RagEngine] 单元测试。
 *
 * 验证：
 * - 关键词命中返回正确 RagReference
 * - 无结果时返回 NO_RESULT_MESSAGE
 * - 疑问句式正确提取关键词
 * - RagReference 字段映射正确（sourceFile/sourcePage/contentSource/excerpt）
 */
class RagEngineTest {

    @Test
    fun `search 有关键词命中时返回 RagReference`() = runTest {
        val entity = sampleEntity(
            id = "kp1",
            title = "唐宋八大家",
            coreConclusion = "韩愈、柳宗元、欧阳修、苏洵、苏轼、苏辙、王安石、曾巩",
            sourceFile = "中国文学史",
            sourcePage = 156,
            contentSource = "TEXTBOOK_NATIVE",
        )
        val dao = FakeKnowledgePointDao(searchResults = listOf(entity))
        val engine = RagEngine(dao)

        engine.search("什么是唐宋八大家？").test {
            val result = awaitItem()
            assertTrue(result.hasResults)
            assertEquals(1, result.references.size)

            val ref = result.references.first()
            assertEquals("中国文学史", ref.sourceFile)
            assertEquals(156, ref.sourcePage)
            assertEquals("TEXTBOOK_NATIVE", ref.contentSource)
            assertTrue(ref.excerpt.contains("韩愈"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search 无结果时返回 NO_RESULT_MESSAGE`() = runTest {
        val dao = FakeKnowledgePointDao(searchResults = emptyList())
        val engine = RagEngine(dao)

        engine.search("量子力学").test {
            val result = awaitItem()
            assertFalse(result.hasResults)
            assertEquals(RagEngine.NO_RESULT_MESSAGE, result.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * v0.9.23 P2-1 回归：DAO 抛异常时 RAG 检索降级为"无结果"，
     * 不沿 flow 抛出导致主流程（AI 调用）失败。
     */
    @Test
    fun `search DAO异常时降级为无结果不崩溃`() = runTest {
        val dao = FakeKnowledgePointDao(searchResults = emptyList()).apply { throwOnSearch = true }
        val engine = RagEngine(dao)

        engine.search("苏轼").test {
            val result = awaitItem()
            assertFalse("DAO 异常应降级为无结果", result.hasResults)
            assertEquals(RagEngine.NO_RESULT_MESSAGE, result.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search 疑问句式正确提取关键词`() = runTest {
        val entity = sampleEntity(
            id = "kp1",
            title = "苏轼",
            coreConclusion = "北宋文学家",
            fullContent = "苏轼的贡献在于词的开创",
        )
        val dao = FakeKnowledgePointDao(searchResults = listOf(entity))
        val engine = RagEngine(dao)

        // 各种疑问句式都应能提取"苏轼"或"苏轼的贡献"
        val queries = listOf("苏轼是什么人", "什么是苏轼", "简述苏轼", "请论述苏轼的贡献", "苏轼？")
        for (query in queries) {
            engine.search(query).test {
                val result = awaitItem()
                assertTrue("查询 '$query' 应有结果", result.hasResults)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `search 空 query 返回无结果`() = runTest {
        val dao = FakeKnowledgePointDao(searchResults = listOf(sampleEntity(id = "kp1", title = "测试")))
        val engine = RagEngine(dao)

        engine.search("").test {
            val result = awaitItem()
            assertFalse(result.hasResults)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RagReference 摘录优先取 coreConclusion`() = runTest {
        val entity = sampleEntity(
            id = "kp1",
            title = "李白",
            coreConclusion = "诗仙，浪漫主义诗人代表",
            fullContent = "李白（701-762），字太白，号青莲居士...",
            studyText = "李白是唐代最伟大的诗人之一",
        )
        val dao = FakeKnowledgePointDao(searchResults = listOf(entity))
        val engine = RagEngine(dao)

        engine.search("李白").test {
            val result = awaitItem()
            val ref = result.references.first()
            assertEquals("诗仙，浪漫主义诗人代表", ref.excerpt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sampleEntity(
        id: String,
        title: String,
        coreConclusion: String = "",
        fullContent: String = "",
        studyText: String? = null,
        sourceFile: String? = null,
        sourcePage: Int? = null,
        contentSource: String? = null,
    ) = KnowledgePointEntity(
        id = id,
        chapterId = "ch1",
        title = title,
        summary = null,
        coreConclusion = coreConclusion,
        fullContent = fullContent,
        multiPerspectives = null,
        relatedIds = null,
        contrastIds = null,
        extensionIds = null,
        examRecords = null,
        examFrequency = "MEDIUM",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        contentSource = contentSource,
        ocrStatus = "VERIFIED",
        sourceFile = sourceFile,
        sourcePage = sourcePage,
        studyText = studyText,
    )
}
