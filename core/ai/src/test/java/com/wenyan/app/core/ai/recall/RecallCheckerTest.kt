package com.wenyan.app.core.ai.recall

import com.wenyan.app.core.ai.AiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [RecallChecker] 单元测试（阶段4重写版）。
 *
 * 验证 checklist 项：
 * - C5.14: L1关键词匹配（TERM_EXPLANATION走L1，覆盖率<30%→AGAIN，30-60%→HARD，60-85%→GOOD，≥85%→EASY）
 * - C5.15: L2语义相似度（ESSAY走L2，覆盖率<60%→HARD，60-85%→部分正确触发L3，≥85%→EASY）
 * - C5.16: L3 LLM异步评估不阻塞主流程（L3需单独调用 checkL3Llm）
 * - C5.17: LLM输出0-100分及理由（L3的score和reason字段）
 *
 * 新实现变更（与旧骨架的差异）：
 * - L2 从 BGE-small-zh 改为 Jaccard bigram 相似度
 * - L1 加入同义词扩展（苏轼→苏东坡/子瞻/东坡 等）
 * - L3 通过 [AiService].chat() 调用 LLM，返回 JSON 由正则解析
 */
class RecallCheckerTest {

    private lateinit var aiService: FakeAiService
    private lateinit var checker: RecallChecker

    @Before
    fun setup() {
        aiService = FakeAiService()
        checker = RecallChecker(aiService)
    }

    // ── C5.14: L1 关键词匹配 ───────────────────────────────────
    // 注意：extractKeywords 过滤 length < 2 的词，必须用 ≥2 字的关键词。
    // 使用"春秋/战国/秦汉/唐宋"避免触发同义词扩展（不在 SYNONYM_MAP 中）。

    @Test
    fun c5_14_l1_lowCoverage_returnsAgain() = runTest {
        // 4个关键词，匹配1个 → 0.25 < 0.30 → AGAIN
        val correctAnswer = "春秋，战国，秦汉，唐宋"
        val userAnswer = "春秋"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(RecallRating.AGAIN, result.rating)
        assertEquals(0.25f, result.coverage, 0.001f)
    }

    @Test
    fun c5_14_l1_mediumCoverage_returnsHard() = runTest {
        // 4个关键词，匹配2个 → 0.50 → HARD
        val correctAnswer = "春秋，战国，秦汉，唐宋"
        val userAnswer = "春秋和战国"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(RecallRating.HARD, result.rating)
        assertEquals(0.50f, result.coverage, 0.001f)
    }

    @Test
    fun c5_14_l1_highCoverage_returnsGood() = runTest {
        // 4个关键词，匹配3个 → 0.75 → GOOD
        val correctAnswer = "春秋，战国，秦汉，唐宋"
        val userAnswer = "春秋战国秦汉"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(RecallRating.GOOD, result.rating)
        assertEquals(0.75f, result.coverage, 0.001f)
    }

    @Test
    fun c5_14_l1_fullCoverage_returnsEasy() = runTest {
        // 4个关键词，匹配4个 → 1.00 → EASY
        val correctAnswer = "春秋，战国，秦汉，唐宋"
        val userAnswer = "春秋战国秦汉唐宋"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(RecallRating.EASY, result.rating)
        assertEquals(1.00f, result.coverage, 0.001f)
    }

    @Test
    fun c5_14_l1_noMatch_returnsAgain() = runTest {
        val correctAnswer = "春秋，战国，秦汉，唐宋"
        val userAnswer = "完全无关"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.AGAIN, result.rating)
        assertEquals(0f, result.coverage, 0.001f)
    }

    // ── L1 同义词扩展验证 ───────────────────────────────────────

    @Test
    fun l1_synonymExpansion_苏东坡匹配苏轼() = runTest {
        // correctAnswer = "苏轼"（1个关键词）
        // expandWithSynonyms = ["苏轼", "苏东坡", "子瞻", "东坡"]
        // userAnswer = "苏东坡" → 匹配"苏东坡"和"东坡"（子串）→ 2/4 = 0.50 → HARD
        val correctAnswer = "苏轼"
        val userAnswer = "苏东坡"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        // 验证同义词扩展生效：用户答"苏东坡"也能匹配"苏轼"
        assertTrue("同义词扩展后覆盖率应 > 0", result.coverage > 0f)
        assertEquals(0.50f, result.coverage, 0.001f)
        assertEquals(RecallRating.HARD, result.rating)
    }

    @Test
    fun l1_synonymExpansion_诗仙匹配李白() = runTest {
        // correctAnswer = "李白"（1个关键词）
        // expandWithSynonyms = ["李白", "诗仙", "太白", "青莲居士"]
        // userAnswer = "诗仙" → 匹配"诗仙" → 1/4 = 0.25 → AGAIN
        val correctAnswer = "李白"
        val userAnswer = "诗仙"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertTrue("同义词扩展后覆盖率应 > 0", result.coverage > 0f)
        assertEquals(0.25f, result.coverage, 0.001f)
    }

    @Test
    fun l1_synonymExpansion_诗圣匹配杜甫() = runTest {
        // correctAnswer = "杜甫"（1个关键词）
        // expandWithSynonyms = ["杜甫", "诗圣", "子美", "少陵野老"]
        // userAnswer = "诗圣" → 匹配"诗圣" → 1/4 = 0.25
        val correctAnswer = "杜甫"
        val userAnswer = "诗圣"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertTrue("同义词扩展后覆盖率应 > 0", result.coverage > 0f)
    }

    // ── C5.15: L2 Jaccard 相似度 ───────────────────────────────

    @Test
    fun c5_15_l2_essayGoesThroughL2() = runTest {
        val result = checker.checkRecall("答案", "正确答案", QuestionType.ESSAY).first()

        assertEquals("ESSAY应走L2", RecallLevel.L2, result.level)
    }

    @Test
    fun c5_15_l2_identicalAnswers_returnsEasy() = runTest {
        // 完全相同 → Jaccard = 1.0 ≥ 0.85 → EASY
        val answer = "建安风骨是汉末建安时期诗歌的风格特征"
        val result = checker.checkRecall(answer, answer, QuestionType.ESSAY).first()

        assertEquals(RecallLevel.L2, result.level)
        assertEquals(RecallRating.EASY, result.rating)
        assertEquals(1.0f, result.coverage, 0.001f)
    }

    @Test
    fun c5_15_l2_completelyDifferent_returnsHard() = runTest {
        // 完全不同 → Jaccard = 0 < 0.60 → HARD
        val result = checker.checkRecall("量子力学", "建安风骨", QuestionType.ESSAY).first()

        assertEquals(RecallLevel.L2, result.level)
        assertEquals(RecallRating.HARD, result.rating)
        assertEquals(0f, result.coverage, 0.001f)
    }

    @Test
    fun c5_15_l2_partialSimilarity_returnsHard() = runTest {
        // 部分相似 → Jaccard < 0.60 → HARD
        val correctAnswer = "建安风骨是汉末建安时期诗歌的风格特征"
        val userAnswer = "建安风骨是一种风格"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.ESSAY).first()

        assertEquals(RecallLevel.L2, result.level)
        assertEquals(RecallRating.HARD, result.rating)
        assertTrue("覆盖率<60%应为HARD", result.coverage < 0.60f)
    }

    @Test
    fun c5_15_l2_scoreAndReasonAreNull() = runTest {
        val result = checker.checkRecall("答案", "正确答案", QuestionType.ESSAY).first()

        assertEquals(RecallLevel.L2, result.level)
        assertNull("L2结果 score 应为 null", result.score)
        assertNull("L2结果 reason 应为 null", result.reason)
    }

    // ── C5.16: L3 LLM 异步评估不阻塞主流程 ─────────────────────

    @Test
    fun c5_16_l3_doesNotBlockFlow_essayReturnsL2First() = runTest {
        // ESSAY类型 → L2，L3不在主流程中触发（需单独调用 checkL3Llm）
        val result = checker.checkRecall("答案", "正确答案", QuestionType.ESSAY).first()

        assertEquals("L3异步评估不阻塞，应返回L2结果", RecallLevel.L2, result.level)
        assertNull("L2结果不应有 score", result.score)
    }

    @Test
    fun c5_16_l1_doesNotTriggerL3() = runTest {
        val result = checker.checkRecall("春秋", "春秋，战国", QuestionType.TERM_EXPLANATION).first()

        assertEquals("L1不应触发L3", RecallLevel.L1, result.level)
    }

    // ── C5.17: LLM 输出 0-100 分及理由 ─────────────────────────

    @Test
    fun c5_17_l3_returnsScoreAndReason() = runTest {
        aiService.response = """{"score": 85, "reason": "答案覆盖了主要知识点"}"""

        val result = checker.checkL3Llm("用户答案", "正确答案")

        assertEquals(RecallLevel.L3, result.level)
        assertEquals(85, result.score)
        assertEquals("答案覆盖了主要知识点", result.reason)
        assertEquals(0.85f, result.coverage, 0.001f)
        // 85 < 90 → GOOD
        assertEquals(RecallRating.GOOD, result.rating)
    }

    @Test
    fun c5_17_l3_scoreBelow60_returnsAgain() = runTest {
        aiService.response = """{"score": 50, "reason": "答案严重不足"}"""

        val result = checker.checkL3Llm("答案", "正确答案")

        assertEquals(RecallRating.AGAIN, result.rating)
        assertEquals(50, result.score)
    }

    @Test
    fun c5_17_l3_score60To75_returnsHard() = runTest {
        aiService.response = """{"score": 65, "reason": "部分正确"}"""

        val result = checker.checkL3Llm("答案", "正确答案")

        assertEquals(RecallRating.HARD, result.rating)
    }

    @Test
    fun c5_17_l3_score75To90_returnsGood() = runTest {
        aiService.response = """{"score": 80, "reason": "较好"}"""

        val result = checker.checkL3Llm("答案", "正确答案")

        assertEquals(RecallRating.GOOD, result.rating)
    }

    @Test
    fun c5_17_l3_scoreAbove90_returnsEasy() = runTest {
        aiService.response = """{"score": 95, "reason": "全面准确"}"""

        val result = checker.checkL3Llm("答案", "正确答案")

        assertEquals(RecallRating.EASY, result.rating)
    }

    @Test
    fun c5_17_l3_jsonParsingTolerant_非标准JSON也能解析() = runTest {
        // LLM 可能在 JSON 前后加额外文本
        aiService.response = """评估结果如下：
            {"score": 70, "reason": "部分正确"}
            以上是评估。"""

        val result = checker.checkL3Llm("答案", "正确答案")

        assertEquals(70, result.score)
        assertEquals("部分正确", result.reason)
    }

    @Test
    fun c5_17_l3_invalidJson_returnsDefaults() = runTest {
        aiService.response = "这不是 JSON 格式"

        val result = checker.checkL3Llm("答案", "正确答案")

        // score 解析失败 → 0，0 < 60 → AGAIN
        assertEquals(0, result.score)
        assertEquals(RecallRating.AGAIN, result.rating)
        // reason 解析失败 → 默认提示
        assertNotNull("reason 应有默认值", result.reason)
    }

    @Test
    fun c5_17_l3_dataClass_supportsScoreAndReason() {
        // 验证 RecallResult 数据类支持 L3 的 score 和 reason 字段
        val l3Result = RecallResult(
            level = RecallLevel.L3,
            coverage = 0.75f,
            rating = RecallRating.GOOD,
            score = 75,
            reason = "答案覆盖了主要知识点，但缺少具体例证。",
        )

        assertEquals(RecallLevel.L3, l3Result.level)
        assertNotNull("L3结果应有 score", l3Result.score)
        assertTrue("L3 score应在0-100范围", l3Result.score!! in 0..100)
        assertNotNull("L3结果应有 reason", l3Result.reason)
        assertTrue("L3 reason应非空", l3Result.reason!!.isNotBlank())
    }

    // ── L1 阈值边界值验证 ──────────────────────────────────────
    // 使用干支2字词（甲子/乙丑/...），避免单字被 length>=2 过滤，且不在 SYNONYM_MAP 中。

    @Test
    fun boundary_030_returnsHard() = runTest {
        // 10个关键词，匹配3个 → 0.30 → HARD
        val correctAnswer = "甲子，乙丑，丙寅，丁卯，戊辰，己巳，庚午，辛未，壬申，癸酉"
        val userAnswer = "甲子乙丑丙寅"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.HARD, result.rating)
        assertEquals(0.30f, result.coverage, 0.001f)
    }

    @Test
    fun boundary_060_returnsGood() = runTest {
        // 5个关键词，匹配3个 → 0.60 → GOOD
        val correctAnswer = "甲子，乙丑，丙寅，丁卯，戊辰"
        val userAnswer = "甲子乙丑丙寅"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.GOOD, result.rating)
        assertEquals(0.60f, result.coverage, 0.001f)
    }

    @Test
    fun boundary_085_returnsEasy() = runTest {
        // 20个关键词，匹配17个 → 0.85 → EASY
        val correctAnswer = "甲子，乙丑，丙寅，丁卯，戊辰，己巳，庚午，辛未，壬申，癸酉，" +
            "甲戌，乙亥，丙子，丁丑，戊寅，己卯，庚辰，辛巳，壬午，癸未"
        // userAnswer 包含前17个词（甲子~庚辰），不含辛巳/壬午/癸未
        val userAnswer = "甲子乙丑丙寅丁卯戊辰己巳庚午辛未壬申癸酉" +
            "甲戌乙亥丙子丁丑戊寅己卯庚辰"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.EASY, result.rating)
        assertEquals(0.85f, result.coverage, 0.001f)
    }

    // ── checkRecall 路由逻辑 ───────────────────────────────────

    @Test
    fun checkRecall_termExplanation_goesThroughL1() = runTest {
        val result = checker.checkRecall("春秋", "春秋，战国", QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
    }

    @Test
    fun checkRecall_essay_goesThroughL2() = runTest {
        val result = checker.checkRecall("答案", "正确答案", QuestionType.ESSAY).first()

        assertEquals(RecallLevel.L2, result.level)
    }

    // ── 枚举验证 ───────────────────────────────────────────────

    @Test
    fun questionType_enumHasExpectedValues() {
        val types = QuestionType.values()
        assertEquals(2, types.size)
        assertTrue(types.contains(QuestionType.TERM_EXPLANATION))
        assertTrue(types.contains(QuestionType.ESSAY))
    }

    @Test
    fun recallLevel_enumHasExpectedValues() {
        val levels = RecallLevel.values()
        assertEquals(3, levels.size)
        assertTrue(levels.contains(RecallLevel.L1))
        assertTrue(levels.contains(RecallLevel.L2))
        assertTrue(levels.contains(RecallLevel.L3))
    }

    @Test
    fun recallRating_enumHasExpectedValues() {
        val ratings = RecallRating.values()
        assertEquals(4, ratings.size)
        assertTrue(ratings.contains(RecallRating.AGAIN))
        assertTrue(ratings.contains(RecallRating.HARD))
        assertTrue(ratings.contains(RecallRating.GOOD))
        assertTrue(ratings.contains(RecallRating.EASY))
    }
}

/**
 * [AiService] 的 Fake 实现，供 [RecallCheckerTest] 使用。
 *
 * 支持动态设置 [response]，用于 L3 LLM 评估测试。
 */
private class FakeAiService(
    var response: String = "默认 AI 回复",
) : AiService {

    override fun chat(query: String): Flow<String> = flowOf(response)

    override fun isAvailable(): Flow<Boolean> = flowOf(true)
}
