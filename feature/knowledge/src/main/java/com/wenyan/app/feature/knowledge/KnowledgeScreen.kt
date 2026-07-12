package com.wenyan.app.feature.knowledge

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 知识点列表界面骨架。
 *
 * 布局参考 Web 原型 index.html（顶部搜索 + 分类标签 + 列表），
 * 采用 Material3 FilterChip 做分类、LazyColumn 渲染知识点卡片。
 *
 * TopAppBar 右上角提供"导师信息"入口（Spec C6.8，外链南师大文学院官网）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    onNavigateToMentor: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知识点") },
                actions = {
                    IconButton(onClick = onNavigateToMentor) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "导师信息",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 分类标签行
            CategoryChips(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::selectCategory,
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.knowledgePoints.isEmpty()) {
                EmptyState(text = "暂无知识点，等待种子数据加载")
            } else {
                KnowledgeList(
                    items = uiState.knowledgePoints,
                    onNavigateToDetail = onNavigateToDetail,
                    contentPadding = PaddingValues(16.dp),
                )
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(KnowledgeCategory.entries.toList()) { category ->
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items) { item ->
            KnowledgePointCard(item, onClick = { onNavigateToDetail(item.id) })
        }
    }
}

// 单个知识点卡片
@Composable
private fun KnowledgePointCard(
    item: KnowledgePointItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

// 空状态占位
@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
