package com.wenyan.app.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WritingMaterialWithSources(
    @Embedded val material: WritingMaterialEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "writing_material_id",
    )
    val sources: List<DataSourceEntity>,
)
