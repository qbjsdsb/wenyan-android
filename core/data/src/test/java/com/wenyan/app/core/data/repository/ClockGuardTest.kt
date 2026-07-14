package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.entity.AppMetaEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [ClockGuard] 单元测试。
 *
 * 覆盖：
 * - 首次启动（lastKnown=null）→ 返回 current，写入 lastKnown
 * - 正常推进（current > lastKnown）→ 返回 current，更新 lastKnown
 * - 深回拨（current < lastKnown - TOLERANCE）→ 返回 lastKnown + 1，更新 lastKnown
 * - 浅回拨（current >= lastKnown - TOLERANCE）→ 返回 current（视为正常）
 * - P1-AUDIT-2 修复：回拨期间多次调用返回值严格单调递增
 *
 * 通过预填 DB（lastKnown 设为未来时间）模拟回拨场景，无需 mock System.currentTimeMillis()。
 */
class ClockGuardTest {

    private lateinit var dao: FakeAppMetaDao
    private lateinit var clockGuard: ClockGuard

    @Before
    fun setup() {
        dao = FakeAppMetaDao()
        clockGuard = ClockGuard(dao)
    }

    @Test
    fun firstCall_lastKnownNull_returnsCurrentAndWritesToDb() = runTest {
        val result = clockGuard.effectiveNowMillis()

        val now = System.currentTimeMillis()
        assertTrue("first call should return ~current time", now - result < 1000L)

        val stored = dao.getByKey("last_known_timestamp_ms")
        assertNotNull(stored)
        assertEquals(result, stored?.longValue)
    }

    @Test
    fun normalAdvance_currentGreaterThanLastKnown_returnsCurrentAndUpdates() = runTest {
        // 预填 lastKnown 为过去 1 小时
        val pastTime = System.currentTimeMillis() - 3_600_000L
        dao.upsert(AppMetaEntity(key = "last_known_timestamp_ms", longValue = pastTime))

        val result = clockGuard.effectiveNowMillis()

        val now = System.currentTimeMillis()
        assertTrue("normal advance should return ~current time", now - result < 1000L)

        val stored = dao.getByKey("last_known_timestamp_ms")
        assertEquals(result, stored?.longValue)
    }

    @Test
    fun deepRollback_currentBelowTolerance_returnsLastKnownPlus1() = runTest {
        // 预填 lastKnown 为未来 1 小时（模拟之前时间前移过，现在回拨到正常时间）
        val futureTime = System.currentTimeMillis() + 3_600_000L
        dao.upsert(AppMetaEntity(key = "last_known_timestamp_ms", longValue = futureTime))

        val result = clockGuard.effectiveNowMillis()

        // P1-AUDIT-2 修正：回拨期间返回 lastKnown + 1（而非固定 lastKnown）
        assertEquals(futureTime + 1, result)

        val stored = dao.getByKey("last_known_timestamp_ms")
        assertEquals("DB should be updated to lastKnown + 1", futureTime + 1, stored?.longValue)
    }

    @Test
    fun shallowRollback_currentWithinTolerance_returnsCurrent() = runTest {
        // 预填 lastKnown 为 30 秒前（在 TOLERANCE_MS=60s 范围内）
        val recentPast = System.currentTimeMillis() - 30_000L
        dao.upsert(AppMetaEntity(key = "last_known_timestamp_ms", longValue = recentPast))

        val result = clockGuard.effectiveNowMillis()

        val now = System.currentTimeMillis()
        assertTrue(
            "shallow rollback within tolerance should return ~current time",
            now - result < 1000L,
        )
    }

    @Test
    fun rollback_multipleCalls_monotonicIncrement() = runTest {
        // P1-AUDIT-2 核心测试：回拨期间多次调用必须严格单调递增
        val futureTime = System.currentTimeMillis() + 3_600_000L
        dao.upsert(AppMetaEntity(key = "last_known_timestamp_ms", longValue = futureTime))

        val result1 = clockGuard.effectiveNowMillis()
        val result2 = clockGuard.effectiveNowMillis()
        val result3 = clockGuard.effectiveNowMillis()

        assertEquals(futureTime + 1, result1)
        assertEquals(futureTime + 2, result2)
        assertEquals(futureTime + 3, result3)
        assertTrue("result1 < result2", result1 < result2)
        assertTrue("result2 < result3", result2 < result3)
    }
}
