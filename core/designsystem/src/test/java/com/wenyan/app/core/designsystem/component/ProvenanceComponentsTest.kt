package com.wenyan.app.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class ProvenanceComponentsTest {
    @Test fun `unknown status uses restrained legacy presentation`() {
        val result = provenancePresentation("FUTURE_STATUS")
        assertEquals(ProvenanceTone.LEGACY, result.tone)
        assertEquals("历史资料", result.label)
    }

    @Test fun `legacy is not represented as dangerous rejected state`() {
        assertEquals(ProvenanceTone.LEGACY, provenancePresentation("LEGACY_UNVERIFIED").tone)
        assertEquals(ProvenanceTone.REJECTED, provenancePresentation("REJECTED").tone)
    }

    @Test fun `page labels handle ranges and missing bounds without invention`() {
        assertEquals(null, pageRangeLabel(null, null))
        assertEquals("第 12 页", pageRangeLabel(12, 12))
        assertEquals("第 12–18 页", pageRangeLabel(12, 18))
        assertEquals("至第 18 页", pageRangeLabel(null, 18))
    }

    @Test fun `unknown evidence type is explicit`() {
        assertEquals("来源类型待确认", sourceEvidenceLabel("FUTURE_TYPE"))
    }
}
