package com.wenyan.app.core.fsrs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/**
 * Task 30 - ExamCountdownManager 单元测试。
 *
 * 验证 checklist C3.6-C3.9 项：考研倒计时驱动动态保持率（基础 0.85 / 强化 0.90 / 冲刺 0.95）、
 * 阶段切换平滑过渡（7 天内从 0 到 1 线性递增）、考研日期计算（12 月倒数第二个周六）、
 * 阶段边界、过渡后保持率线性插值、resolveRetention 取 maxOf（卡片级优先不降级）。
 *
 * 运行环境：纯 JVM 单元测试（core:fsrs/src/test）。
 */
class ExamCountdownManagerTest {

    // ===================== C3.6: 基础阶段全局保持率 0.85 =====================

    /** C3.6: 基础阶段（>180天）全局保持率 0.85 */
    @Test
    fun basicPhase_retentionIs085() {
        assertEquals(StudyPhase.BASIC, ExamCountdownManager.getStudyPhase(200))
        assertEquals(0.85f, ExamCountdownManager.getGlobalRetention(200), 0.0001f)
        assertEquals(0.85f, ExamCountdownManager.RETENTION_BASIC, 0.0001f)
    }

    // ===================== C3.7: 强化阶段全局保持率 0.90 =====================

    /** C3.7: 强化阶段（90-180天）全局保持率 0.90 */
    @Test
    fun intensivePhase_retentionIs090() {
        assertEquals(StudyPhase.INTENSIVE, ExamCountdownManager.getStudyPhase(150))
        assertEquals(0.90f, ExamCountdownManager.getGlobalRetention(150), 0.0001f)
        assertEquals(0.90f, ExamCountdownManager.RETENTION_INTENSIVE, 0.0001f)
    }

    // ===================== C3.8: 冲刺阶段全局保持率 0.95 =====================

    /** C3.8: 冲刺阶段（<90天）全局保持率 0.95 */
    @Test
    fun sprintPhase_retentionIs095() {
        assertEquals(StudyPhase.SPRINT, ExamCountdownManager.getStudyPhase(50))
        assertEquals(0.95f, ExamCountdownManager.getGlobalRetention(50), 0.0001f)
        assertEquals(0.95f, ExamCountdownManager.RETENTION_SPRINT, 0.0001f)
    }

    // ===================== C3.9: 阶段切换平滑过渡 =====================

    /** C3.9: getTransitionFactor 7 天内从 0 到 1 线性递增 */
    @Test
    fun transitionFactor_increasesLinearlyOver7Days() {
        val today = LocalDate.of(2026, 7, 10)
        // 切换当天（0 天）→ 0.0
        assertEquals(0.0f, ExamCountdownManager.getTransitionFactor(today, today), 0.0001f)
        // 切换后第 3 天 → 3/7
        assertEquals(3f / 7f, ExamCountdownManager.getTransitionFactor(today.minusDays(3), today), 0.0001f)
        // 切换后第 7 天（>=TRANSITION_DAYS）→ 1.0
        assertEquals(1.0f, ExamCountdownManager.getTransitionFactor(today.minusDays(7), today), 0.0001f)
        // 线性递增验证
        for (d in 0..7) {
            val expected = if (d >= ExamCountdownManager.TRANSITION_DAYS) {
                1.0f
            } else {
                d.toFloat() / ExamCountdownManager.TRANSITION_DAYS
            }
            assertEquals(
                expected,
                ExamCountdownManager.getTransitionFactor(today.minusDays(d.toLong()), today),
                0.0001f
            )
        }
    }

    // ===================== 考研日期计算 =====================

    /** 考研日期计算：每年 12 月倒数第二个周六（以 2026 年为例 = 2026-12-19） */
    @Test
    fun examDate_isSecondToLastSaturdayOfDecember() {
        val examDate = ExamCountdownManager.getExamDate(2026)
        assertEquals(LocalDate.of(2026, 12, 19), examDate)
        assertEquals(DayOfWeek.SATURDAY, examDate.dayOfWeek)
        assertEquals(Month.DECEMBER, examDate.month)
        // 倒数第二个周六：下一周六（最后一周六）仍在 12 月，再下一周六已跨年
        assertEquals(Month.DECEMBER, examDate.plusWeeks(1).month)
        assertEquals(Month.JANUARY, examDate.plusWeeks(2).month)
    }

    // ===================== 阶段边界 =====================

    /** getStudyPhase 边界：181→BASIC, 180→INTENSIVE, 90→INTENSIVE, 89→SPRINT */
    @Test
    fun studyPhase_boundaries() {
        assertEquals(StudyPhase.BASIC, ExamCountdownManager.getStudyPhase(181))
        assertEquals(StudyPhase.INTENSIVE, ExamCountdownManager.getStudyPhase(180))
        assertEquals(StudyPhase.INTENSIVE, ExamCountdownManager.getStudyPhase(90))
        assertEquals(StudyPhase.SPRINT, ExamCountdownManager.getStudyPhase(89))
    }

    // ===================== getTransitionedRetention 线性插值 =====================

    /** getTransitionedRetention 线性插值（old=0.85, new=0.90, factor=0.5 → 0.875） */
    @Test
    fun transitionedRetention_linearInterpolation() {
        assertEquals(0.875f, ExamCountdownManager.getTransitionedRetention(0.85f, 0.90f, 0.5f), 0.0001f)
        // factor=0 → 旧值
        assertEquals(0.85f, ExamCountdownManager.getTransitionedRetention(0.85f, 0.90f, 0.0f), 0.0001f)
        // factor=1 → 新值
        assertEquals(0.90f, ExamCountdownManager.getTransitionedRetention(0.85f, 0.90f, 1.0f), 0.0001f)
    }

    // ===================== resolveRetention 取 maxOf（不降级） =====================

    /** resolveRetention 取 maxOf（TIER_EXACT 0.95 + 全局 0.85 → 0.95，不降级） */
    @Test
    fun resolveRetention_takesMaxOf() {
        assertEquals(0.95f, ExamCountdownManager.resolveRetention(MemoryTier.TIER_EXACT, 0.85f), 0.0001f)
        // 卡片档位低于全局时取全局（TIER_UNDERSTAND 0.85 + 全局 0.90 → 0.90）
        assertEquals(0.90f, ExamCountdownManager.resolveRetention(MemoryTier.TIER_UNDERSTAND, 0.90f), 0.0001f)
    }
}
