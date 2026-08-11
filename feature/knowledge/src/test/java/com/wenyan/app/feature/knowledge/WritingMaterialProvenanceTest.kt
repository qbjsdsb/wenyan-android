package com.wenyan.app.feature.knowledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WritingMaterialProvenanceTest {
    @Test fun placeholderOrAbsentSourceIsHidden() {
        assertNull(visibleWritingMaterialSource(null))
        assertNull(visibleWritingMaterialSource("其他"))
        assertNull(visibleWritingMaterialSource("  待补 "))
    }

    @Test fun realSourceIsTrimmedAndShown() {
        assertEquals("教材甲", visibleWritingMaterialSource("  教材甲 "))
    }
}
