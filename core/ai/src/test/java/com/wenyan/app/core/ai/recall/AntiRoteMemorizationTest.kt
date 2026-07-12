package com.wenyan.app.core.ai.recall

import com.wenyan.app.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [AntiRoteMemorization] 单元测试（阶段4重写版）。
 *
 * 验证 checklist 项：
 * - C5.18: 防"背关键词但不懂含义"机制（checkRoteMemorization 返回结果）
 * - C5.19: 检测某卡片始终"正确"但关联卡片频繁出错时降低置信度
 *
 * 阶段4实现变更：
 * - 注入 [FakeReviewLogDao] 查询复习历史
 * - detectRotePattern 实际查询 review_logs 表
 * - 连续正确次数：从最新记录开始数 GOOD/EASY，遇到 AGAIN/HARD 停止
 * - 关联错误率：AGAIN 评级占关联卡片总记录数的比例
 *
 * 判定条件：连续正确 ≥ 5 且关联错误率 ≥ 0.4 → isSuspected = true
 */
class AntiRoteMemorizationTest {

    private lateinit var dao: FakeReviewLogDao
    private lateinit var antiRote: AntiRoteMemorization

    @Before
    fun setup() {
        dao = FakeReviewLogDao()
        antiRote = AntiRoteMemorization(dao)
    }

    // ── C5.18: checkRoteMemorization 返回结果 ──────────────────

    @Test
    fun c5_18_checkRoteMemorization_returnsResult() = runTest {
        val result = antiRote.checkRoteMemorization("card_001", listOf("card_002")).first()

        assertNotNull("checkRoteMemorization 应返回结果", result)
        assertNotNull("结果应包含 isSuspected 字段", result.isSuspected)
        assertNotNull("结果应包含 suggestion 字段", result.suggestion)
    }

    @Test
    fun c5_18_checkRoteMemorization_acceptsVariousCardIds() = runTest {
        val cardIds = listOf("card_jian_an", "card_huang_ting_jian", "card_du_fu")

        for (cardId in cardIds) {
            val result = antiRote.checkRoteMemorization(cardId, emptyList()).first()
            assertNotNull("对卡片ID '$cardId' 应返回结果", result)
        }
    }

    @Test
    fun c5_18_checkRoteMemorization_acceptsEmptyRelatedList() = runTest {
        val result = antiRote.checkRoteMemorization("card_001", emptyList()).first()

        assertNotNull("即使关联列表为空也应返回结果", result)
        assertFalse("空关联列表 → 错误率0 → 不疑似", result.isSuspected)
    }

    // ── C5.19: 死记硬背检测逻辑 ────────────────────────────────

    @Test
    fun c5_19_suspectedRote_连续正确5次且关联错误率高() = runTest {
        // card_001：5次 GOOD（连续正确 ≥ 5）
        // card_002：3次 AGAIN + 2次 GOOD → 错误率 3/5 = 0.6 ≥ 0.4
        dao = FakeReviewLogDao(
            logs(
                "card_001" to listOf("GOOD", "GOOD", "GOOD", "GOOD", "GOOD"),
                "card_002" to listOf("AGAIN", "AGAIN", "AGAIN", "GOOD", "GOOD"),
            ),
        )
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_001", listOf("card_002")).first()

        assertTrue("连续正确≥5且关联错误率≥0.4 应判定为疑似", result.isSuspected)
    }

    @Test
    fun c5_19_notSuspected_连续正确不足5次() = runTest {
        // card_003：3次 GOOD（连续正确 < 5）
        // card_004：3次 AGAIN → 错误率 1.0 ≥ 0.4
        dao = FakeReviewLogDao(
            logs(
                "card_003" to listOf("GOOD", "GOOD", "GOOD"),
                "card_004" to listOf("AGAIN", "AGAIN", "AGAIN"),
            ),
        )
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_003", listOf("card_004")).first()

        assertFalse("连续正确<5 不应判定为疑似", result.isSuspected)
    }

    @Test
    fun c5_19_notSuspected_关联错误率不足() = runTest {
        // card_005：6次 GOOD（连续正确 ≥ 5）
        // card_006：1次 AGAIN + 4次 GOOD → 错误率 1/5 = 0.2 < 0.4
        dao = FakeReviewLogDao(
            logs(
                "card_005" to listOf("GOOD", "GOOD", "GOOD", "GOOD", "GOOD", "GOOD"),
                "card_006" to listOf("AGAIN", "GOOD", "GOOD", "GOOD", "GOOD"),
            ),
        )
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_005", listOf("card_006")).first()

        assertFalse("关联错误率<0.4 不应判定为疑似", result.isSuspected)
    }

    @Test
    fun c5_19_notSuspected_连续正确被HARD中断() = runTest {
        // card_007 按时间正序：HARD(t1), GOOD(t2), GOOD(t3)
        // DESC排序后：GOOD(t3), GOOD(t2), HARD(t1)
        // 从最新开始数：GOOD+1, GOOD+2, HARD→停止 = 2 < 5
        // card_008：5次 AGAIN → 错误率 1.0 ≥ 0.4
        dao = FakeReviewLogDao(
            logs(
                "card_007" to listOf("HARD", "GOOD", "GOOD"),
                "card_008" to listOf("AGAIN", "AGAIN", "AGAIN", "AGAIN", "AGAIN"),
            ),
        )
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_007", listOf("card_008")).first()

        assertFalse("连续正确被HARD中断（=2<5）不应判定为疑似", result.isSuspected)
    }

    @Test
    fun c5_19_notSuspected_无复习记录() = runTest {
        dao = FakeReviewLogDao(emptyList())
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_009", listOf("card_010")).first()

        assertFalse("无复习记录不应判定为疑似", result.isSuspected)
    }

    @Test
    fun c5_19_suspected_连续GOOD_EASY混合计数() = runTest {
        // card_010：3次 GOOD + 2次 EASY → 连续正确 = 5 ≥ 5
        // card_011：3次 AGAIN + 2次 GOOD → 错误率 3/5 = 0.6 ≥ 0.4
        dao = FakeReviewLogDao(
            logs(
                "card_010" to listOf("GOOD", "GOOD", "GOOD", "EASY", "EASY"),
                "card_011" to listOf("AGAIN", "AGAIN", "AGAIN", "GOOD", "GOOD"),
            ),
        )
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_010", listOf("card_011")).first()

        assertTrue("GOOD/EASY混合连续5次应判定为疑似", result.isSuspected)
    }

    @Test
    fun c5_19_suspected_连续正确刚好5次() = runTest {
        // card_012：5次 EASY → 连续正确 = 5 ≥ 5（边界值）
        // card_013：2次 AGAIN + 3次 GOOD → 错误率 2/5 = 0.4 ≥ 0.4（边界值）
        dao = FakeReviewLogDao(
            logs(
                "card_012" to listOf("EASY", "EASY", "EASY", "EASY", "EASY"),
                "card_013" to listOf("AGAIN", "AGAIN", "GOOD", "GOOD", "GOOD"),
            ),
        )
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_012", listOf("card_013")).first()

        assertTrue("连续正确=5且错误率=0.4（边界值）应判定为疑似", result.isSuspected)
    }

    @Test
    fun c5_19_notSuspected_连续正确4次不够() = runTest {
        // card_014：4次 GOOD → 连续正确 = 4 < 5
        // card_015：5次 AGAIN → 错误率 1.0 ≥ 0.4
        dao = FakeReviewLogDao(
            logs(
                "card_014" to listOf("GOOD", "GOOD", "GOOD", "GOOD"),
                "card_015" to listOf("AGAIN", "AGAIN", "AGAIN", "AGAIN", "AGAIN"),
            ),
        )
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_014", listOf("card_015")).first()

        assertFalse("连续正确=4<5 不应判定为疑似", result.isSuspected)
    }

    // ── suggestion 内容验证 ────────────────────────────────────

    @Test
    fun suggestion_whenSuspected_containsVariantAndReverseAndRelated() = runTest {
        dao = FakeReviewLogDao(
            logs(
                "card_001" to listOf("GOOD", "GOOD", "GOOD", "GOOD", "GOOD"),
                "card_002" to listOf("AGAIN", "AGAIN", "AGAIN", "GOOD", "GOOD"),
            ),
        )
        antiRote = AntiRoteMemorization(dao)

        val result = antiRote.checkRoteMemorization("card_001", listOf("card_002")).first()

        assertTrue("疑似时 isSuspected 应为 true", result.isSuspected)
        assertTrue("疑似时 suggestion 应包含'变体出题'", result.suggestion.contains("变体出题"))
        assertTrue("疑似时 suggestion 应包含'反向提问'", result.suggestion.contains("反向提问"))
        assertTrue("疑似时 suggestion 应包含'关联检测'", result.suggestion.contains("关联检测"))
    }

    @Test
    fun suggestion_whenNotSuspected_isNormal() = runTest {
        val result = antiRote.checkRoteMemorization("card_001", listOf("card_002")).first()

        assertFalse("正常时 isSuspected 应为 false", result.isSuspected)
        assertEquals("复习表现正常", result.suggestion)
    }

    // ── RoteCheckResult 数据类验证 ──────────────────────────────

    @Test
    fun roteCheckResult_dataClass() {
        val result = RoteCheckResult(
            isSuspected = false,
            suggestion = "复习表现正常",
        )

        assertFalse(result.isSuspected)
        assertEquals("复习表现正常", result.suggestion)
    }

    @Test
    fun roteCheckResult_suspectedDataClass() {
        val result = RoteCheckResult(
            isSuspected = true,
            suggestion = "该卡片可能存在死记硬背，建议安排变体出题（同一知识点不同问法）/ 反向提问 / 关联检测",
        )

        assertTrue(result.isSuspected)
        assertTrue(result.suggestion.contains("变体出题"))
    }

    // ── 常量值验证（反射） ──────────────────────────────────────

    @Test
    fun constants_streakThreshold_isCorrect() {
        val companion = AntiRoteMemorization::class.java
            .getDeclaredField("Companion")
            .apply { isAccessible = true }
            .get(null)

        val streakField = companion::class.java
            .getDeclaredField("STREAK_THRESHOLD")
            .apply { isAccessible = true }
        val streakValue = streakField.get(companion) as Int

        assertEquals("STREAK_THRESHOLD 应为 5", 5, streakValue)
    }

    @Test
    fun constants_relatedErrorThreshold_isCorrect() {
        val companion = AntiRoteMemorization::class.java
            .getDeclaredField("Companion")
            .apply { isAccessible = true }
            .get(null)

        val errorRateField = companion::class.java
            .getDeclaredField("RELATED_ERROR_THRESHOLD")
            .apply { isAccessible = true }
        val errorRateValue = errorRateField.get(companion) as Float

        assertEquals("RELATED_ERROR_THRESHOLD 应为 0.4f", 0.4f, errorRateValue, 0.001f)
    }

    // ── 辅助方法 ───────────────────────────────────────────────

    /**
     * 构造复习日志列表的辅助方法。
     *
     * @param pairs (pointId, rating列表) 对，rating按时间正序排列（最早在前）
     * @return ReviewLogEntity 列表（createdAt 递增）
     */
    private fun logs(
        vararg pairs: Pair<String, List<String>>,
    ): List<ReviewLogEntity> {
        val result = mutableListOf<ReviewLogEntity>()
        var counter = 0
        for ((pointId, ratings) in pairs) {
            for ((index, rating) in ratings.withIndex()) {
                result.add(
                    reviewLog(
                        id = "log_${counter++}",
                        pointId = pointId,
                        rating = rating,
                        createdAt = (index + 1).toLong(),  // 递增时间戳，保证DESC排序正确
                    ),
                )
            }
        }
        return result
    }

    private fun reviewLog(
        id: String,
        pointId: String,
        rating: String,
        createdAt: Long,
    ) = ReviewLogEntity(
        id = id,
        pointId = pointId,
        rating = rating,
        elapsedDays = null,
        scheduledDays = null,
        state = null,
        stability = null,
        difficulty = null,
        reps = null,
        createdAt = createdAt,
    )
}
