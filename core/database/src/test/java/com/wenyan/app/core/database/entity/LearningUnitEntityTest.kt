package com.wenyan.app.core.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LearningUnitEntityTest {
    @Test
    fun `stable ids use point type position and never content hash`() {
        LearningUnitType.entries.forEach { type ->
            val id = LearningUnitId.create("kp_00001", type, 2)
            assertEquals("kp_00001:${type.name.lowercase()}:2", id)
            assertFalse(id.contains("answer text"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative position is rejected`() {
        LearningUnitId.create("kp_00001", LearningUnitType.CORE, -1)
    }
}
