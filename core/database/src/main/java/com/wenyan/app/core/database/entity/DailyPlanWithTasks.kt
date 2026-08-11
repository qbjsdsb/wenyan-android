package com.wenyan.app.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DailyPlanWithTasks(
    @Embedded val plan: DailyPlanEntity,
    @Relation(parentColumn = "id", entityColumn = "plan_id")
    val tasks: List<DailyTaskEntity>,
)
