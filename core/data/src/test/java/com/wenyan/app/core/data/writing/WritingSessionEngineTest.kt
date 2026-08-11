package com.wenyan.app.core.data.writing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingSessionEngineTest {
    @Test fun `modes have stable durations`() {
        assertEquals(listOf(600_000L, 1_800_000L, 3_600_000L), WritingMode.entries.map { it.durationMs })
    }

    @Test fun `pause resume persists elapsed without per-second writes`() {
        val paused = pause(PersistedTimer(1_000, 500, false), 4_000)
        assertEquals(3_500, paused.elapsedBeforePauseMs)
        assertEquals(3_500, elapsedMs(paused, 999_999))
        assertEquals(4_500, elapsedMs(resume(paused, 10_000), 11_000))
    }

    @Test fun `active timer ignores wall-clock changes after restoration`() {
        val active = ActiveWritingTimer(PersistedTimer(1_000, 500, false), wallNowMs = 4_000, monotonicNowMs = 20_000)
        assertEquals(3_500, active.elapsedAt(20_000))
        assertEquals(4_500, active.elapsedAt(21_000))
    }

    @Test fun `clock rollback cannot make restored elapsed negative`() {
        assertEquals(50, elapsedMs(PersistedTimer(1_000, 50, false), 900))
    }

    @Test fun `long offline body is recognized as recoverable`() {
        assertTrue(WritingDraft("", "", "[]", "[]", "文".repeat(20_000)).hasUnsavedContent())
    }

    @Test fun `evidence references use valid deduplicated json`() {
        val encoded = encodeEvidenceRefs(listOf("wm:1", "wm:1", "quoted\"id"))
        assertEquals(listOf("wm:1", "quoted\"id"), decodeEvidenceRefs(encoded))
        assertEquals(emptyList<String>(), decodeEvidenceRefs("not-json"))
    }
}
