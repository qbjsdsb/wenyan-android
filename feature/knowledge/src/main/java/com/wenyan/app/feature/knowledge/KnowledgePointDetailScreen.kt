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
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.designsystem.component.ContentSourceBadge
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    // 副标题：考频 + 难度组合（point 为 null 时不显示）
    val subtitle = uiState.point?.let { point ->
        val freqLabel = when (point.examFrequency) {
            "HIGH" -> "高频"
            "MEDIUM" -> "中频"
            "LOW" -> "低频"
            else -> "未考"
        }
        "$freqLabel · 难度${point.difficulty}/5"
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = uiState.point?.title ?: "知识点详情",
                subtitle = subtitle,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
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
                            .padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = point.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        // 考频 + 难度 + 内容来源标签
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // 内容来源标签（五级+1特殊状态）
            ContentSourceBadge(
                contentSource = point.contentSource,
                stageLabel = null,
            )

            // 考频标签（高频/中频/低频用 PRIMARY 突出）
            val (freqLabel, freqVariant) = when (point.examFrequency) {
                "HIGH" -> "高频" to ChipVariant.PRIMARY
                "MEDIUM" -> "中频" to ChipVariant.SECONDARY
                "LOW" -> "低频" to ChipVariant.TERTIARY
                else -> "未考" to ChipVariant.NEUTRAL
            }
            WenyanInfoChip(text = freqLabel, variant = freqVariant)

            // 难度标签
            WenyanInfoChip(text = "难度 ${point.difficulty}/5")
        }
    }
}

// ── 通用信息区块 ────────────────────────────────────────────

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
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
    val containerColor = if (isOfficial) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val labelColor = if (isOfficial) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val contentColor = if (isOfficial) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

// ── 来源溯源 ────────────────────────────────────────────────

@Composable
private fun SourcesSection(sources: List<DataSourceEntity>) {
    InfoSection(title = "资料来源（${sources.size}）") {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            sources.forEach { source ->
                SourceRow(source)
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun SourceRow(source: DataSourceEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
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
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        points.forEach { point ->
            TonalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDetail(point.id) },
            ) {
                Text(
                    text = point.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )
            }
        }
    }
}
