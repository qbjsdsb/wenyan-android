package com.wenyan.app.feature.knowledge

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.knowledge.R

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.wenyan.app.core.ai.SocraticGuide
import com.wenyan.app.core.common.util.ExamContentCleaner
import com.wenyan.app.core.ai.SocraticStage
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
 * 1. 题目信息区（分值/试卷代码）— v0.9.23：年份已删除
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
 *
 * 参考链接用 [LocalUriHandler] 打开系统默认浏览器（CustomTabsIntent 需额外依赖，
 * LocalUriHandler 已满足需求且与 Compose 生态一致）。
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
                // v0.9.23：年份已删除（用户需求"论述题不要年份"），subtitle 只显示分值
                subtitle = uiState.essay?.let { e ->
                    if (e.score > 0) "${e.score}分" else null
                },
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
                                    // 1. 题目信息区（v0.9.23：年份已删除）
                                    EssayHeaderSection(
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

                                    // 11. AI 审题助手区（v0.9.9 Phase 3 新增）
                                    // 答题区 + 三阶段引导 + 自评错题回写
                                    EssayAiGuideSection(
                                        uiState = uiState,
                                        onStartAnswering = viewModel::startAnswering,
                                        onCancelAnswering = viewModel::cancelAnswering,
                                        onUpdateUserAnswer = viewModel::updateUserAnswer,
                                        onSubmitAnswer = viewModel::submitAnswerAndGuide,
                                        onRetryAiGuide = viewModel::retryAiGuide,
                                        onClearAiGuides = viewModel::clearAiGuides,
                                        onRateSelf = viewModel::rateSelf,
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

// ── 1. 题目信息区 ──────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EssayHeaderSection(
    score: Int,
    examPaperCode: String?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // v0.9.23：年份 chip 已删除（用户需求"论述题不要年份"）
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
            text = ExamContentCleaner.stripQuestionNumber(content).trim(),
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

// ── 11. AI 审题助手区（v0.9.9 Phase 3） ───────────────────

/**
 * AI 审题助手区（v0.9.9 Phase 3 新增）。
 *
 * 区块结构（按 isAnswering 状态切换）：
 * - 未答题：[EssayAnswerEntry] —— "开始练习"入口按钮
 * - 答题中：[EssayAnswerInputSection]（答题 TextField + 提交/取消）
 *          + [EssayAiGuideStagesSection]（三阶段引导，有结果/加载中/错误时显示）
 *          + [EssaySelfRatingSection]（引导完成后自评，AGAIN 写错题本）
 *
 * 子区块均做优雅降级：
 * - AI 引导为空且无加载/错误 → 不渲染引导区
 * - 自评仅在引导有结果且无加载/错误时显示
 */
@Composable
private fun EssayAiGuideSection(
    uiState: EssayDetailUiState,
    onStartAnswering: () -> Unit,
    onCancelAnswering: () -> Unit,
    onUpdateUserAnswer: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onRetryAiGuide: () -> Unit,
    onClearAiGuides: () -> Unit,
    onRateSelf: (EssaySelfRating) -> Unit,
) {
    GroupedCard(title = "AI 审题助手") {
        Column(
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (!uiState.isAnswering) {
                // 入口：开始练习
                EssayAnswerEntry(onStartAnswering = onStartAnswering)
            } else {
                // 答题区
                EssayAnswerInputSection(
                    userAnswer = uiState.userAnswer,
                    isAiGuiding = uiState.isAiGuiding,
                    onUpdateUserAnswer = onUpdateUserAnswer,
                    onSubmitAnswer = onSubmitAnswer,
                    onCancelAnswering = onCancelAnswering,
                )

                // AI 引导区：有结果 / 加载中 / 错误 时显示
                if (uiState.aiGuides.isNotEmpty() || uiState.isAiGuiding || uiState.aiGuideError != null) {
                    EssayAiGuideStagesSection(
                        guides = uiState.aiGuides,
                        isAiGuiding = uiState.isAiGuiding,
                        error = uiState.aiGuideError,
                        onRetry = onRetryAiGuide,
                        onClear = onClearAiGuides,
                    )
                }

                // 自评区：引导有结果且无加载/错误时显示
                if (uiState.aiGuides.isNotEmpty() && !uiState.isAiGuiding && uiState.aiGuideError == null) {
                    EssaySelfRatingSection(
                        selfRating = uiState.selfRating,
                        lastWrongAnswerId = uiState.lastWrongAnswerId,
                        onRateSelf = onRateSelf,
                    )
                }
            }
        }
    }
}

/**
 * 答题入口（未答题时显示）。
 *
 * 展示"开始练习"按钮 + 简短说明，点击后进入答题模式（[EssayAnswerInputSection]）。
 */
@Composable
private fun EssayAnswerEntry(onStartAnswering: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "提交你的答案，AI 将从论证漏洞、改进建议、参考范文三个阶段引导你完善答题思路。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
            onClick = onStartAnswering,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Start,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "开始练习")
        }
    }
}

/**
 * 答题输入区。
 *
 * - [OutlinedTextField] 多行输入，5000 字上限（[EssayDetailViewModel.MAX_USER_ANSWER_LENGTH]）
 * - 字数计数器（接近上限时变色提醒）
 * - 提交按钮（[Icons.Default.Send]）：空答案或 AI 引导中禁用
 * - 取消按钮（[Icons.Default.Cancel]）：退出答题模式，清空答案与引导
 */
@Composable
private fun EssayAnswerInputSection(
    userAnswer: String,
    isAiGuiding: Boolean,
    onUpdateUserAnswer: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onCancelAnswering: () -> Unit,
) {
    val canSubmit = userAnswer.isNotBlank() && !isAiGuiding
    val charCount = userAnswer.length
    val maxLen = EssayDetailViewModel.MAX_USER_ANSWER_LENGTH
    // 接近上限（90%）时用 error 色提醒
    val countColor = if (charCount >= maxLen * 0.9) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = userAnswer,
            onValueChange = onUpdateUserAnswer,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
            label = { Text(stringResource(R.string.text_01)) },
            placeholder = { Text(stringResource(R.string.text_02)) },
            enabled = !isAiGuiding,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "$charCount / $maxLen",
                        style = MaterialTheme.typography.labelSmall,
                        color = countColor,
                    )
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(
                onClick = onCancelAnswering,
                enabled = !isAiGuiding,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = "取消")
            }
            Button(
                onClick = onSubmitAnswer,
                enabled = canSubmit,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = "提交并引导")
            }
        }
    }
}

/**
 * AI 三阶段引导展示区。
 *
 * 按顺序渲染 [SocraticGuide] 列表，每阶段一个卡片：
 * - [SocraticStage.ANALYZE]：分析论证漏洞
 * - [SocraticStage.SUGGEST]：改进建议
 * - [SocraticStage.SHOW_SAMPLE]：参考范文（标注"范文，非标准答案"）
 *
 * 状态：
 * - [isAiGuiding] → 底部显示 Loading 指示器
 * - [error] != null → 显示错误信息 + 重试按钮
 * - 有结果 → 显示"清空"按钮（保留答案，仅清空引导结果）
 */
@Composable
private fun EssayAiGuideStagesSection(
    guides: List<SocraticGuide>,
    isAiGuiding: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // 各阶段引导内容
        guides.forEach { guide ->
            EssayGuideStageCard(guide = guide)
        }

        // 加载中指示器
        if (isAiGuiding) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WenyanLoadingIndicator()
                Spacer(modifier = Modifier.size(Spacing.sm))
                Text(
                    text = "AI 正在生成引导…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 错误信息 + 重试
        error?.let { msg ->
            TonalCardLow(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "引导失败：$msg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onRetry,
                        enabled = !isAiGuiding,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(text = "重试")
                    }
                }
            }
        }

        // 清空按钮（有结果且非加载中时显示）
        if (guides.isNotEmpty() && !isAiGuiding) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = "清空引导结果")
            }
        }
    }
}

/**
 * 单个引导阶段卡片。
 *
 * 阶段标题映射：
 * - ANALYZE → "① 分析论证漏洞"
 * - SUGGEST → "② 改进建议"
 * - SHOW_SAMPLE → "③ 参考范文"（额外标注"范文，非标准答案"）
 */
@Composable
private fun EssayGuideStageCard(guide: SocraticGuide) {
    val (stageLabel, isSample) = when (guide.stage) {
        SocraticStage.ANALYZE -> "① 分析论证漏洞" to false
        SocraticStage.SUGGEST -> "② 改进建议" to false
        SocraticStage.SHOW_SAMPLE -> "③ 参考范文" to true
    }

    TonalCardLow(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (isSample) {
                Text(
                    text = "范文，非标准答案",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = guide.content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * 自评区（引导完成后显示）。
 *
 * 三档评分（对齐 FSRS Rating，无 HARD）：
 * - [EssaySelfRating.AGAIN]：不会 / 答得不好 → 写入错题本 + FSRS 调度
 * - [EssaySelfRating.GOOD]：尚可
 * - [EssaySelfRating.EASY]：轻松
 *
 * 评分后展示选中状态 + 简短确认。
 * AGAIN 且 [lastWrongAnswerId] 非空 → "已记录到错题本"。
 */
@Composable
private fun EssaySelfRatingSection(
    selfRating: EssaySelfRating?,
    lastWrongAnswerId: String?,
    onRateSelf: (EssaySelfRating) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "自评",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SelfRatingButton(
                label = "不会",
                icon = Icons.Default.SentimentDissatisfied,
                isSelected = selfRating == EssaySelfRating.AGAIN,
                onClick = { onRateSelf(EssaySelfRating.AGAIN) },
                modifier = Modifier.weight(1f),
            )
            SelfRatingButton(
                label = "尚可",
                icon = Icons.Default.SentimentNeutral,
                isSelected = selfRating == EssaySelfRating.GOOD,
                onClick = { onRateSelf(EssaySelfRating.GOOD) },
                modifier = Modifier.weight(1f),
            )
            SelfRatingButton(
                label = "轻松",
                icon = Icons.Default.SentimentVerySatisfied,
                isSelected = selfRating == EssaySelfRating.EASY,
                onClick = { onRateSelf(EssaySelfRating.EASY) },
                modifier = Modifier.weight(1f),
            )
        }

        // 评分后确认信息
        selfRating?.let { rating ->
            val confirmText = when (rating) {
                EssaySelfRating.AGAIN -> {
                    if (lastWrongAnswerId != null) {
                        "已记录到错题本，将按 FSRS 间隔重复调度复习。"
                    } else {
                        "已自评。错题记录未成功，可稍后重试或手动加入错题本。"
                    }
                }
                EssaySelfRating.GOOD -> "已自评：尚可。建议回顾 AI 改进建议加深理解。"
                EssaySelfRating.EASY -> "已自评：轻松。答题思路已掌握。"
            }
            val confirmColor = if (rating == EssaySelfRating.AGAIN && lastWrongAnswerId == null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = confirmColor,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.bodySmall,
                    color = confirmColor,
                )
            }
        }
    }
}

/**
 * 自评档位按钮。
 *
 * 选中时用 [FilledTonalButton]（M3 次级强调），未选中用 [OutlinedButton]。
 */
@Composable
private fun SelfRatingButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSelected) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = label)
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
        androidx.compose.material3.Surface {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                EssayHeaderSection(score = 30, examPaperCode = "604")
                EssayContentSection(content = "试述冰心，丁玲，萧红，张爱玲，王安忆五位女作家创作的异同，并梳理她们在不同时期的创作演变。")
            }
        }
    }
}

/**
 * AI 审题助手入口态 Preview（未答题，显示"开始练习"按钮）。
 */
@Preview(name = "Essay AI Guide - Entry", showBackground = true)
@Composable
private fun EssayAiGuideEntryPreview() {
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        androidx.compose.material3.Surface {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                EssayAiGuideSection(
                    uiState = EssayDetailUiState(isAnswering = false),
                    onStartAnswering = {},
                    onCancelAnswering = {},
                    onUpdateUserAnswer = {},
                    onSubmitAnswer = {},
                    onRetryAiGuide = {},
                    onClearAiGuides = {},
                    onRateSelf = {},
                )
            }
        }
    }
}

/**
 * AI 审题助手三阶段引导完成态 Preview（含自评区）。
 */
@Preview(name = "Essay AI Guide - Stages", showBackground = true)
@Composable
private fun EssayAiGuideStagesPreview() {
    val sampleGuides = listOf(
        SocraticGuide(
            stage = SocraticStage.ANALYZE,
            content = "你的答案涵盖了五位作家，但缺乏纵向演变梳理。建议补充不同时期的创作转向，如丁玲从《莎菲女士的日记》到延安时期的转变。",
            isSampleEssay = false,
            contentSource = "AI_GENERATED",
        ),
        SocraticGuide(
            stage = SocraticStage.SUGGEST,
            content = "可从时代背景、女性意识、叙事风格三个维度比较异同，再按时间线梳理演变脉络。",
            isSampleEssay = false,
            contentSource = "AI_GENERATED",
        ),
        SocraticGuide(
            stage = SocraticStage.SHOW_SAMPLE,
            content = "五位女作家的创作各具特色又相互映照。冰心的'爱的哲学'奠定温情基调，丁玲以女性觉醒切入革命叙事，萧红用底层视角拓展苦难书写，张爱玲在都市语境中解构浪漫，王安忆则在宏大叙事与日常细节间寻找平衡…",
            isSampleEssay = true,
            contentSource = "AI_GENERATED",
        ),
    )
    WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
        androidx.compose.material3.Surface {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                EssayAiGuideSection(
                    uiState = EssayDetailUiState(
                        isAnswering = true,
                        userAnswer = "冰心、丁玲、萧红、张爱玲、王安忆五位女作家各有特色…",
                        aiGuides = sampleGuides,
                    ),
                    onStartAnswering = {},
                    onCancelAnswering = {},
                    onUpdateUserAnswer = {},
                    onSubmitAnswer = {},
                    onRetryAiGuide = {},
                    onClearAiGuides = {},
                    onRateSelf = {},
                )
            }
        }
    }
}
