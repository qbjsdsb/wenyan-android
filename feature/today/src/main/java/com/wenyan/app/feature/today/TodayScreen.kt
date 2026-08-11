package com.wenyan.app.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator

@Composable
fun TodayRoute(
    onTaskClick: (TodayTaskUi) -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(state, onTaskClick, onRetry = viewModel::retry)
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onTaskClick: (TodayTaskUi) -> Unit,
    onRetry: () -> Unit = {},
) {
    when {
        state.isLoading -> WenyanLoadingIndicator(modifier = Modifier.fillMaxSize())
        state.error != null -> ErrorState(Icons.Default.Warning, "加载失败", onRetry, message = state.error)
        state.tasks.isEmpty() -> EmptyState(
            icon = Icons.Default.EventNote,
            title = "今天还没有学习任务",
            description = state.infeasibleMessage ?: "计划生成后会显示在这里",
            action = { Button(onClick = onRetry) { Text("重新加载今日计划") } },
        )
        else -> TodayContent(state, onTaskClick)
    }
}

@Composable
private fun TodayContent(state: TodayUiState, onTaskClick: (TodayTaskUi) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("今日学习", style = MaterialTheme.typography.headlineMedium)
                Text(
                    listOfNotNull(
                        state.countdownDays?.let { "距考试 $it 天" },
                        "预计 ${state.estimatedMinutes} 分钟",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                )
                state.infeasibleMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.nextTask?.let { next ->
                    Button(onClick = { onTaskClick(next) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("继续学习")
                    }
                }
                if (state.isFinished) Text("今日计划已完成", style = MaterialTheme.typography.titleLarge)
            }
        }
        TodayGroup.entries.forEach { group ->
            val grouped = state.tasks.filter { it.group == group }
            if (grouped.isNotEmpty()) {
                item { Text(group.label, style = MaterialTheme.typography.titleMedium) }
                items(grouped, key = { it.id }) { task -> TodayTaskCard(task, onTaskClick) }
            }
        }
    }
}

@Composable
private fun TodayTaskCard(task: TodayTaskUi, onTaskClick: (TodayTaskUi) -> Unit) {
    Card(
        onClick = { onTaskClick(task) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (task.completed) Icon(Icons.Default.CheckCircle, contentDescription = "已完成")
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text("约 ${task.estimatedMinutes} 分钟", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val TodayGroup.label: String get() = when (this) {
    TodayGroup.DUE -> "到期复习"
    TodayGroup.REPAIR -> "遗忘修复"
    TodayGroup.NEW -> "新学内容"
    TodayGroup.OUTPUT -> "输出训练"
    TodayGroup.WRITING -> "专业写作"
    TodayGroup.OTHER -> "其他任务"
}

private val previewState = TodayUiState(
    date = "2026-08-11", countdownDays = 130, estimatedMinutes = 35,
    tasks = listOf(
        TodayTaskUi("1", "鲁迅：核心结论", TodayGroup.DUE, 5, false, TodayDestination.CARDS, "kp_1"),
        TodayTaskUi("2", "论述题输出训练", TodayGroup.OUTPUT, 15, false, TodayDestination.QUIZ, "eq_1"),
        TodayTaskUi("3", "610 专业写作", TodayGroup.WRITING, 15, true, TodayDestination.WRITING_MATERIALS, null),
    ),
)

@Preview(showBackground = true, fontScale = 2f)
@Composable private fun TodayLargeFontPreview() = TodayScreen(previewState, onTaskClick = {})

@Preview(showBackground = true, device = "spec:width=900dp,height=500dp,dpi=420")
@Composable private fun TodayLandscapePreview() = TodayScreen(previewState, onTaskClick = {})
