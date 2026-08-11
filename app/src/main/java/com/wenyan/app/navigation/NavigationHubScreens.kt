package com.wenyan.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.theme.WenyanTheme

internal data class TrainingEntry(
    val title: String,
    val description: String,
    val route: String,
    val icon: ImageVector,
)

internal fun trainingEntries(): List<TrainingEntry> = listOf(
    TrainingEntry("快速回忆", "继续 LearningUnit 记忆卡片", TopLevelDestination.ROUTE_CARDS, Icons.AutoMirrored.Filled.MenuBook),
    TrainingEntry("真题作答", "按真实题目进入现有真题训练", ROUTE_QUIZ_PRACTICE, Icons.Default.HistoryEdu),
    TrainingEntry("610 写作", "从现有写作材料开始", ROUTE_WRITING_MATERIALS, Icons.Default.EditNote),
    TrainingEntry("错题修复", "回到已有错题本处理待修复内容", TopLevelDestination.ROUTE_WRONG_ANSWER, Icons.Default.Replay),
)

@Composable
internal fun TrainingHubScreen(onNavigate: (String) -> Unit) {
    TrainingHubContent(trainingEntries(), onNavigate)
}

@Composable
private fun TrainingHubContent(entries: List<TrainingEntry>, onNavigate: (String) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaxContentWidth.comfortable)
                .padding(horizontal = Spacing.lg),
            contentPadding = PaddingValues(vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { Text("训练", style = MaterialTheme.typography.headlineMedium) }
            if (entries.isEmpty()) {
                item { Text("暂无可用训练入口，请稍后重试。") }
            } else {
                items(entries, key = TrainingEntry::route) { entry ->
                    FilledTonalButton(
                        onClick = { onNavigate(entry.route) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(entry.icon, contentDescription = null)
                        Column(Modifier.padding(start = Spacing.md).fillMaxWidth()) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium)
                            Text(entry.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MyHubScreen(onNavigate: (String) -> Unit) {
    NavigationHub(
        title = "我的",
        entries = listOf(
            "错题本" to TopLevelDestination.ROUTE_WRONG_ANSWER,
            "设置" to TopLevelDestination.ROUTE_SETTINGS,
            "AI 助手" to ROUTE_AI_ASSISTANT,
        ),
        onNavigate = onNavigate,
    )
}

@Composable
private fun NavigationHub(title: String, entries: List<Pair<String, String>>, onNavigate: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        entries.forEach { (label, route) ->
            FilledTonalButton(onClick = { onNavigate(route) }, modifier = Modifier.fillMaxWidth()) {
                Text(label)
            }
        }
    }
}

@Preview(name = "Training 2x font", fontScale = 2f, showBackground = true)
@Preview(name = "Training landscape", widthDp = 900, heightDp = 500, showBackground = true)
@Composable
private fun TrainingHubPreview() = WenyanTheme {
    TrainingHubContent(trainingEntries(), onNavigate = {})
}
