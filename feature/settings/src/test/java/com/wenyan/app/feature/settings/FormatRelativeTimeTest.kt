package com.wenyan.app.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [formatRelativeTime] 单元测试(v0.8.13 P1-1 新增)。
 *
 * 覆盖边界条件:
 * - 未来时间戳(时钟回拨/异常数据)返回"刚刚"(P1-1 修复核心)
 * - 刚刚(<1分钟)
 * - 分钟级
 * - 小时级
 * - 昨天(恰好 1 天)
 * - 天级
 * - 月级
 *
 * 注:[formatRelativeTime] 依赖 [System.currentTimeMillis],分钟及以上级别的断言
 * 用足够大的时间差避免与"当前时间"边界重叠(如 5 分钟不会刚好因执行延迟变成 4 分钟)。
 */
class FormatRelativeTimeTest {

    @Test
    fun futureTimestamp_returnsJustNow() {
        // P1-1 修复核心:未来时间戳不应显示负数时间(如"-3 分钟前")
        val now = System.currentTimeMillis()
        assertEquals("刚刚", formatRelativeTime(now + 3 * 60 * 1000)) // 3 分钟后
        assertEquals("刚刚", formatRelativeTime(now + 24 * 60 * 60 * 1000)) // 1 天后
        assertEquals("刚刚", formatRelativeTime(now + 1)) // 1 毫秒后
    }

    @Test
    fun zeroDiff_returnsJustNow() {
        val now = System.currentTimeMillis()
        assertEquals("刚刚", formatRelativeTime(now))
    }

    @Test
    fun lessThanOneMinute_returnsJustNow() {
        val now = System.currentTimeMillis()
        assertEquals("刚刚", formatRelativeTime(now - 30 * 1000)) // 30 秒前
    }

    @Test
    fun minutesLevel_returnsMinutesAgo() {
        val now = System.currentTimeMillis()
        assertEquals("5分钟前", formatRelativeTime(now - 5 * 60 * 1000))
        assertEquals("59分钟前", formatRelativeTime(now - 59 * 60 * 1000))
    }

    @Test
    fun hoursLevel_returnsHoursAgo() {
        val now = System.currentTimeMillis()
        assertEquals("3小时前", formatRelativeTime(now - 3 * 60 * 60 * 1000))
        assertEquals("23小时前", formatRelativeTime(now - 23 * 60 * 60 * 1000))
    }

    @Test
    fun exactlyOneDay_returnsYesterday() {
        val now = System.currentTimeMillis()
        assertEquals("昨天", formatRelativeTime(now - 24 * 60 * 60 * 1000))
    }

    @Test
    fun daysLevel_returnsDaysAgo() {
        val now = System.currentTimeMillis()
        assertEquals("3天前", formatRelativeTime(now - 3 * 24 * 60 * 60 * 1000L))
        assertEquals("29天前", formatRelativeTime(now - 29 * 24 * 60 * 60 * 1000L))
    }

    @Test
    fun monthsLevel_returnsMonthsAgo() {
        val now = System.currentTimeMillis()
        // 60 天 ≈ 2 个月
        assertEquals("2个月前", formatRelativeTime(now - 60 * 24 * 60 * 60 * 1000L))
        // 365 天 ≈ 12 个月
        assertEquals("12个月前", formatRelativeTime(now - 365 * 24 * 60 * 60 * 1000L))
    }
}
