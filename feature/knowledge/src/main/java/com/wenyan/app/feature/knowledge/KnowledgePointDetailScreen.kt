package com.wenyan.app.feature.knowledge

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.designsystem.component.ContentSourceBadge
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.common.util.ExamContentCleaner
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCardLow
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity

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
    onNavigateToEssay: (String) -> Unit = {},
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
        // NF-UC1 修复：scrollState 移出 Crossfade lambda，避免状态切换（loading→content）
        // 时 Composable 重建导致 scrollState 丢失，滚动位置归零。
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.error, uiState.notFound || uiState.point == null),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "knowledge_detail_state",
                modifier = Modifier.fillMaxSize(),
            ) { (isLoading, error, isNotFound) ->
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            WenyanLoadingIndicator()
                        }
                    }
                    error != null -> {
                        // v0.8.3 修复：原代码未处理 error 状态，异常时误显示"知识点不存在"
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
                    isNotFound -> {
                        // v0.8.3 优化：用 EmptyState 组件替代裸 Text，与全 App 一致
                        EmptyState(
                            icon = Icons.Default.Inbox,
                            title = "知识点不存在",
                            description = "该知识点可能已被删除或 ID 错误",
                        )
                    }
                    else -> {
                        uiState.point?.let { point ->
                            // v0.8.15 Stage 1: 横屏/平板下限制内容最大宽度并居中，避免教材原文/解析行宽过宽阅读疲劳。
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = MaxContentWidth.comfortable)
                                        .verticalScroll(scrollState)
                                        .padding(Spacing.lg),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                                ) {
                                // ── 标题区 ──
                                HeaderSection(point)

                                // ── 摘要 ──
                                point.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                                    GroupedCard(title = "摘要") {
                                        Text(
                                            text = summary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(
                                                start = Spacing.lg,
                                                end = Spacing.lg,
                                                top = Spacing.md,
                                                bottom = Spacing.md,
                                            ),
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

                                // ── 相关论述题(v0.9.8 知识点串联器) ──
                                RelatedEssaysSection(
                                    essays = uiState.relatedEssays,
                                    onNavigateToEssay = onNavigateToEssay,
                                )

                                // ── 错题记录(v0.8.19 P1-REL-1) ──
                                WrongAnswersSection(
                                    wrongAnswers = uiState.wrongAnswers,
                                    onMarkResolved = viewModel::markWrongAnswerResolved,
                                )
                                } // Column end
                            } // Box end
                        }
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
            // P2-2 修复：Bold(700) 过重，M3 Expressive 推荐 SemiBold(600)
            fontWeight = FontWeight.SemiBold,
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

/**
 * 无容器的标题区块（仅用于内部有容器的场景，避免嵌套卡片）。
 *
 * 当前仅 MultiPerspectiveSection 使用——其内部 PerspectiveCard 已有 Surface/TonalCardLow 容器，
 * 若再套 GroupedCard 的 TonalCard 会导致 AMOLED 模式下色调层级反转
 *（surfaceBright 未被 AMOLED 覆盖为 Black，而 surfaceContainerLow 被覆盖）。
 */
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
                    content = point.studyText.orEmpty(),
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
    if (isOfficial) {
        // 答题基准（马工程）：用 primaryContainer 突出官方权威性
        // designsystem 的 TonalCard/TonalCardLow 无 primaryContainer 变体，此处保留自定义 Surface
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    } else {
        // 学习理解/多视角：用 TonalCardLow（surfaceContainerLow + shapes.medium）
        TonalCardLow(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ── 来源溯源 ────────────────────────────────────────────────

@Composable
private fun SourcesSection(sources: List<DataSourceEntity>) {
    GroupedCard(title = "资料来源（${sources.size}）") {
        sources.forEachIndexed { index, source ->
            SourceRow(source)
            if (index < sources.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

@Composable
private fun SourceRow(source: DataSourceEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.md),
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

/**
 * 关联知识点的关系类型（ADR-001 B2.1 视觉编码）。
 *
 * 三种关系类型用 icon + chipVariant 双重编码，色盲友好：
 * - [RELATED] 关联：Link 图标 + PRIMARY 色（与 title 色一致，表示强关联）
 * - [CONTRAST] 对比：CompareArrows 图标 + SECONDARY 色（双向箭头表对比）
 * - [EXTENSION] 延伸：NorthEast 图标 + TERTIARY 色（右上箭头表延伸方向）
 *
 * 色彩选择遵循 M3 Expressive "tonal layering"：primary/secondary/tertiary
 * 是 M3 colorScheme 的三个强调色族，互不重叠，视觉区分度高。
 */
private enum class RelationshipType(
    val label: String,
    val icon: ImageVector,
    val chipVariant: ChipVariant,
) {
    RELATED("关联", Icons.Filled.Link, ChipVariant.PRIMARY),
    CONTRAST("对比", Icons.AutoMirrored.Filled.CompareArrows, ChipVariant.SECONDARY),
    EXTENSION("延伸", Icons.Filled.NorthEast, ChipVariant.TERTIARY),
}

/**
 * 关联知识点区块的容器颜色（按关系类型取 M3 container 色）。
 *
 * 用于 [RelatedGroup] 标题栏图标的 tint，使三种关系类型在标题栏就有视觉区分。
 * 不用 [MaterialTheme.colorScheme.primary] 统一色，避免三种关系看起来相同。
 */
@Composable
private fun RelationshipType.iconTint() = when (chipVariant) {
    ChipVariant.PRIMARY -> MaterialTheme.colorScheme.primary
    ChipVariant.SECONDARY -> MaterialTheme.colorScheme.secondary
    ChipVariant.TERTIARY -> MaterialTheme.colorScheme.tertiary
    ChipVariant.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    ChipVariant.ERROR -> MaterialTheme.colorScheme.error
}

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

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        if (hasRelated) {
            RelatedGroup(
                type = RelationshipType.RELATED,
                points = detail.relatedPoints,
                onNavigateToDetail = onNavigateToDetail,
            )
        }
        if (hasContrast) {
            RelatedGroup(
                type = RelationshipType.CONTRAST,
                points = detail.contrastPoints,
                onNavigateToDetail = onNavigateToDetail,
            )
        }
        if (hasExtension) {
            RelatedGroup(
                type = RelationshipType.EXTENSION,
                points = detail.extensionPoints,
                onNavigateToDetail = onNavigateToDetail,
            )
        }
    }
}

/**
 * 关联知识点分组卡片（B2.1 视觉编码 + B2.2 信息密度增强）。
 *
 * 标题栏：关系类型图标 + 关系名 + 数量 chip（如"3 个"），用 [type.chipVariant] 着色
 * 列表项：知识点标题 + 摘要预览 + 考频 chip + 难度 chip（[RelatedPointItem]）
 *
 * 视觉编码层级：
 * 1. 标题栏 icon + chipVariant 色 → 一眼区分三种关系类型
 * 2. 列表项 trailing chip → 快速判断知识点重要度（考频）和难度
 * 3. 列表项 description → 摘要预览，无需点进去即可粗略判断是否需要复习
 */
@Composable
private fun RelatedGroup(
    type: RelationshipType,
    points: List<KnowledgePointEntity>,
    onNavigateToDetail: (String) -> Unit,
) {
    GroupedCard(
        title = type.label,
        titleTrailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null, // 装饰性，title 已读"关联/对比/延伸"
                    tint = type.iconTint(),
                    modifier = Modifier.size(18.dp),
                )
                WenyanInfoChip(
                    text = "${points.size} 个",
                    variant = type.chipVariant,
                )
            }
        },
    ) {
        points.forEachIndexed { index, point ->
            RelatedPointItem(
                point = point,
                onClick = { onNavigateToDetail(point.id) },
            )
            if (index < points.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

/**
 * 关联知识点列表项（B2.2 信息密度增强）。
 *
 * 基于 [GroupedCardItem] 复用其成熟的无障碍支持（48dp 触控目标 + mergeDescendants），
 * 增强：
 * - [description]：摘要预览（截断到 [RELATED_SUMMARY_PREVIEW_LENGTH] 字符 + …），无摘要时不显示
 * - [trailing]：考频 chip（仅非 NEVER 时）+ 难度 chip，用 Row 横向排列
 *
 * 不用 [GroupedCardItem.leadingIcon]：关系类型视觉编码已在 [RelatedGroup] 标题栏，
 * 列表项保持简洁，避免每行都重复关系类型图标造成视觉噪音。
 *
 * @param point 关联知识点
 * @param onClick 点击跳转到该知识点详情
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedPointItem(
    point: KnowledgePointEntity,
    onClick: () -> Unit,
) {
    GroupedCardItem(
        title = point.title,
        description = point.summary
            ?.takeIf { it.isNotBlank() }
            ?.let { summary ->
                if (summary.length > RELATED_SUMMARY_PREVIEW_LENGTH) {
                    summary.take(RELATED_SUMMARY_PREVIEW_LENGTH) + "…"
                } else {
                    summary
                }
            },
        onClick = onClick,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 考频 chip：仅非 NEVER 时显示（NEVER 语义为"未考"，不展示避免噪音）
                if (point.examFrequency != "NEVER") {
                    val (freqLabel, freqVariant) = examFrequencyChip(point.examFrequency)
                    WenyanInfoChip(text = freqLabel, variant = freqVariant)
                }
                // 难度 chip：始终显示（1-5 是知识点固有属性）
                WenyanInfoChip(
                    text = "${point.difficulty}/5",
                    variant = ChipVariant.NEUTRAL,
                )
            }
        },
    )
}

/**
 * 关联知识点摘要预览的最大字符数（B2.2）。
 *
 * 80 字符：覆盖大多数知识点摘要的前 1-2 句，足够用户判断是否需要跳转。
 * 超长截断 + …，避免单条关联项撑高卡片导致详情页无限滚动。
 */
private const val RELATED_SUMMARY_PREVIEW_LENGTH = 80

/**
 * 考频文本 + chip variant 映射（与 [HeaderSection] 一致，确保全 App 考频编码统一）。
 *
 * - HIGH 高频 → PRIMARY（红色系强调，提醒重点复习）
 * - MEDIUM 中频 → SECONDARY
 * - LOW 低频 → TERTIARY
 * - 其他 → NEUTRAL
 */
private fun examFrequencyChip(examFrequency: String): Pair<String, ChipVariant> = when (examFrequency) {
    "HIGH" -> "高频" to ChipVariant.PRIMARY
    "MEDIUM" -> "中频" to ChipVariant.SECONDARY
    "LOW" -> "低频" to ChipVariant.TERTIARY
    else -> "未考" to ChipVariant.NEUTRAL
}

// ── 相关论述题（v0.9.8 知识点串联器） ──────────────────────

/**
 * 相关论述题区块（v0.9.8 新增，论述题板块核心入口）。
 *
 * 设计目标（对应 docs/design/essay-module-design.md 1.2 节"知识点串联器"）：
 * 把 910 个孤立知识点通过 134 道论述题串联成答题网络。
 * 用户在知识点详情页看到"这道题考过这个知识点"，点击进入论述题详情页
 * 查看审题思路 + 答题框架 + 依据 + 交叉验证链接。
 *
 * UI 结构（与 [SourcesSection] / [WrongAnswersSection] 一致的 GroupedCard 模式）：
 * - 标题："相关论述题（N）"+ 论述题图标
 * - 列表项：年份 chip + 分值 chip + 题目正文预览（截断 2 行）
 * - 点击列表项 → 跳转论述题详情页（[onNavigateToEssay]）
 *
 * 无关联论述题时不显示该区块（与 [WrongAnswersSection] 一致的空数据降级策略）。
 *
 * @param essays 关联论述题列表（按年份倒序，来自 [KnowledgePointDetailViewModel.uiState.relatedEssays]）
 * @param onNavigateToEssay 点击论述题跳转详情页，参数为 examQuestionId
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedEssaysSection(
    essays: List<ExamQuestionEntity>,
    onNavigateToEssay: (String) -> Unit,
) {
    if (essays.isEmpty()) return

    GroupedCard(title = "相关论述题（${essays.size}）") {
        essays.forEachIndexed { index, essay ->
            EssayItem(
                essay = essay,
                onClick = { onNavigateToEssay(essay.id) },
            )
            if (index < essays.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

/**
 * 论述题列表项。
 *
 * 信息层级（从上到下）：
 * 1. 年份 chip + 分值 chip（横向 FlowRow，快速定位考题年份和分值权重）
 * 2. 题目正文预览（maxLines=2 + Ellipsis，截断超长题目避免撑高卡片）
 *
 * 不展示关联知识点数量：该信息在论述题详情页"关联知识点"区块展示，
 * 列表项保持简洁，与 [RelatedPointItem] 的信息密度策略一致。
 *
 * @param essay 论述题实体
 * @param onClick 点击跳转论述题详情页
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EssayItem(
    essay: ExamQuestionEntity,
    onClick: () -> Unit,
) {
    GroupedCardItem(
        title = ExamContentCleaner.stripQuestionNumber(essay.content).trim().take(MAX_ESSAY_PREVIEW_LENGTH),
        onClick = onClick,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WenyanInfoChip(
                    text = "${essay.year}年",
                    variant = ChipVariant.SECONDARY,
                )
                if (essay.score > 0) {
                    WenyanInfoChip(
                        text = "${essay.score}分",
                        variant = ChipVariant.NEUTRAL,
                    )
                }
            }
        },
    )
}

/**
 * 论述题正文预览的最大字符数。
 *
 * 120 字符：覆盖大多数论述题的前 2-3 句（含审题关键词），
 * 足够用户判断是否需要跳转查看完整审题思路和答题框架。
 * 超长截断，避免单条论述题撑高卡片（部分论述题正文 300+ 字）。
 */
private const val MAX_ESSAY_PREVIEW_LENGTH = 120

// ── 错题记录(v0.8.19 P1-REL-1 新增) ───────────────────────

/**
 * 错题记录区块。
 *
 * 展示该知识点的未解决错题,让用户在详情页直接看到"这题我错过几次",
 * 无需跳转到错题本。每条错题展示:
 * - 来源(卡片复习 / 真题练习)
 * - 错答次数(wrongCount)
 * - 最后答错时间(相对时间,如"3小时前"/"昨天")
 * - 用户错误答案
 * - 正确答案(若有)
 * - "标记已解决"按钮(用户确认已掌握后移除该错题)
 *
 * 无错题时不显示该区块(避免空区块干扰阅读)。
 *
 * v0.8.19 P1-REL-2 修复:原注释写"可折叠"但未实现,移除误导性注释;
 * 新增"最后答错时间"展示(原注释提及但代码漏实现),与 settings 模块
 * formatRelativeTime 一致的相对时间格式。
 *
 * @param wrongAnswers 未解决错题列表(按 lastWrongAt DESC)
 * @param onMarkResolved 标记错题为已解决的回调,参数为错题 ID
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrongAnswersSection(
    wrongAnswers: List<WrongAnswerEntity>,
    onMarkResolved: (String) -> Unit,
) {
    if (wrongAnswers.isEmpty()) return

    GroupedCard(title = "错题记录（${wrongAnswers.size}）") {
        wrongAnswers.forEachIndexed { index, wrong ->
            WrongAnswerRow(
                wrong = wrong,
                onMarkResolved = { onMarkResolved(wrong.id) },
            )
            if (index < wrongAnswers.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrongAnswerRow(
    wrong: WrongAnswerEntity,
    onMarkResolved: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // 第一行:来源标签 + 错答次数 + 最后答错时间
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            // v0.8.20 P2-3 修复:合并三 chip 为单一语义节点,TalkBack 一次性朗读
            // "来源 卡片复习,错答 3 次,3 小时前"而非逐个聚焦
            modifier = Modifier.semantics(mergeDescendants = true) {},
        ) {
            // v0.8.20 P1-7 修复:原硬编码 "CARD_AGAIN"/"QUIZ_WRONG" 字符串,
            // 改用 WrongAnswerRepository 常量,与 WrongAnswerScreen.formatSource 一致
            // v0.8.17 修复 M5:unknown source 不直接暴露英文原始字符串,
            // fallback 到中文"未知来源",与 friendlyErrorMessage 友好提示原则一致
            val (sourceLabel, sourceVariant) = when (wrong.source) {
                WrongAnswerRepository.SOURCE_CARD_AGAIN -> "卡片复习" to ChipVariant.SECONDARY
                WrongAnswerRepository.SOURCE_QUIZ_WRONG -> "真题练习" to ChipVariant.TERTIARY
                WrongAnswerRepository.SOURCE_ESSAY_PRACTICE -> "论述题自评" to ChipVariant.PRIMARY
                else -> "未知来源" to ChipVariant.NEUTRAL
            }
            WenyanInfoChip(text = sourceLabel, variant = sourceVariant)
            // v0.8.20 P2-7 修复:wrongCount <= 0 时不展示(语义不合理,数据异常时不误导用户)
            if (wrong.wrongCount > 0) {
                WenyanInfoChip(
                    text = "错答 ${wrong.wrongCount} 次",
                    variant = ChipVariant.NEUTRAL,
                )
            }
            // v0.8.19 P1-REL-2: 最后答错时间(相对时间),让用户感知"这题多久前错过"
            WenyanInfoChip(
                text = formatRelativeTime(wrong.lastWrongAt),
                variant = ChipVariant.NEUTRAL,
            )
        }

        // 第二行:用户错误答案(v0.8.20 P1-4 对齐:截断超长答案,避免撑高卡片)
        Text(
            text = "你的答案：${wrong.userAnswer.take(MAX_WRONG_ANSWER_PREVIEW)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        // 第三行:正确答案(若有)
        wrong.correctAnswer?.takeIf { it.isNotBlank() }?.let { correct ->
            Text(
                text = "正确答案：${correct.take(MAX_WRONG_ANSWER_PREVIEW)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 第四行:标记已解决按钮
        FilledTonalButton(
            onClick = onMarkResolved,
            modifier = Modifier.padding(top = Spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.sm),
            )
            Text("标记已解决")
        }
    }
}

/**
 * 错题答案预览的最大字符数(v0.8.20 P1-4 新增)。
 *
 * 用户答案 / 正确答案可能很长(如真题主观题答案 500+ 字),
 * 在详情页错题卡片中截断为 [MAX_WRONG_ANSWER_PREVIEW] 字符 + maxLines=3 + Ellipsis,
 * 避免单条错题撑高卡片导致详情页无限滚动。
 *
 * 用户需查看完整答案可跳转到错题本(后续错题本支持展开/折叠)。
 *
 * 200 字符:覆盖大多数简答题答案的前 1-2 段,足够用户判断错因。
 */
private const val MAX_WRONG_ANSWER_PREVIEW = 200

/**
 * 将时间戳格式化为相对时间文本(如"3小时前"/"昨天"/"3天前")。
 *
 * v0.8.19 P1-REL-2 新增:供 [WrongAnswerRow] 展示"最后答错时间"。
 * 实现与 settings 模块的 formatRelativeTime 一致(未抽到 common 模块,
 * 避免为单函数引入跨模块依赖;后续若有第三处使用再抽取)。
 *
 * v0.8.20 P1-4 修复:处理未来时间戳(时钟回拨或异常数据),
 * 负 diffMillis 直接返回"刚刚",避免显示负数。
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp
    // v0.8.20 P1-4 修复:未来时间戳(时钟回拨或异常数据)直接返回"刚刚",
    // 避免下面计算结果为负数,显示"-3 分钟前"等异常文案
    if (diffMillis < 0) return "刚刚"
    val diffMinutes = diffMillis / (60 * 1000)
    val diffHours = diffMillis / (60 * 60 * 1000)
    val diffDays = diffMillis / (24 * 60 * 60 * 1000)
    return when {
        diffMinutes < 1 -> "刚刚"
        diffMinutes < 60 -> "${diffMinutes}分钟前"
        diffHours < 24 -> "${diffHours}小时前"
        diffDays == 1L -> "昨天"
        diffDays < 30 -> "${diffDays}天前"
        else -> "${diffDays / 30}个月前"
    }
}

// ── Previews（B2.3 关联知识点视觉编码预览）─────────────────

/**
 * Preview 用的样本知识点（B2.3）。
 *
 * 覆盖四种考频 × 三种难度组合，验证：
 * - HIGH/MEDIUM/LOW 显示对应 chip（PRIMARY/SECONDARY/TERTIARY）
 * - NEVER 不显示考频 chip（仅显示难度 chip）
 * - 摘要预览截断（80 字符 + …）
 */
private fun sampleRelatedPoint(
    id: String,
    title: String,
    summary: String?,
    examFrequency: String,
    difficulty: Int,
): KnowledgePointEntity = KnowledgePointEntity(
    id = id,
    chapterId = "chapter_sample",
    title = title,
    summary = summary,
    coreConclusion = "",
    fullContent = "",
    multiPerspectives = null,
    relatedIds = null,
    contrastIds = null,
    extensionIds = null,
    examRecords = null,
    examFrequency = examFrequency,
    termTemplate = null,
    tags = null,
    difficulty = difficulty,
    createdAt = 0L,
    updatedAt = 0L,
    contentSource = "TEXTBOOK_NATIVE",
    ocrStatus = "VERIFIED",
    sourceFile = null,
    sourcePage = null,
    studyText = null,
)

/** 长摘要样本（> 80 字符，验证截断） */
private const val LONG_SUMMARY =
    "中国现代文学史上第一篇白话短篇小说，发表于 1918 年《新青年》。作品借" +
        "狂人之口揭示封建礼教吃人的本质，开创了中国现代小说的先河，对后世文学影响深远。"

private val sampleRelatedPoints = listOf(
    sampleRelatedPoint(
        id = "kp_link_1",
        title = "鲁迅《狂人日记》",
        summary = LONG_SUMMARY,
        examFrequency = "HIGH",
        difficulty = 4,
    ),
    sampleRelatedPoint(
        id = "kp_link_2",
        title = "《呐喊》自序",
        summary = "鲁迅第一部小说集《呐喊》的序言，阐述文学救国的创作动机。",
        examFrequency = "MEDIUM",
        difficulty = 3,
    ),
    sampleRelatedPoint(
        id = "kp_link_3",
        title = "新文化运动",
        summary = null, // 验证无摘要时不显示 description
        examFrequency = "NEVER",
        difficulty = 2,
    ),
)

private val sampleContrastPoints = listOf(
    sampleRelatedPoint(
        id = "kp_contrast_1",
        title = "胡适《文学改良刍议》",
        summary = "1917 年发表于《新青年》，提出文学改良八事，主张白话文。",
        examFrequency = "HIGH",
        difficulty = 3,
    ),
    sampleRelatedPoint(
        id = "kp_contrast_2",
        title = "陈独秀《文学革命论》",
        summary = "1917 年发表，提出三大主义，比胡适更激进地主张文学革命。",
        examFrequency = "LOW",
        difficulty = 4,
    ),
)

private val sampleExtensionPoints = listOf(
    sampleRelatedPoint(
        id = "kp_ext_1",
        title = "五四文学思潮",
        summary = "1919 年五四运动前后的文学思潮，包括浪漫主义、现实主义等流派。",
        examFrequency = "MEDIUM",
        difficulty = 5,
    ),
)

/**
 * 三种关系类型同时展示的 Preview 内容（供 Light/Dark/AMOLED 复用）。
 */
@Composable
private fun RelatedPointsPreviewContent() {
    Surface {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            RelatedGroup(
                type = RelationshipType.RELATED,
                points = sampleRelatedPoints,
                onNavigateToDetail = {},
            )
            RelatedGroup(
                type = RelationshipType.CONTRAST,
                points = sampleContrastPoints,
                onNavigateToDetail = {},
            )
            RelatedGroup(
                type = RelationshipType.EXTENSION,
                points = sampleExtensionPoints,
                onNavigateToDetail = {},
            )
        }
    }
}

@Preview(name = "Related - Light", showBackground = true)
@Composable
private fun RelatedPointsLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        RelatedPointsPreviewContent()
    }
}

@Preview(name = "Related - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RelatedPointsDarkPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, dynamicColor = false)) {
        RelatedPointsPreviewContent()
    }
}

@Preview(name = "Related - AMOLED", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RelatedPointsAmoledPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.DARK, amoledMode = true, dynamicColor = false)) {
        RelatedPointsPreviewContent()
    }
}
