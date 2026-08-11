package com.wenyan.app.core.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WritingSessionEntityTest {
    @Test fun unknownPersistedEnumsFailClosed() {
        assertNull(WritingSessionMode.fromDb("OTHER"))
        assertNull(WritingSessionState.fromDb("OTHER"))
    }

    @Test fun supportedModesAreExplicitAndStable() {
        assertEquals(listOf("OUTLINE_10_MIN", "MICRO_30_MIN", "FULL_TIMED"), WritingSessionMode.entries.map { it.name })
    }
}
