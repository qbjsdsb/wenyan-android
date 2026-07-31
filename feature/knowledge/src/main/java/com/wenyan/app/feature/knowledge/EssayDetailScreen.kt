package com.wenyan.app.feature.knowledge

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCardLow
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import androidx.compose.ui.tooling.preview.Preview

/**
 * 论述题详情界面（v0.9.8 新增，对应 docs/design/essay-module-design.md）。
 *
 * 10 区块结构：
 * 1. 题目信息区（年份/科目/分值/试卷代码）
 * 2. 题目正文
 * 3. 审题思路区（题型/关键词/任务/突破角度/角度理由）— angle JSON 有数据时显示
 * 4. 论证路径区（总论点/分论点/结论）— angle.argumentPath 有数据时显示
 * 5. 答题框架区（answerFramework）— 有数据时显示
 * 6. 依据区（evidences：作品原文/学者观点/教材共识）— notes JSON 有数据时显示
 * 7. 交叉验证区（教材对比/学者对比）— notes.crossValidation 有数据时显示
 * 8. 参考链接区（referenceLinks：可点击 URL）— notes.referenceLinks 有数据时显示
 * 9. 知识盲点区（knowledgeGaps：项目暂无知识点提醒）— notes.knowledgeGaps 有数据时显示
 * 10. 关联知识点区（relatedPoints：点击跳转知识点详情）— 有数据时显示
 *
 * 优雅降级：angle/notes 为 null（131/134 道未填充）→ 隐藏对应区块，
 * 仅显示题目信息 + 正文 + 答题框架 + 关联知识点。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssayDetailScreen(
    onBack: () -> Unit = {},
    onNavigateToKnowledgeDetail: (String) -> Unit = {},
    viewModel: EssayDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "论述题详情",
                subtitle = uiState.essay?.let { e -> "${e.year}年 · ${e.score}分" },
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.error, uiState.notFound || uiState.essay == null),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "essay_detail_state",
                modifier = Modifier.fillMaxSize(),
            ) { (isLoading, error, isNotFound) ->
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { WenyanLoadingIndicator() }
                    }
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
                    isNotFound -> {
                        EmptyState(
                            icon = Icons.Default.Inbox,
                            title = "论述题不存在",
                            description = "该题目可能已被删除或 ID 错误",
                        )
                    }
                    else -> {
                        uiState.essay?.let { essay ->
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
                                    // 1. 题目信息区
                                    EssayHeaderSection(
                                        year = essay.year,
                                        score = essay.score,
                                        examPaperCode = essay.examPaperCode,
                                    )

                                    // 2. 题目正文
                                    EssayContentSection(content = essay.content)

                                    // 3. 审题思路区
                                    uiState.angle?.let { angle ->
                                        EssayAngleSection(angle = angle)
                                    }

                                    // 4. 论证路径区
                                    uiState.angle?.argumentPath?.let { path ->
                                        EssayArgumentPathSection(path = path)
                                    }

                                    // 5. 答题框架区
                                    essay.answerFramework?.takeIf { it.isNotBlank() }?.let { framework ->
                                        EssayFrameworkSection(framework = framework)
                                    }

                                    // 6. 依据区
                                    uiState.notes?.evidences?.takeIf { it.isNotEmpty() }?.let { evidences ->
                                        EssayEvidencesSection(evidences = evidences)
                                    }

                                    // 7. 交叉验证区
                                    uiState.notes?.crossValidation?.let { cv ->
                                        if (!cv.textbookComparison.isNullOrBlank() || !cv.scholarComparison.isNullOrBlank()) {
                                            EssayCrossValidationSection(crossValidation = cv)
                                        }
                                    }

                                    // 8. 参考链接区
                                    uiState.notes?.referenceLinks?.takeIf { it.isNotEmpty() }?.let { links ->
                                        EssayReferenceLinksSection(links = links)
                                    }

                                    // 9. 知识盲点区
                                    uiState.notes?.knowledgeGaps?.takeIf { it.isNotEmpty() }?.let { gaps ->
                                        EssayKnowledgeGapsSection(gaps = gaps)
                                    }

                                    // 10. 关联知识点区
                                    if (uiState.relatedPoints.isNotEmpty()) {
                                        EssayRelatedPointsSection(
                                            points = uiState.relatedPoints,
                                            onNavigateToKnowledgeDetail = onNavigateToKnowledgeDetail,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 1. 题目信息区 ──────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EssayHeaderSection(
    year: Int,
    score: Int,
    examPaperCode: String?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        WenyanInfoChip(text = "${year}年", variant = ChipVariant.PRIMARY)
        if (score > 0) {
            WenyanInfoChip(text = "${score}分", variant = ChipVariant.SECONDARY)
        }
        examPaperCode?.takeIf { it.isNotBlank() }?.let { code ->
            WenyanInfoChip(text = "卷 $code", variant = ChipVariant.NEUTRAL)
        }
    }
}

// ── 2. 题目正文 ────────────────────────────────────────────

@Composable
private fun EssayContentSection(content: String) {
    GroupedCard(title = "题目") {
        Text(
            text = content.trim(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
        )
    }
}

// ── 3. 审题思路区 ──────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EssayAngleSection(angle: EssayAngle) {
    GroupedCard(title = "审题思路") {
        Column(
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // 题型 + 任务
            angle.questionType?.takeIf { it.isNotBlank() }?.let { qt ->
                LabeledText(label = "题型", content = qt)
            }
            angle.task?.takeIf { it.isNotBlank() }?.let { task ->
                LabeledText(label = "任务", content = task)
            }

            // 核心关键词
            angle.coreKeywords?.takeIf { it.isNotEmpty() }?.let { keywords ->
                Text(
                    text = "核心关键词",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    keywords.forEach { kw ->
                        WenyanInfoChip(text = kw, variant = ChipVariant.PRIMARY)
                    }
                }
            }

            // 限制关键词
            angle.limitKeywords?.takeIf { it.isNotEmpty() }?.let { keywords ->
                Text(
                    text = "限制条件",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    keywords.forEach { kw ->
                        WenyanInfoChip(text = kw, variant = ChipVariant.TERTIARY)
                    }
                }
            }

            // 突破角度
            angle.breakthroughAngles?.takeIf { it.isNotEmpty() }?.let { angles ->
                Text(
                    text = "突破角度",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                angles.forEach { a ->
                    Text(
                        text = a,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                }
            }

            // 角度理由
            angle.angleRationale?.takeIf { it.isNotBlank() }?.let { rationale ->
                TonalCardLow(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "角度选择理由：$rationale",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.md),
                    )
                }
            }
        }
    }
}

// ── 4. 论证路径区 ──────────────────────────────────────────

@Composable
private fun EssayArgumentPathSection(path: EssayArgumentPath) {
    GroupedCard(title = "论证路径（总-分-总）") {
        Column(
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // 总论点
            path.thesis?.takeIf { it.isNotBlank() }?.let { thesis ->
                LabeledText(label = "总论点", content = thesis, isHighlighted = true)
            }

            // 分论点
            path.points?.takeIf { it.isNotEmpty() }?.let { points ->
                Text(
                    text = "分论点",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                points.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        point.label?.takeIf { it.isNotBlank() }?.let { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.widthIn(max = 100.dp),
                            )
                        }
                        point.content?.takeIf { it.isNotBlank() }?.let { content ->
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // 结论
            path.conclusion?.takeIf { it.isNotBlank() }?.let { conclusion ->
                LabeledText(label = "结论", content = conclusion, isHighlighted = true)
            }
        }
    }
}

// ── 5. 答题框架区 ──────────────────────────────────────────

@Composable
private fun EssayFrameworkSection(framework: String) {
    GroupedCard(title = "答题框架") {
        Text(
            text = framework.trim(),
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

// ── 6. 依据区 ──────────────────────────────────────────────

@Composable
private fun EssayEvidencesSection(evidences: List<EssayEvidence>) {
    GroupedCard(title = "依据（${evidences.size}）") {
        evidences.forEachIndexed { index, evidence ->
            EvidenceRow(evidence = evidence)
            if (index < evidences.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

@Composable
private fun EvidenceRow(evidence: EssayEvidence) {
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
        // 类型标签 + 来源
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            evidence.label?.takeIf { it.isNotBlank() }?.let { label ->
                val variant = when (evidence.type) {
                    "WORK_TEXT" -> ChipVariant.PRIMARY
                    "SCHOLAR_OPINION" -> ChipVariant.SECONDARY
                    "TEXTBOOK_CONSENSUS" -> ChipVariant.TERTIARY
                    else -> ChipVariant.NEUTRAL
                }
                WenyanInfoChip(text = label, variant = variant)
            }
        }

        // 依据内容
        evidence.content?.takeIf { it.isNotBlank() }?.let { content ->
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // 来源
        evidence.source?.takeIf { it.isNotBlank() }?.let { source ->
            Text(
                text = "来源：$source",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── 7. 交叉验证区 ──────────────────────────────────────────

@Composable
private fun EssayCrossValidationSection(crossValidation: EssayCrossValidation) {
    GroupedCard(title = "交叉验证") {
        Column(
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            crossValidation.textbookComparison?.takeIf { it.isNotBlank() }?.let { tc ->
                LabeledText(label = "教材对比", content = tc)
            }
            crossValidation.scholarComparison?.takeIf { it.isNotBlank() }?.let { sc ->
                LabeledText(label = "学者对比", content = sc)
            }
        }
    }
}

// ── 8. 参考链接区 ──────────────────────────────────────────

@Composable
private fun EssayReferenceLinksSection(links: List<EssayReferenceLink>) {
    val uriHandler = LocalUriHandler.current

    GroupedCard(title = "参考链接（${links.size}）") {
        links.forEachIndexed { index, link ->
            GroupedCardItem(
                title = link.label.orEmpty().ifBlank { link.url.orEmpty() },
                leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                leadingIconContentDescription = "打开链接",
                onClick = {
                    link.url?.takeIf { it.isNotBlank() }?.let { url ->
                        uriHandler.openUri(url)
                    }
                },
            )
            if (index < links.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

// ── 9. 知识盲点区 ──────────────────────────────────────────

@Composable
private fun EssayKnowledgeGapsSection(gaps: List<EssayKnowledgeGap>) {
    GroupedCard(title = "知识盲点提醒（${gaps.size}）") {
        gaps.forEachIndexed { index, gap ->
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
                gap.author?.takeIf { it.isNotBlank() }?.let { author ->
                    Text(
                        text = author,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                gap.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (index < gaps.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

// ── 10. 关联知识点区 ───────────────────────────────────────

@Composable
private fun EssayRelatedPointsSection(
    points: List<KnowledgePointEntity>,
    onNavigateToKnowledgeDetail: (String) -> Unit,
) {
    GroupedCard(title = "关联知识点（${points.size}）") {
        points.forEachIndexed { index, point ->
            GroupedCardItem(
                title = point.title,
                description = point.summary?.takeIf { it.isNotBlank() },
                onClick = { onNavigateToKnowledgeDetail(point.id) },
            )
            if (index < points.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

// ── 通用组件 ───────────────────────────────────────────────

@Composable
private fun LabeledText(
    label: String,
    content: String,
    isHighlighted: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Previews ───────────────────────────────────────────────

@Preview(name = "Essay - Light", showBackground = true)
@Composable
private fun EssayDetailLightPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                EssayHeaderSection(year = 2008, score = 30, examPaperCode = "604")
                EssayContentSection(content = "试述冰心，丁玲，萧红，张爱玲，王安忆五位女作家创作的异同，并梳理她们在不同时期的创作演变。")
            }
        }
    }
}

@Composable
private fun Surface(content: @Composable () -> Unit) {
    androidx.compose.material3.Surface { content() }
}
