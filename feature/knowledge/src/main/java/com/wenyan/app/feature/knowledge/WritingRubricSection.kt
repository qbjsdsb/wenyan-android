package com.wenyan.app.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.data.writing.DimensionTrend
import com.wenyan.app.core.data.writing.RubricAssessment
import com.wenyan.app.core.data.writing.RubricDimension
import com.wenyan.app.core.data.writing.RubricLevel
import com.wenyan.app.core.data.writing.followUpTasks

@Composable
fun WritingRubricSection(
    assessment: RubricAssessment,
    trends: List<DimensionTrend>,
    onLevel: (RubricDimension, RubricLevel) -> Unit,
    onNote: (RubricDimension, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("本地自评量规", style = MaterialTheme.typography.titleMedium)
        Text(
            "总分 ${assessment.total}/${assessment.maximum}，由七个维度直接相加，仅供自我复盘，不是官方评分。",
            style = MaterialTheme.typography.bodySmall,
        )
        RubricDimension.entries.forEach { dimension ->
            val mark = assessment.marks.firstOrNull { it.dimension == dimension }
            val trend = trends.firstOrNull { it.dimension == dimension }?.direction ?: "首次记录"
            Column {
                Text("${dimension.label} · $trend")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RubricLevel.entries.forEach { level ->
                        FilterChip(
                            selected = mark?.level == level,
                            onClick = { onLevel(dimension, level) },
                            label = { Text(level.points.toString()) },
                        )
                    }
                }
                Text(mark?.level?.explanation ?: "尚未自评", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = mark?.note.orEmpty(),
                    onValueChange = { onNote(dimension, it) },
                    label = { Text("${dimension.label}备注") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        followUpTasks(assessment).forEach { Text("• $it") }
    }
}
