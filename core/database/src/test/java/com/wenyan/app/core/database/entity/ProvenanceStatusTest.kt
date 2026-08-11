package com.wenyan.app.core.database.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class ProvenanceStatusTest {
    @Test
    fun unknownContentStatusFailsClosed() {
        assertEquals(ContentReviewStatus.LEGACY_UNVERIFIED, ContentReviewStatus.fromStorage("FUTURE"))
        assertEquals(ContentReviewStatus.LEGACY_UNVERIFIED, ContentReviewStatus.fromStorage(null))
    }

    @Test
    fun unknownSourceTypeFailsClosed() {
        assertEquals(SourceEvidenceType.UNKNOWN, SourceEvidenceType.fromStorage("FUTURE"))
        assertEquals(SourceEvidenceType.UNKNOWN, SourceEvidenceType.fromStorage(null))
    }

    @Test
    fun knownValuesRoundTrip() {
        ContentReviewStatus.entries.forEach { assertEquals(it, ContentReviewStatus.fromStorage(it.name)) }
        SourceEvidenceType.entries.forEach { assertEquals(it, SourceEvidenceType.fromStorage(it.name)) }
    }
}
