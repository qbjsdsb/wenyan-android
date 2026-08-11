package com.wenyan.app.core.data.writing

enum class RubricDimension(val label: String) {
    THESIS("立意"), STRUCTURE("结构"), THEORY("理论"), TEXTUAL_EVIDENCE("文本证据"),
    ANALYSIS("分析"), LANGUAGE("语言"), TIME("时间"),
}

enum class RubricLevel(val points: Int, val explanation: String) {
    NEEDS_WORK(1, "尚需补足，记录具体问题"), DEVELOPING(2, "已有基础，仍需明确改进"),
    SOLID(3, "结构清楚且有可核对依据"), STRONG(4, "完成度高，仍不代表官方评分"),
}

data class RubricMark(val dimension: RubricDimension, val level: RubricLevel, val note: String = "")
data class RubricAssessment(val marks: List<RubricMark>) {
    init { require(marks.map { it.dimension }.toSet().size == marks.size) }
    val total get() = marks.sumOf { it.level.points }
    val maximum get() = RubricDimension.entries.size * RubricLevel.STRONG.points
    val weaknesses get() = marks.filter { it.level.points <= 2 }.map { it.dimension }
}
data class EvidenceCandidate(val id: String, val status: String)
data class DimensionTrend(val dimension: RubricDimension, val points: List<Int>, val direction: String)

fun citableEvidence(items: List<EvidenceCandidate>) = items.distinctBy { it.id }.filter { it.status == "REVIEWED" }
fun evidenceClues(items: List<EvidenceCandidate>) = items.distinctBy { it.id }.filter { it.status != "REVIEWED" }
fun trends(history: List<RubricAssessment>) = RubricDimension.entries.map { dimension ->
    val values = history.mapNotNull { assessment -> assessment.marks.firstOrNull { it.dimension == dimension }?.level?.points }
    DimensionTrend(dimension, values, when {
        values.size < 2 -> "首次记录"
        values.last() > values.first() -> "改善"
        values.last() < values.first() -> "需关注"
        else -> "持平"
    })
}
fun followUpTasks(assessment: RubricAssessment) = assessment.weaknesses.map { "针对${it.label}安排一次离线修复练习" }
fun encodeAssessment(assessment: RubricAssessment) = assessment.marks.sortedBy { it.dimension.ordinal }.joinToString("|") {
    "${it.dimension.name},${it.level.name},${it.note.replace("|", " ").replace(",", " ")}"
}
fun decodeAssessment(value: String) = RubricAssessment(value.split('|').mapNotNull { part ->
    val fields = part.split(',', limit = 3)
    if (fields.size < 2) null else runCatching {
        RubricMark(RubricDimension.valueOf(fields[0]), RubricLevel.valueOf(fields[1]), fields.getOrElse(2) { "" })
    }.getOrNull()
}.distinctBy { it.dimension })
