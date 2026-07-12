package com.wenyan.app.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.ContentSourceBadge
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity

/**
 * 知识点详情界面（Spec C1.27 多教材对照 + C7.2 来源溯源）。
 *
 * 功能：
 * - 标题 + 考频 + 难度 + 内容来源标签
 * - 多教材对照（core_conclusion 马工程 / study_text 袁行霈 / multi_perspectives 游国恩等）
 * - 来源溯源列表（data_sources 表记录）
 * - 关联/对比/延伸知识点（点击跳转）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgePointDetailScreen(
    onBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: KnowledgePointDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.point?.title ?: "知识点详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.notFound || uiState.point == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "知识点不存在",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    val point = uiState.point!!
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // ── 标题区 ──
                        HeaderSection(point)

                        // ── 摘要 ──
                        point.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                            InfoSection(title = "摘要") {
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        // ── 多教材对照 ──
                        MultiPerspectiveSection(point)

                        // ── 来源溯源 ──
                        if (uiState.sources.isNotEmpty()) {
                            SourcesSection(uiState.sources)
                        }

                        // ── 关联知识点 ──
                        RelatedPointsSection(
                            detail = uiState.detail,
                            onNavigateToDetail = onNavigateToDetail,
                        )
                    }
                }
            }
        }
    }
}

// ── 标题区 ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderSection(point: KnowledgePointEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = point.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        // 考频 + 难度 + 内容来源标签
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 内容来源标签（五级+1特殊状态）
            ContentSourceBadge(
                contentSource = point.contentSource,
                stageLabel = null,
            )

            // 考频标签
            val freqLabel = when (point.examFrequency) {
                "HIGH" -> "高频"
                "MEDIUM" -> "中频"
                "LOW" -> "低频"
                else -> "未考"
            }
            InfoChip(text = freqLabel)

            // 难度标签
            InfoChip(text = "难度 ${point.difficulty}/5")
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ── 通用信息区块 ────────────────────────────────────────────

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

// ── 多教材对照 ──────────────────────────────────────────────

@Composable
private fun MultiPerspectiveSection(point: KnowledgePointEntity) {
    val hasCoreConclusion = point.coreConclusion.isNotBlank()
    val hasStudyText = !point.studyText.isNullOrBlank()
    val hasMultiPerspectives = !point.multiPerspectives.isNullOrEmpty()

    if (!hasCoreConclusion && !hasStudyText && !hasMultiPerspectives) return

    InfoSection(title = "多教材对照") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 答题基准（马工程版）
            if (hasCoreConclusion) {
                PerspectiveCard(
                    label = "答题基准（马工程）",
                    content = point.coreConclusion,
                    isOfficial = true,
                )
            }

            // 学习理解（袁行霈版）
            if (hasStudyText) {
                PerspectiveCard(
                    label = "学习理解（袁行霈）",
                    content = point.studyText!!,
                    isOfficial = false,
                )
            }

            // 多视角（游国恩等其他教材）
            point.multiPerspectives?.forEach { (versionName, content) ->
                if (content.isNotBlank()) {
                    PerspectiveCard(
                        label = versionName,
                        content = content,
                        isOfficial = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun PerspectiveCard(
    label: String,
    content: String,
    isOfficial: Boolean,
) {
    Surface(
        color = if (isOfficial) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isOfficial) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOfficial) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

// ── 来源溯源 ────────────────────────────────────────────────

@Composable
private fun SourcesSection(sources: List<DataSourceEntity>) {
    InfoSection(title = "资料来源（${sources.size}）") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sources.forEach { source ->
                SourceRow(source)
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun SourceRow(source: DataSourceEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContentSourceBadge(
            contentSource = source.contentSource,
            stageLabel = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.sourceFile,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            source.sourcePage?.let { page ->
                Text(
                    text = "第 $page 页",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── 关联知识点 ──────────────────────────────────────────────

@Composable
private fun RelatedPointsSection(
    detail: com.wenyan.app.core.data.repository.KnowledgePointDetail?,
    onNavigateToDetail: (String) -> Unit,
) {
    if (detail == null) return

    val hasRelated = detail.relatedPoints.isNotEmpty()
    val hasContrast = detail.contrastPoints.isNotEmpty()
    val hasExtension = detail.extensionPoints.isNotEmpty()

    if (!hasRelated && !hasContrast && !hasExtension) return

    InfoSection(title = "关联知识点") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hasRelated) {
                RelatedGroup(
                    title = "关联",
                    points = detail.relatedPoints,
                    onNavigateToDetail = onNavigateToDetail,
                )
            }
            if (hasContrast) {
                RelatedGroup(
                    title = "对比",
                    points = detail.contrastPoints,
                    onNavigateToDetail = onNavigateToDetail,
                )
            }
            if (hasExtension) {
                RelatedGroup(
                    title = "延伸",
                    points = detail.extensionPoints,
                    onNavigateToDetail = onNavigateToDetail,
                )
            }
        }
    }
}

@Composable
private fun RelatedGroup(
    title: String,
    points: List<KnowledgePointEntity>,
    onNavigateToDetail: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        points.forEach { point ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                onClick = { onNavigateToDetail(point.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = point.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
