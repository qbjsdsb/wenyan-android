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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.designsystem.component.ContentSourceBadge
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCardLow
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.database.entity.DataSourceEntity
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

                                // ── 错题记录(v0.8.19 P1-REL-1) ──
                                WrongAnswersSection(
                                    wrongAnswers = uiState.wrongAnswers,
                                    onMarkResolved = viewModel::markWrongAnswerResolved,
                                )
                            }
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

@Composable
private fun RelatedGroup(
    title: String,
    points: List<KnowledgePointEntity>,
    onNavigateToDetail: (String) -> Unit,
) {
    GroupedCard(title = title) {
        points.forEachIndexed { index, point ->
            GroupedCardItem(
                title = point.title,
                onClick = { onNavigateToDetail(point.id) },
            )
            if (index < points.size - 1) {
                GroupedCardDivider()
            }
        }
    }
}

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
            val (sourceLabel, sourceVariant) = when (wrong.source) {
                WrongAnswerRepository.SOURCE_CARD_AGAIN -> "卡片复习" to ChipVariant.SECONDARY
                WrongAnswerRepository.SOURCE_QUIZ_WRONG -> "真题练习" to ChipVariant.TERTIARY
                else -> wrong.source to ChipVariant.NEUTRAL
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
