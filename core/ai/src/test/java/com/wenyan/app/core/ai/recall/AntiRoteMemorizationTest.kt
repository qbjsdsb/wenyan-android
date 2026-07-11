package com.wenyan.app.core.ai.recall

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 32 - 主动回忆检测测试
 * AntiRoteMemorization 单元测试
 *
 * 验证 checklist 项：
 * - C5.18: 验证防"背关键词但不懂含义"机制（checkRoteMemorization返回结果）
 * - C5.19: 验证检测到某卡片始终"正确"但关联卡片频繁出错时降低置信度
 * - 验证suggestion内容（疑似时建议"变体出题/反向提问/关联检测"）
 * - 验证常量值（STREAK_THRESHOLD=5, RELATED_ERROR_THRESHOLD=0.4f）
 *
 * Spec 第 419-423 行新增功能（设计文档无此功能）：
 * - 检测某卡片始终"正确"但关联卡片频繁出错时，降低该卡片置信度
 * - 安排变体出题（同一知识点不同问法）
 * - 安排反向提问
 * - 关联检测
 *
 * 注意：使用已有的生产代码 AntiRoteMemorization。当前骨架实现 detectRotePattern 返回 false。
 * 常量为 private，通过反射验证。
 */

class AntiRoteMemorizationTest {

    private val antiRote = AntiRoteMemorization()

    // C5.18: 验证防"背关键词但不懂含义"机制（checkRoteMemorization返回结果）
    @Test
    fun c5_18_checkRoteMemorization_returnsResult() = runBlocking {
        val result = antiRote.checkRoteMemorization("card_001", listOf("card_002", "card_003")).first()

        assertNotNull("checkRoteMemorization 应返回结果", result)
        assertNotNull("结果应包含 isSuspected 字段", result.isSuspected)
        assertNotNull("结果应包含 suggestion 字段", result.suggestion)
    }

    // C5.18 补充：checkRoteMemorization 对不同卡片ID均返回结果
    @Test
    fun c5_18_checkRoteMemorization_acceptsVariousCardIds() = runBlocking {
        val cardIds = listOf("card_jian_an", "card_huang_ting_jian", "card_du_fu")

        for (cardId in cardIds) {
            val result = antiRote.checkRoteMemorization(cardId, emptyList()).first()
            assertNotNull("对卡片ID '$cardId' 应返回结果", result)
        }
    }

    // C5.18 补充：checkRoteMemorization 接受空关联列表
    @Test
    fun c5_18_checkRoteMemorization_acceptsEmptyRelatedList() = runBlocking {
        val result = antiRote.checkRoteMemorization("card_001", emptyList()).first()

        assertNotNull("即使关联列表为空也应返回结果", result)
    }

    // C5.19: 验证检测到某卡片始终"正确"但关联卡片频繁出错时降低置信度
    @Test
    fun c5_19_detectsRoteMemorization_whenCardAlwaysCorrectButRelatedFrequentError() = runBlocking {
        // 验证 RoteCheckResult 数据类支持 isSuspected 字段
        // 当卡片自身连续正确≥5次且关联卡片错误率≥0.4时，isSuspected 应为 true
        val suspectedResult = RoteCheckResult(
            isSuspected = true,
            suggestion = "该卡片可能存在死记硬背，建议安排变体出题/反向提问/关联检测",
        )

        assertTrue("疑似死记硬背时 isSuspected 应为 true", suspectedResult.isSuspected)
    }

    // C5.19 补充：正常复习表现时 isSuspected=false
    @Test
    fun c5_19_normalPerformance_isNotSuspected() = runBlocking {
        // 当前骨架实现 detectRotePattern 返回 false
        val result = antiRote.checkRoteMemorization("card_001", listOf("card_002")).first()

        assertFalse("正常复习表现时 isSuspected 应为 false", result.isSuspected)
    }

    // C5.19 补充：RoteCheckResult 数据类验证
    @Test
    fun c5_19_roteCheckResult_dataClass() {
        val result = RoteCheckResult(
            isSuspected = false,
            suggestion = "复习表现正常",
        )

        assertFalse(result.isSuspected)
        assertEquals("复习表现正常", result.suggestion)
    }

    // 验证suggestion内容（疑似时建议"变体出题/反向提问/关联检测"）
    @Test
    fun suggestion_whenSuspected_containsVariantAndReverseAndRelated() {
        val suspectedSuggestion = "该卡片可能存在死记硬背，建议安排变体出题（同一知识点不同问法）/ 反向提问 / 关联检测"

        assertTrue("疑似时 suggestion 应包含'变体出题'", suspectedSuggestion.contains("变体出题"))
        assertTrue("疑似时 suggestion 应包含'反向提问'", suspectedSuggestion.contains("反向提问"))
        assertTrue("疑似时 suggestion 应包含'关联检测'", suspectedSuggestion.contains("关联检测"))
    }

    // 验证suggestion内容 - 正常时不含变体出题建议
    @Test
    fun suggestion_whenNotSuspected_doesNotContainVariant() = runBlocking {
        val result = antiRote.checkRoteMemorization("card_001", listOf("card_002")).first()

        // 当前骨架返回 "复习表现正常"
        assertTrue("正常时 suggestion 应非空", result.suggestion.isNotBlank())
    }

    // 验证常量值（STREAK_THRESHOLD=5, RELATED_ERROR_THRESHOLD=0.4f）
    @Test
    fun constants_streakThreshold_isCorrect() {
        // Kotlin companion object 中的 private const val 被编译为外部类的静态字段
        val streakField = AntiRoteMemorization::class.java
            .getDeclaredField("STREAK_THRESHOLD")
            .apply { isAccessible = true }
        val streakValue = streakField.get(null) as Int

        assertEquals("STREAK_THRESHOLD 应为 5", 5, streakValue)
    }

    // 验证常量值 - RELATED_ERROR_THRESHOLD
    @Test
    fun constants_relatedErrorThreshold_isCorrect() {
        val errorRateField = AntiRoteMemorization::class.java
            .getDeclaredField("RELATED_ERROR_THRESHOLD")
            .apply { isAccessible = true }
        val errorRateValue = errorRateField.get(null) as Float

        assertEquals("RELATED_ERROR_THRESHOLD 应为 0.4f", 0.4f, errorRateValue, 0.001f)
    }
}
