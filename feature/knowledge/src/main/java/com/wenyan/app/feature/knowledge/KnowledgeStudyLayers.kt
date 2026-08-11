package com.wenyan.app.feature.knowledge

import com.wenyan.app.core.database.entity.KnowledgePointEntity

internal enum class KnowledgeStudyLayer {
    RECALL_30_SECONDS,
    OUTLINE_2_MINUTES,
    EXAM_EXPRESSION,
    UNDERSTANDING,
    EVIDENCE,
}

internal data class KnowledgeStudyLayerContent(
    val layer: KnowledgeStudyLayer,
    val content: String?,
)

/** Maps only existing, persisted content. Missing layers stay null rather than being generated. */
internal fun KnowledgePointEntity.studyLayerContents(): List<KnowledgeStudyLayerContent> = listOf(
    KnowledgeStudyLayerContent(KnowledgeStudyLayer.RECALL_30_SECONDS, summary?.takeIf(String::isNotBlank)),
    KnowledgeStudyLayerContent(KnowledgeStudyLayer.OUTLINE_2_MINUTES, null),
    KnowledgeStudyLayerContent(KnowledgeStudyLayer.EXAM_EXPRESSION, coreConclusion.takeIf(String::isNotBlank)),
    KnowledgeStudyLayerContent(
        KnowledgeStudyLayer.UNDERSTANDING,
        buildList {
            studyText?.takeIf(String::isNotBlank)?.let(::add)
            multiPerspectives.orEmpty().forEach { (source, text) ->
                text.takeIf(String::isNotBlank)?.let { add("$source\n$it") }
            }
        }.joinToString("\n\n").takeIf(String::isNotBlank),
    ),
    KnowledgeStudyLayerContent(KnowledgeStudyLayer.EVIDENCE, null),
)

internal fun nextStudyLayer(revealed: Set<KnowledgeStudyLayer>): KnowledgeStudyLayer? =
    KnowledgeStudyLayer.entries.firstOrNull { it !in revealed }

internal fun decodeStudyLayers(names: Collection<String>): Set<KnowledgeStudyLayer> =
    names.mapNotNull { name -> KnowledgeStudyLayer.entries.find { it.name == name } }.toSet()
