package com.wenyan.app.core.ai

import app.cash.turbine.test
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [SocraticTutor] 单元测试。
 *
 * 验证：
 * - validateUserAnswer 过短/离题/正常
 * - guideEssayAnswer 三阶段输出
 * - RAG 无结果时的降级处理
 * - explainWrongAnswer 错误分析输出
 */
class SocraticTutorTest {

    private lateinit var ragEngine: RagEngine
    private lateinit var aiService: FakeAiService
    private lateinit var tutor: SocraticTutor

    @Before
    fun setup() {
        // RagEngine 用真实实现 + FakeKnowledgePointDao
        val dao = FakeKnowledgePointDao(searchResults = listOf(
            sampleEntity(id = "kp1", title = "苏轼", coreConclusion = "北宋文学家"),
        ))
        ragEngine = RagEngine(dao)

        // AiService 用 Fake 实现
        aiService = FakeAiService()
        tutor = SocraticTutor(ragEngine, aiService)
    }

    @Test
    fun `validateUserAnswer 答案过短时返回无效`() {
        val validation = tutor.validateUserAnswer("太短了")
        assertFalse(validation.isValid)
        assertEquals("答案内容不足", validation.issue)
    }

    @Test
    fun `validateUserAnswer 答案无中文时返回离题`() {
        val validation = tutor.validateUserAnswer("This is a long enough answer in English without any Chinese characters.")
        assertFalse(validation.isValid)
        assertEquals("偏离题目", validation.issue)
    }

    @Test
    fun `validateUserAnswer 答案正常时返回有效`() {
        val answer = "苏轼是北宋著名的文学家，他在诗、词、文、书、画等方面都有很高的成就。" +
            "他的词开创了豪放派，对后世文学产生了深远的影响。"
        val validation = tutor.validateUserAnswer(answer)
        assertTrue(validation.isValid)
    }

    @Test
    fun `guideEssayAnswer 答案过短时返回引导提示`() = runTest {
        tutor.guideEssayAnswer("论述苏轼的文学成就", "太短").test {
            val guide = awaitItem()
            assertEquals(SocraticStage.ANALYZE, guide.stage)
            assertTrue(guide.content.contains("不足") || guide.content.contains("回顾"))
            assertFalse(guide.isSampleEssay)
            awaitComplete()
        }
    }

    @Test
    fun `guideEssayAnswer RAG 无结果时返回降级提示`() = runTest {
        // 用空搜索结果的 DAO 构造 RagEngine
        val emptyDao = FakeKnowledgePointDao(searchResults = emptyList())
        val emptyRagEngine = RagEngine(emptyDao)
        val emptyTutor = SocraticTutor(emptyRagEngine, aiService)

        val longAnswer = "这是一段足够长的答案，包含足够的中文字符，用于测试RAG无结果时的降级处理逻辑。" +
            "这段答案的长度超过了五十字，确保通过长度验证。"

        emptyTutor.guideEssayAnswer("量子力学原理", longAnswer).test {
            val guide = awaitItem()
            assertEquals(SocraticStage.ANALYZE, guide.stage)
            assertTrue(guide.content.contains("不在当前资料库") || guide.content.contains("查阅"))
            awaitComplete()
        }
    }

    @Test
    fun `guideEssayAnswer 正常答案输出三阶段`() = runTest {
        aiService.response = "AI 分析结果"

        val longAnswer = "苏轼是北宋著名的文学家，他在诗、词、文、书、画等方面都有很高的成就。" +
            "他的词开创了豪放派，对后世文学产生了深远的影响。"

        tutor.guideEssayAnswer("苏轼", longAnswer).test {
            // 阶段1：分析论证漏洞
            val analyze = awaitItem()
            assertEquals(SocraticStage.ANALYZE, analyze.stage)
            assertFalse(analyze.isSampleEssay)

            // 阶段2：改进建议
            val suggest = awaitItem()
            assertEquals(SocraticStage.SUGGEST, suggest.stage)

            // 阶段3：范文
            val sample = awaitItem()
            assertEquals(SocraticStage.SHOW_SAMPLE, sample.stage)
            assertTrue(sample.isSampleEssay)

            awaitComplete()
        }
    }

    @Test
    fun `explainWrongAnswer 输出错误分析和正确思路`() = runTest {
        aiService.response = "AI 分析结果"

        tutor.explainWrongAnswer("苏轼", "错误答案", "正确答案").test {
            val explanation = awaitItem()
            assertEquals("AI 分析结果", explanation.errorAnalysis)
            assertEquals("AI 分析结果", explanation.correctApproach)
            awaitComplete()
        }
    }

    private fun sampleEntity(
        id: String,
        title: String,
        coreConclusion: String = "",
    ) = KnowledgePointEntity(
        id = id,
        chapterId = "ch1",
        title = title,
        summary = null,
        coreConclusion = coreConclusion,
        fullContent = "",
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
        contentSource = "TEXTBOOK_NATIVE",
        ocrStatus = "VERIFIED",
        sourceFile = "中国文学史",
        sourcePage = 100,
        studyText = null,
    )
}

/**
 * [AiService] 的 Fake 实现，供单元测试使用。
 */
class FakeAiService(
    var response: String = "默认 AI 回复",
) : AiService {

    override fun chat(query: String): Flow<String> = flowOf(response)

    override fun isAvailable(): Flow<Boolean> = flowOf(true)
}
