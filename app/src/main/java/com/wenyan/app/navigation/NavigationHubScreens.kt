package com.wenyan.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
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
    ExpressiveScaffold(
        topBar = { WenyanLargeTopAppBar(title = "训练") },
    ) { padding ->
        TrainingHubContent(
            entries = trainingEntries(),
            onNavigate = onNavigate,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun TrainingHubContent(
    entries: List<TrainingEntry>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaxContentWidth.comfortable)
                .padding(horizontal = Spacing.lg),
            contentPadding = PaddingValues(vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { NavigationHubIntro("选择一个入口开始练习") }
            item {
                if (entries.isEmpty()) {
                    NavigationHubEmptyState()
                } else {
                    GroupedCard(title = "训练入口") {
                        entries.forEachIndexed { index, entry ->
                            if (index > 0) GroupedCardDivider()
                            GroupedCardItem(
                                title = entry.title,
                                description = entry.description,
                                leadingIcon = entry.icon,
                                onClick = { onNavigate(entry.route) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MyHubScreen(onNavigate: (String) -> Unit) {
    ExpressiveScaffold(
        topBar = { WenyanLargeTopAppBar(title = "我的") },
    ) { padding ->
        NavigationHub(
            entries = listOf(
                TrainingEntry("错题本", "集中处理仍未解决的错题", TopLevelDestination.ROUTE_WRONG_ANSWER, Icons.Default.Replay),
                TrainingEntry("设置", "主题、学习计划与应用偏好", TopLevelDestination.ROUTE_SETTINGS, Icons.Default.Settings),
                TrainingEntry("AI 助手", "用对话辅助理解和复盘", ROUTE_AI_ASSISTANT, Icons.Default.SmartToy),
            ),
            onNavigate = onNavigate,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun NavigationHub(
    entries: List<TrainingEntry>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaxContentWidth.comfortable)
                .padding(horizontal = Spacing.lg),
            contentPadding = PaddingValues(vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { NavigationHubIntro("常用功能集中在这里") }
            item {
                GroupedCard(title = "我的功能") {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) GroupedCardDivider()
                        GroupedCardItem(
                            title = entry.title,
                            description = entry.description,
                            leadingIcon = entry.icon,
                            onClick = { onNavigate(entry.route) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationHubIntro(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NavigationHubEmptyState() {
    Text(
        text = "暂无可用训练入口，请稍后重试。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Training 2x font", fontScale = 2f, showBackground = true)
@Preview(name = "Training landscape", widthDp = 900, heightDp = 500, showBackground = true)
@Composable
private fun TrainingHubPreview() = WenyanTheme {
    TrainingHubContent(trainingEntries(), onNavigate = {})
}
