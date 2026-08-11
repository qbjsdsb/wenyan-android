package com.wenyan.app.core.data.learning

import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.LearningUnitEntity
import com.wenyan.app.core.database.entity.LearningUnitId
import com.wenyan.app.core.database.entity.LearningUnitType

/** Pure, deterministic decomposition. It never calls a model or guesses absent structure. */
object LearningUnitGenerator {
    fun generate(point: KnowledgePointEntity, now: Long): List<LearningUnitEntity> = buildList {
        add(
            unit(
                point = point,
                type = LearningUnitType.CORE,
                position = 0,
                prompt = point.title,
                answer = point.coreConclusion,
                now = now,
            ),
        )
        point.tags.orEmpty()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .forEachIndexed { position, keyword ->
                add(
                    unit(
                        point = point,
                        type = LearningUnitType.KEYWORD,
                        position = position,
                        prompt = "解释关键词：$keyword",
                        answer = keyword,
                        now = now,
                    ),
                )
            }
    }

    private fun unit(
        point: KnowledgePointEntity,
        type: LearningUnitType,
        position: Int,
        prompt: String,
        answer: String,
        now: Long,
    ) = LearningUnitEntity(
        id = LearningUnitId.create(point.id, type, position),
        pointId = point.id,
        unitType = type.name,
        position = position,
        prompt = prompt,
        answer = answer,
        active = true,
        createdAt = now,
        updatedAt = now,
    )
}
