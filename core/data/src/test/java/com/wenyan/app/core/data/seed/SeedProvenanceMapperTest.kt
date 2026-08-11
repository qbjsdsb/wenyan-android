package com.wenyan.app.core.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedProvenanceMapperTest {
    @Test
    fun `only explicit valid reviewed metadata becomes reviewed`() {
        assertEquals("REVIEWED", SeedProvenanceMapper.contentStatus("REVIEWED", null, null))
        assertEquals("LEGACY_UNVERIFIED", SeedProvenanceMapper.contentStatus(null, null, null))
        assertEquals("LEGACY_UNVERIFIED", SeedProvenanceMapper.contentStatus("DRAFT", null, null))
        assertEquals("LEGACY_UNVERIFIED", SeedProvenanceMapper.contentStatus("future-value", null, null))
    }

    @Test
    fun `aliases use first explicitly declared status`() {
        assertEquals("AI_DRAFT", SeedProvenanceMapper.contentStatus(null, "AI_DRAFT", "REVIEWED"))
        assertEquals("REJECTED", SeedProvenanceMapper.contentStatus(null, null, "REJECTED"))
    }

    @Test
    fun `source evidence requires both a real title and valid explicit type`() {
        assertEquals("OFFICIAL_ORIGINAL", SeedProvenanceMapper.sourceStatus("OFFICIAL_ORIGINAL", "教材"))
        assertEquals("UNKNOWN", SeedProvenanceMapper.sourceStatus("OFFICIAL_ORIGINAL", null))
        assertEquals("UNKNOWN", SeedProvenanceMapper.sourceStatus(null, "教材"))
        assertEquals("UNKNOWN", SeedProvenanceMapper.sourceStatus("future-value", "教材"))
    }

    @Test
    fun `draft and rejected content cannot enter formal learning`() {
        assertTrue(SeedProvenanceMapper.isFormalLearningContent("REVIEWED"))
        assertTrue(SeedProvenanceMapper.isFormalLearningContent("LEGACY_UNVERIFIED"))
        assertTrue(SeedProvenanceMapper.isFormalLearningContent("future-value"))
        assertFalse(SeedProvenanceMapper.isFormalLearningContent("AI_DRAFT"))
        assertFalse(SeedProvenanceMapper.isFormalLearningContent("REJECTED"))
    }

    @Test
    fun `placeholder and absent writing sources do not become evidence`() {
        val seeds = listOf(
            WritingMaterialSeed("wm_1", "文学理论", content = "a", source = "其他", sourceStatus = "OFFICIAL_ORIGINAL"),
            WritingMaterialSeed("wm_2", "文学理论", content = "b", source = null, sourceStatus = "USER_CONFIRMED"),
            WritingMaterialSeed("wm_3", "文学理论", content = "c", source = "  教材甲  ", sourceStatus = "USER_CONFIRMED"),
        )

        val entities = buildWritingMaterialDataSourceEntities(seeds, createdAt = 7L)

        assertEquals(1, entities.size)
        assertEquals("wm_3", entities.single().writingMaterialId)
        assertEquals("教材甲", entities.single().sourceTitle)
        assertEquals("USER_CONFIRMED", entities.single().sourceStatus)
    }
}
