package com.wenyan.app.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class LearningUnitWithRecord(
    @Embedded val unit: LearningUnitEntity,
    @Relation(parentColumn = "id", entityColumn = "learning_unit_id")
    val records: List<LearningUnitRecordEntity>,
) {
    val record: LearningUnitRecordEntity? get() = records.singleOrNull()
}
