package com.wenyan.app.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [daysUntilExam] 单元测试（v0.9.29 卡片备考系统）。
 *
 * 覆盖：正常未来日期 / 当天 / 已过日期（clamp 0）/ 跨月计算。
 */
class DaysUntilExamTest {

    private val dayMs = 86_400_000L

    @Test
    fun `未来 100 天返回 100`() {
        assertEquals(100, daysUntilExam(nowMillis = 1_000_000L, examDateMillis = 1_000_000L + 100 * dayMs))
    }

    @Test
    fun `当天返回 0`() {
        assertEquals(0, daysUntilExam(nowMillis = 5_000_000L, examDateMillis = 5_000_000L))
    }

    @Test
    fun `已过日期返回 0 而非负数`() {
        assertEquals(0, daysUntilExam(nowMillis = 10_000_000L, examDateMillis = 5_000_000L))
    }

    @Test
    fun `不足一天按 0 处理`() {
        // 还差 12 小时（0.5 天）→ 0 天
        assertEquals(0, daysUntilExam(nowMillis = 0L, examDateMillis = dayMs / 2))
    }

    @Test
    fun `跨月计算正确`() {
        // 2026-12-24 考试，2026-08-04 现在（示例值）
        val nowMillis = 1_752_710_400_000L  // 2026-08-04
        val exam = nowMillis + 142 * dayMs
        assertEquals(142, daysUntilExam(nowMillis = nowMillis, examDateMillis = exam))
    }
}
