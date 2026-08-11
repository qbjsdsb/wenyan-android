package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LearningUnitType {
    CORE,
    KEYWORD,
    SEQUENCE,
    COMPARE,
    EVIDENCE,
    EXAM_OUTLINE,
}

object LearningUnitId {
    fun create(pointId: String, type: LearningUnitType, position: Int): String {
        require(pointId.isNotBlank()) { "pointId must not be blank" }
        require(position >= 0) { "position must be non-negative" }
        return "$pointId:${type.name.lowercase()}:$position"
    }
}

@Entity(
    tableName = "learning_units",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["point_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("point_id"),
        Index("active"),
        Index(value = ["point_id", "unit_type", "position"], unique = true),
    ],
)
data class LearningUnitEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "point_id") val pointId: String,
    @ColumnInfo(name = "unit_type") val unitType: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "prompt") val prompt: String,
    @ColumnInfo(name = "answer") val answer: String,
    @ColumnInfo(name = "active", defaultValue = "1") val active: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
