package com.wenyan.app.core.database.entity

/** Human-review state of content. Unknown stored values fail closed. */
enum class ContentReviewStatus {
    REVIEWED,
    LEGACY_UNVERIFIED,
    AI_DRAFT,
    REJECTED;

    companion object {
        fun fromStorage(value: String?): ContentReviewStatus =
            entries.firstOrNull { it.name == value } ?: LEGACY_UNVERIFIED
    }
}

/** Evidence class of a source. Unknown stored values never become trusted evidence. */
enum class SourceEvidenceType {
    OFFICIAL_ORIGINAL,
    USER_CONFIRMED,
    SECONDARY_RECOLLECTION,
    UNKNOWN;

    companion object {
        fun fromStorage(value: String?): SourceEvidenceType =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}
