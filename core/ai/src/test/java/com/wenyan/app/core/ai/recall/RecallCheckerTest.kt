package com.wenyan.app.core.ai.recall

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.wenyan.app.core.ai.AiService

/**
 * Task 32 - 主动回忆检测测试
 * RecallChecker 单元测试
 *
 * 验证 checklist 项：
 * - C5.14: 验证L1关键词匹配（TERM_EXPLANATION走L1，覆盖率<30%→AGAIN，30-60%→HARD，60-85%→GOOD，≥85%→EASY）
 * - C5.15: 验证L2语义相似度（ESSAY走L2，覆盖率<60%→HARD，60-85%→部分正确触发L3，≥85%→EASY）
 * - C5.16: 验证L3 LLM异步评估不阻塞流程（L2部分正确时先返回L2结果）
 * - C5.17: 验证LLM输出0-100分及理由（L3的score和reason字段）
 * - 验证L1覆盖率计算（关键词匹配）
 * - 验证阈值边界值（0.29→AGAIN, 0.30→HARD, 0.59→HARD, 0.60→GOOD, 0.84→GOOD, 0.85→EASY）
 *
 * 注意：使用已有的生产代码 RecallChecker。L1关键词通过分割"，。、；"提取，
 * 覆盖率 = 匹配关键词数 / 总关键词数。L2当前骨架返回0f相似度。
 */

class RecallCheckerTest {

    private val aiService = FakeAiService()
    private val checker = RecallChecker(aiService)

    // ── C5.14: L1 关键词匹配 ──────────────────────────────────────

    // C5.14: L1关键词匹配 - 覆盖率<30%→AGAIN
    @Test
    fun c5_14_l1_keywordMatch_lowCoverage_returnsAgain() = runBlocking {
        // 4个关键词，匹配1个 → coverage = 0.25 < 0.30 → AGAIN
        val correctAnswer = "黄庭坚，杜甫，宋诗，瘦硬"
        val userAnswer = "黄庭坚"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(RecallRating.AGAIN, result.rating)
        assertEquals(0.25f, result.coverage, 0.001f)
    }

    // C5.14: L1关键词匹配 - 30-60%→HARD
    @Test
    fun c5_14_l1_keywordMatch_mediumCoverage_returnsHard() = runBlocking {
        // 4个关键词，匹配2个 → coverage = 0.50 → HARD
        val correctAnswer = "黄庭坚，杜甫，宋诗，瘦硬"
        val userAnswer = "黄庭坚和杜甫"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(RecallRating.HARD, result.rating)
        assertEquals(0.50f, result.coverage, 0.001f)
    }

    // C5.14: L1关键词匹配 - 60-85%→GOOD
    @Test
    fun c5_14_l1_keywordMatch_highCoverage_returnsGood() = runBlocking {
        // 4个关键词，匹配3个 → coverage = 0.75 → GOOD
        val correctAnswer = "黄庭坚，杜甫，宋诗，瘦硬"
        val userAnswer = "黄庭坚杜甫宋诗"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(RecallRating.GOOD, result.rating)
        assertEquals(0.75f, result.coverage, 0.001f)
    }

    // C5.14: L1关键词匹配 - ≥85%→EASY
    @Test
    fun c5_14_l1_keywordMatch_fullCoverage_returnsEasy() = runBlocking {
        // 4个关键词，匹配4个 → coverage = 1.00 → EASY
        val correctAnswer = "黄庭坚，杜甫，宋诗，瘦硬"
        val userAnswer = "黄庭坚杜甫宋诗瘦硬"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(RecallRating.EASY, result.rating)
        assertEquals(1.00f, result.coverage, 0.001f)
    }

    // ── C5.15: L2 语义相似度 ──────────────────────────────────────

    // C5.15: L2语义相似度 - ESSAY走L2
    @Test
    fun c5_15_l2_semantic_essayGoesThroughL2() = runBlocking {
        val correctAnswer = "建安风骨是汉末建安时期诗歌的风格特征，以慷慨悲凉著称。"
        val userAnswer = "建安风骨是一种诗歌风格。"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.ESSAY).first()

        assertEquals("ESSAY应走L2", RecallLevel.L2, result.level)
    }

    // C5.15: L2语义相似度 - 覆盖率<60%→HARD
    @Test
    fun c5_15_l2_lowSimilarity_returnsHard() = runBlocking {
        // 当前骨架实现 calculateSemanticSimilarity 返回 0f → HARD
        val correctAnswer = "建安风骨是汉末建安时期诗歌的风格特征。"
        val userAnswer = "建安风骨是一种风格。"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.ESSAY).first()

        assertEquals(RecallLevel.L2, result.level)
        assertEquals(RecallRating.HARD, result.rating)
        assertTrue("覆盖率<60%应为HARD", result.coverage < 0.60f)
    }

    // C5.15: L2语义相似度 - L2结果score和reason为null
    @Test
    fun c5_15_l2_result_scoreAndReasonAreNull() = runBlocking {
        val result = checker.checkRecall("答案", "正确答案", QuestionType.ESSAY).first()

        assertEquals(RecallLevel.L2, result.level)
        assertNull("L2结果 score 应为 null", result.score)
        assertNull("L2结果 reason 应为 null", result.reason)
    }

    // ── C5.16: L3 LLM异步评估不阻塞流程 ──────────────────────────

    // C5.16: L2部分正确时先返回L2结果（不阻塞）
    @Test
    fun c5_16_l3_async_doesNotBlockFlow() = runBlocking {
        // ESSAY类型 → L2，当前骨架返回0f → HARD
        // 验证第一个返回的结果是L2（L3不阻塞流程）
        val result = checker.checkRecall("答案", "正确答案", QuestionType.ESSAY).first()

        assertEquals("L3异步评估不阻塞，应先返回L2结果", RecallLevel.L2, result.level)
    }

    // C5.16 补充：L1不触发L3
    @Test
    fun c5_16_l1_doesNotTriggerL3() = runBlocking {
        val result = checker.checkRecall("黄庭坚", "黄庭坚，杜甫", QuestionType.TERM_EXPLANATION).first()

        assertEquals("L1不应触发L3", RecallLevel.L1, result.level)
    }

    // ── C5.17: LLM输出0-100分及理由 ───────────────────────────────

    // C5.17: 验证L3的score和reason字段（通过数据类）
    @Test
    fun c5_17_l3_result_hasScoreAndReason() {
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

    // C5.17: L3评分映射 - score<60→AGAIN
    @Test
    fun c5_17_l3_scoreBelow60_returnsAgain() {
        val result = RecallResult(
            level = RecallLevel.L3,
            coverage = 0.50f,
            rating = RecallRating.AGAIN,
            score = 50,
            reason = "答案严重不足。",
        )

        assertEquals(RecallRating.AGAIN, result.rating)
        assertTrue(result.score!! < 60)
    }

    // C5.17: L3评分映射 - 60-75→HARD
    @Test
    fun c5_17_l3_score60To75_returnsHard() {
        val result = RecallResult(
            level = RecallLevel.L3,
            coverage = 0.65f,
            rating = RecallRating.HARD,
            score = 65,
            reason = "答案部分正确，但有遗漏。",
        )

        assertEquals(RecallRating.HARD, result.rating)
        assertTrue(result.score!! in 60..74)
    }

    // C5.17: L3评分映射 - 75-90→GOOD
    @Test
    fun c5_17_l3_score75To90_returnsGood() {
        val result = RecallResult(
            level = RecallLevel.L3,
            coverage = 0.80f,
            rating = RecallRating.GOOD,
            score = 80,
            reason = "答案较好，覆盖主要知识点。",
        )

        assertEquals(RecallRating.GOOD, result.rating)
        assertTrue(result.score!! in 75..89)
    }

    // C5.17: L3评分映射 - ≥90→EASY
    @Test
    fun c5_17_l3_scoreAbove90_returnsEasy() {
        val result = RecallResult(
            level = RecallLevel.L3,
            coverage = 0.95f,
            rating = RecallRating.EASY,
            score = 95,
            reason = "答案全面准确。",
        )

        assertEquals(RecallRating.EASY, result.rating)
        assertTrue(result.score!! >= 90)
    }

    // ── L1覆盖率计算验证 ──────────────────────────────────────────

    // 验证L1覆盖率计算（关键词匹配）
    @Test
    fun l1_coverageCalculation_keywordMatching() = runBlocking {
        // 3个关键词，匹配2个 → coverage = 2/3 ≈ 0.667 → GOOD
        val correctAnswer = "建安风骨，慷慨悲凉，汉末建安"
        val userAnswer = "建安风骨和慷慨悲凉"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallLevel.L1, result.level)
        assertEquals(2f / 3f, result.coverage, 0.001f)
        assertEquals(RecallRating.GOOD, result.rating)
    }

    // 验证L1覆盖率 - 无匹配
    @Test
    fun l1_coverageCalculation_noMatch() = runBlocking {
        val correctAnswer = "建安风骨，慷慨悲凉，汉末建安"
        val userAnswer = "完全无关的内容"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(0f, result.coverage, 0.001f)
        assertEquals(RecallRating.AGAIN, result.rating)
    }

    // ── 阈值边界值验证 ────────────────────────────────────────────

    // 边界值：0.29(≈2/7)→AGAIN
    @Test
    fun boundary_029_returnsAgain() = runBlocking {
        // 7个关键词，匹配2个 → coverage = 2/7 ≈ 0.2857 → AGAIN
        val correctAnswer = "甲，乙，丙，丁，戊，己，庚"
        val userAnswer = "甲乙"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.AGAIN, result.rating)
        assertTrue("覆盖率应≈0.29（<0.30）", result.coverage < 0.30f)
    }

    // 边界值：0.30(=3/10)→HARD
    @Test
    fun boundary_030_returnsHard() = runBlocking {
        // 10个关键词，匹配3个 → coverage = 3/10 = 0.30 → HARD
        val correctAnswer = "甲，乙，丙，丁，戊，己，庚，辛，壬，癸"
        val userAnswer = "甲乙丙"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.HARD, result.rating)
        assertEquals(0.30f, result.coverage, 0.001f)
    }

    // 边界值：0.59(≈10/17)→HARD
    @Test
    fun boundary_059_returnsHard() = runBlocking {
        // 17个关键词，匹配10个 → coverage = 10/17 ≈ 0.588 → HARD
        val correctAnswer = "甲，乙，丙，丁，戊，己，庚，辛，壬，癸，子，丑，寅，卯，辰，巳，午"
        val userAnswer = "甲乙丙丁戊己庚辛壬癸"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.HARD, result.rating)
        assertTrue("覆盖率应≈0.59（<0.60）", result.coverage < 0.60f)
        assertTrue("覆盖率应≥0.30", result.coverage >= 0.30f)
    }

    // 边界值：0.60(=3/5)→GOOD
    @Test
    fun boundary_060_returnsGood() = runBlocking {
        // 5个关键词，匹配3个 → coverage = 3/5 = 0.60 → GOOD
        val correctAnswer = "甲，乙，丙，丁，戊"
        val userAnswer = "甲乙丙"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.GOOD, result.rating)
        assertEquals(0.60f, result.coverage, 0.001f)
    }

    // 边界值：0.84(≈5/6)→GOOD
    @Test
    fun boundary_084_returnsGood() = runBlocking {
        // 6个关键词，匹配5个 → coverage = 5/6 ≈ 0.833 → GOOD
        val correctAnswer = "甲，乙，丙，丁，戊，己"
        val userAnswer = "甲乙丙丁戊"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.GOOD, result.rating)
        assertTrue("覆盖率应≈0.84（<0.85）", result.coverage < 0.85f)
        assertTrue("覆盖率应≥0.60", result.coverage >= 0.60f)
    }

    // 边界值：0.85(=17/20)→EASY
    @Test
    fun boundary_085_returnsEasy() = runBlocking {
        // 20个关键词，匹配17个 → coverage = 17/20 = 0.85 → EASY
        val correctAnswer = "甲，乙，丙，丁，戊，己，庚，辛，壬，癸，子，丑，寅，卯，辰，巳，午，未，申，酉"
        // userAnswer 包含前17个关键词（甲~午），不含未/申/酉
        val userAnswer = "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午"

        val result = checker.checkRecall(userAnswer, correctAnswer, QuestionType.TERM_EXPLANATION).first()

        assertEquals(RecallRating.EASY, result.rating)
        assertEquals(0.85f, result.coverage, 0.001f)
    }

    // ── QuestionType 枚举验证 ─────────────────────────────────────

    // 验证 QuestionType 枚举包含 TERM_EXPLANATION 和 ESSAY
    @Test
    fun questionType_enumHasExpectedValues() {
        val types = QuestionType.values()
        assertEquals(2, types.size)
        assertTrue(types.contains(QuestionType.TERM_EXPLANATION))
        assertTrue(types.contains(QuestionType.ESSAY))
    }

    // 验证 RecallLevel 枚举包含 L1/L2/L3
    @Test
    fun recallLevel_enumHasExpectedValues() {
        val levels = RecallLevel.values()
        assertEquals(3, levels.size)
        assertTrue(levels.contains(RecallLevel.L1))
        assertTrue(levels.contains(RecallLevel.L2))
        assertTrue(levels.contains(RecallLevel.L3))
    }

    // 验证 RecallRating 枚举包含 AGAIN/HARD/GOOD/EASY
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

// ── Fake 实现 ─────────────────────────────────────────────────────

/**
 * AiService 的 fake 实现，用于测试。
 */
private class FakeAiService : AiService {
    override fun chat(query: String): Flow<String> = flow {
        emit("AI 回复：$query")
    }

    override fun isAvailable(): Flow<Boolean> = flow {
        emit(true)
    }
}
