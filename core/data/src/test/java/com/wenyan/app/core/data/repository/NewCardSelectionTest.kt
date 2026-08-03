package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 新卡选择逻辑单元测试（v0.9.29 卡片备考系统）。
 *
 * 覆盖 [selectNewPoints] / [matchesFrequency] / [frequencyRank] / [takeNewPointsByCardLimit]：
 * - 排除已学知识点（有 memo_record）
 * - 考频筛选：HIGH / HIGH+MEDIUM / ALL
 * - 科目筛选：多选 / 未关联科目保留
 * - 排序：考频 HIGH → MEDIUM → LOW，同频按 updated_at
 * - 每日限额：按卡片数取整到知识点（60 张 ≈ 10 个）
 */
class NewCardSelectionTest {

    private fun kp(
        id: String,
        frequency: String = "MEDIUM",
        updatedAt: Long = 1000L,
        subject: String? = "中国古代文学",
    ): KnowledgePointWithSubject = KnowledgePointWithSubject(
        point = KnowledgePointEntity(
            id = id,
            chapterId = "ch_1",
            title = "知识点$id",
            summary = null,
            coreConclusion = "结论",
            fullContent = "内容",
            multiPerspectives = null,
            relatedIds = null,
            contrastIds = null,
            extensionIds = null,
            examRecords = null,
            examFrequency = frequency,
            termTemplate = null,
            tags = null,
            createdAt = 0L,
            updatedAt = updatedAt,
            contentSource = "TEXTBOOK_NATIVE",
            ocrStatus = "VERIFIED",
            sourceFile = null,
            sourcePage = null,
            studyText = null,
        ),
        subjectName = subject,
    )

    // ============ 排除已学 ============

    @Test
    fun `已学知识点不进入新卡候选`() {
        val candidates = listOf(kp("a", "HIGH"), kp("b", "HIGH"), kp("c", "HIGH"))
        val learned = setOf("b")

        val result = selectNewPoints(candidates, learned, CardSettings(), 60)

        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    // ============ 考频筛选 ============

    @Test
    fun `HIGH 筛选只保留高频`() {
        val candidates = listOf(
            kp("h", "HIGH"),
            kp("m", "MEDIUM"),
            kp("l", "LOW"),
            kp("u", "UNKNOWN"),
        )
        val settings = CardSettings(frequencyFilter = CardFrequencyFilter.HIGH)

        val result = selectNewPoints(candidates, emptySet(), settings, 60)

        assertEquals(listOf("h"), result.map { it.id })
    }

    @Test
    fun `HIGH_MEDIUM 筛选保留高频和中频`() {
        val candidates = listOf(kp("h", "HIGH"), kp("m", "MEDIUM"), kp("l", "LOW"))
        val settings = CardSettings(frequencyFilter = CardFrequencyFilter.HIGH_MEDIUM)

        val result = selectNewPoints(candidates, emptySet(), settings, 60)

        assertEquals(listOf("h", "m"), result.map { it.id })
    }

    @Test
    fun `ALL 筛选保留全部考频`() {
        val candidates = listOf(kp("h", "HIGH"), kp("m", "MEDIUM"), kp("l", "LOW"))
        val settings = CardSettings(frequencyFilter = CardFrequencyFilter.ALL)

        val result = selectNewPoints(candidates, emptySet(), settings, 60)

        assertEquals(listOf("h", "m", "l"), result.map { it.id })
    }

    @Test
    fun `matchesFrequency 单测各档位`() {
        assertTrue(matchesFrequency("HIGH", CardFrequencyFilter.HIGH))
        assertTrue(!matchesFrequency("MEDIUM", CardFrequencyFilter.HIGH))
        assertTrue(matchesFrequency("MEDIUM", CardFrequencyFilter.HIGH_MEDIUM))
        assertTrue(!matchesFrequency("LOW", CardFrequencyFilter.HIGH_MEDIUM))
        assertTrue(matchesFrequency("LOW", CardFrequencyFilter.ALL))
        assertTrue(matchesFrequency("UNKNOWN", CardFrequencyFilter.ALL))
    }

    // ============ 科目筛选 ============

    @Test
    fun `科目筛选只保留选中科目`() {
        val candidates = listOf(
            kp("a", subject = "中国古代文学"),
            kp("b", subject = "中国现当代文学"),
            kp("c", subject = "外国文学"),
        )
        val settings = CardSettings(subjectFilters = setOf("中国古代文学", "外国文学"))

        val result = selectNewPoints(candidates, emptySet(), settings, 60)

        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    @Test
    fun `未关联科目的知识点保留（避免漏卡）`() {
        val candidates = listOf(
            kp("a", subject = null),
            kp("b", subject = "外国文学"),
        )
        val settings = CardSettings(subjectFilters = setOf("外国文学"))

        val result = selectNewPoints(candidates, emptySet(), settings, 60)

        // subjectName == null 时保留，a 和 b 都应出现
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    // ============ 排序 ============

    @Test
    fun `按考频 HIGH 优先排序，同频按 updatedAt`() {
        val candidates = listOf(
            kp("low_new", "LOW", updatedAt = 5000L),
            kp("high_old", "HIGH", updatedAt = 100L),
            kp("med", "MEDIUM", updatedAt = 3000L),
            kp("high_new", "HIGH", updatedAt = 200L),
        )
        val settings = CardSettings(frequencyFilter = CardFrequencyFilter.ALL)

        val result = selectNewPoints(candidates, emptySet(), settings, 60)

        // HIGH(100,200) → MEDIUM(3000) → LOW(5000)
        assertEquals(listOf("high_old", "high_new", "med", "low_new"), result.map { it.id })
    }

    // ============ 每日限额 ============

    @Test
    fun `每日限额 60 张约取 10 个知识点`() {
        val candidates = (1..30).map { kp("p$it", "HIGH") }

        val result = takeNewPointsByCardLimit(candidates.map { it.point }, 60)

        // 60 / 6 = 10 个知识点
        assertEquals(10, result.size)
        assertEquals("p1", result.first().id)
        assertEquals("p10", result.last().id)
    }

    @Test
    fun `每日限额取整到知识点（不会拆散半个知识点）`() {
        // 65 张限额 → 10 个知识点（60 张）+ 第 11 个（66 张）超过？不，65 >= 60 后停止
        val candidates = (1..20).map { kp("p$it", "HIGH") }

        val result = takeNewPointsByCardLimit(candidates.map { it.point }, 61)

        // 每点 6 张：10 个点 = 60 张 < 61 → 取第 11 个 = 66 张 >= 61 停止 → 11 个
        assertEquals(11, result.size)
    }

    @Test
    fun `候选不足限额时全部返回`() {
        val candidates = (1..3).map { kp("p$it", "HIGH") }

        val result = takeNewPointsByCardLimit(candidates.map { it.point }, 60)

        assertEquals(3, result.size)
    }

    @Test
    fun `限额为 0 时不取新卡`() {
        val candidates = (1..5).map { kp("p$it", "HIGH") }

        val result = takeNewPointsByCardLimit(candidates.map { it.point }, 0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `frequencyRank 排序权重`() {
        assertEquals(0, frequencyRank("HIGH"))
        assertEquals(1, frequencyRank("MEDIUM"))
        assertEquals(2, frequencyRank("LOW"))
        assertEquals(2, frequencyRank("UNKNOWN"))
    }

    // ============ 复习/新卡比例保护（v0.9.29 打磨） ============

    @Test
    fun `复习知识点少时新卡按限额`() {
        assertEquals(60, computeEffectiveNewLimit(duePointCount = 0, dailyNewLimit = 60))
        assertEquals(60, computeEffectiveNewLimit(duePointCount = 10, dailyNewLimit = 60))
    }

    @Test
    fun `复习知识点中等时新卡减半`() {
        assertEquals(30, computeEffectiveNewLimit(duePointCount = 15, dailyNewLimit = 60))
        assertEquals(15, computeEffectiveNewLimit(duePointCount = 20, dailyNewLimit = 30))
    }

    @Test
    fun `复习知识点过多时暂停新卡`() {
        assertEquals(0, computeEffectiveNewLimit(duePointCount = 21, dailyNewLimit = 60))
        assertEquals(0, computeEffectiveNewLimit(duePointCount = 100, dailyNewLimit = 60))
    }

    @Test
    fun `比例保护后限额为 0 不取新卡`() {
        val candidates = (1..5).map { kp("p$it", "HIGH") }
        // 模拟复习过多：effectiveLimit = 0
        val result = takeNewPointsByCardLimit(candidates.map { it.point }, 0)
        assertTrue(result.isEmpty())
    }
}
