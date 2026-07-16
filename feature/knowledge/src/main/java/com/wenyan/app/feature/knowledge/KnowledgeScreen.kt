package com.wenyan.app.feature.knowledge

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.SmartToy
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar

/**
 * 知识点列表界面骨架。
 *
 * 布局参考 Web 原型 index.html（顶部搜索 + 分类标签 + 列表），
 * 采用 Material3 FilterChip 做分类、LazyColumn 渲染知识点卡片。
 *
 * TopAppBar 右上角提供"AI助手"入口（与底部 NavigationBar 第 5 个 Tab 形成双入口）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "知识点",
                actions = {
                    IconButton(onClick = onNavigateToAiAssistant) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI助手",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            // 分类标签行
            CategoryChips(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::selectCategory,
            )

            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.error, uiState.knowledgePoints.isEmpty()),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "knowledge_state",
                modifier = Modifier.fillMaxSize(),
            ) { (isLoading, error, isEmpty) ->
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            WenyanLoadingIndicator()
                        }
                    }
                    // P0-6 修复：加 error 分支，数据加载失败时展示错误信息 + 重试按钮
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ErrorState(
                                icon = Icons.Default.CloudOff,
                                title = "加载失败",
                                message = error,
                                onRetry = viewModel::retry,
                            )
                        }
                    }
                    isEmpty -> {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = "暂无知识点，等待种子数据加载",
                        )
                    }
                    else -> {
                        KnowledgeList(
                            items = uiState.knowledgePoints,
                            onNavigateToDetail = onNavigateToDetail,
                            contentPadding = PaddingValues(Spacing.lg),
                        )
                    }
                }
            }
        }
    }
}

// 分类标签行
@Composable
private fun CategoryChips(
    selectedCategory: KnowledgeCategory,
    onCategorySelected: (KnowledgeCategory) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // P2-LAZY-1 修正：LazyRow items 加 key（用 category.name 唯一标识），避免重组时丢失选中状态
        // NF-UP4 修正：加 contentType 让 LazyRow 复用同一类型 item 的 slot，提升滚动性能
        items(items = KnowledgeCategory.entries.toList(), key = { it.name }, contentType = { "category" }) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.label) },
            )
        }
    }
}

// 知识点列表
@Composable
private fun KnowledgeList(
    items: List<KnowledgePointItem>,
    onNavigateToDetail: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(items = items, key = { it.id }, contentType = { "knowledgeItem" }) { item ->
            KnowledgePointCard(
                item = item,
                onClick = { onNavigateToDetail(item.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

// 单个知识点卡片
@Composable
private fun KnowledgePointCard(
    item: KnowledgePointItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalCard(
        modifier = modifier
            .fillMaxWidth()
            // NF-UA4 修复：加 role=Role.Button 语义，TalkBack 朗读"按钮"，
            // 视障用户才能识别卡片可点击。原 .clickable 无 role，TalkBack 不朗读"按钮"。
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.subject,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// 空状态占位（已迁移至共享 EmptyState 组件）
