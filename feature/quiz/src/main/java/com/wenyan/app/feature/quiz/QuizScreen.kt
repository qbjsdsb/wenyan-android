package com.wenyan.app.feature.quiz

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.quiz.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.wenyan.app.core.common.util.ExamContentCleaner
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import com.wenyan.app.core.designsystem.component.LocalLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SmartToy
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.common.model.ContentSource
import com.wenyan.app.core.designsystem.component.ContentSourceBadge
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar

/**
 * 真题练习界面（阶段5增强）。
 *
 * 增强点（Spec C1.27 / C5.8-C5.13a / Task 26）：
 * - 完整题目正文展示（非截断）
 * - 科目代码历史变动适配（610/801 语义翻转，通过 SubjectResolution 判定）
 * - 答案状态标注（HAS_ANSWER/NO_ANSWER/AI_GENERATED，使用 ContentSourceBadge）
 * - 折叠展开答题框架
 * - 关联知识点入口（跳转知识点详情）
 * - AI助手入口（跳转AI助手，苏格拉底式引导）
 *
 * v0.9.0 变更：移除 TopBar 错题本入口（Inbox 图标），
 * 错题本已提升为顶级 Tab，由底部 NavigationBar 直接切换。
 *
 * 布局：年份选择（LazyRow）+ 题目列表（LazyColumn）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedIds by viewModel.expandedQuestionIds.collectAsStateWithLifecycle()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    // v0.8.21 修复 M3:订阅 errorMessage,selfEvaluate 错题记录失败时通过 Snackbar 反馈
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    // v0.8.21 修复 M3:errorMessage 非 null 时弹 Snackbar,展示后立即 clearError 避免重组重复弹
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "真题练习",
                actions = {
                    // v0.9.0：错题本入口已移除（提升为顶级 Tab，由底部 NavigationBar 切换）
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .imePadding(),
        ) {
            // 年份选择行
            YearSelector(
                years = uiState.availableYears,
                selectedYear = uiState.selectedYear,
                onYearSelected = viewModel::selectYear,
            )

            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.error, uiState.questions.isEmpty()),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "quiz_state",
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
                            title = if (uiState.selectedYear == null) {
                                "请选择年份查看真题"
                            } else {
                                "该年份暂无真题数据"
                            },
                        )
                    }
                    else -> {
                        QuestionList(
                            questions = uiState.questions,
                            expandedIds = expandedIds,
                            answers = answers,
                            onToggleExpanded = viewModel::toggleExpanded,
                            onUpdateAnswer = viewModel::updateAnswer,
                            onSubmitAnswer = viewModel::submitAnswer,
                            onSelfEvaluate = viewModel::selfEvaluate,
                            onNavigateToAiAssistant = onNavigateToAiAssistant,
                            onNavigateToDetail = onNavigateToDetail,
                            contentPadding = PaddingValues(Spacing.lg),
                        )
                    }
                }
            }
        }
    }
}

// 年份选择行
@Composable
private fun YearSelector(
    years: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // P2-LAZY-1 修正：LazyRow items 加 key，避免列表重组时 item 丢失状态/动画异常
        // NF-UP4 修正：加 contentType 让 LazyRow 复用同一类型 item 的 slot，提升滚动性能
        items(items = years, key = { it }, contentType = { "year" }) { year ->
            FilterChip(
                selected = selectedYear == year,
                onClick = { onYearSelected(year) },
                label = { Text("${year}年") },
                leadingIcon = if (selectedYear == year) {
                    // v0.8.3 修复：原用 Text("✓") Unicode 字符，改为 Material Icon 保持视觉一致
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else {
                    null
                },
            )
        }
    }
}

// 题目列表
@Composable
private fun QuestionList(
    questions: List<QuizQuestionItem>,
    expandedIds: Set<String>,
    answers: Map<String, QuizAnswerState>,
    onToggleExpanded: (String) -> Unit,
    onUpdateAnswer: (String, String) -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onSelfEvaluate: (String, Boolean) -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
) {
    // v0.8.15 Stage 1: 横屏/平板下限制内容最大宽度并居中，避免题目卡片行宽过宽阅读疲劳。
    // 竖屏（<720dp）下 widthIn(max=720) 不生效（屏幕宽 < max），不影响竖屏布局。
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        CompositionLocalProvider(LocalLazyListState provides listState) {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = MaxContentWidth.comfortable),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items = questions, key = { it.id }, contentType = { "question" }) { question ->
                    QuestionCard(
                        question = question,
                        isExpanded = question.id in expandedIds,
                        answerState = answers[question.id] ?: QuizAnswerState(),
                        onToggleExpanded = { onToggleExpanded(question.id) },
                        onUpdateAnswer = { text -> onUpdateAnswer(question.id, text) },
                        onSubmitAnswer = { onSubmitAnswer(question.id) },
                        onSelfEvaluate = { isCorrect -> onSelfEvaluate(question.id, isCorrect) },
                        onNavigateToAiAssistant = onNavigateToAiAssistant,
                        onNavigateToDetail = onNavigateToDetail,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/**
 * 单个题目卡片（阶段5增强）。
 *
 * 结构：
 * 1. 顶部信息行：年份 + 科目显示名称（含试卷代码与年份标注）
 * 2. 标签行：题型 + 分值 + 答案状态（ContentSourceBadge）
 * 3. 科目警告（如有，红色提示"年份待核实"）
 * 4. 题目正文（完整展示）
 * 5. 材料题原文（如有）
 * 6. 考查角度（如有）
 * 7. 答题区（折叠/展开）：
 *    - HAS_ANSWER → 展示答题框架
 *    - NO_ANSWER → 提示"暂无参考答案，可使用AI助手辅助分析"
 *    - AI_GENERATED → 展示AI生成的答题框架
 * 8. 底部操作行：关联知识点入口 + AI助手入口
 */
@Composable
private fun QuestionCard(
    question: QuizQuestionItem,
    isExpanded: Boolean,
    answerState: QuizAnswerState,
    onToggleExpanded: () -> Unit,
    onUpdateAnswer: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onSelfEvaluate: (Boolean) -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // 1. 顶部信息行：年份 + 科目显示名称
            Text(
                text = "${question.year}年 · ${question.subjectDisplayName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            // 2. 标签行：题型 + 分值 + 答案状态
            Row(
                modifier = Modifier.padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WenyanInfoChip(text = formatQuestionType(question.questionType), variant = ChipVariant.SECONDARY)
                WenyanInfoChip(text = "${question.score}分", variant = ChipVariant.SECONDARY)
                ContentSourceBadge(
                    contentSource = mapAnswerStatus(question.answerStatus),
                )
            }

            // 3. 科目警告（如有）
            question.subjectWarning?.let { warning ->
                Text(
                    text = warning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            // 4. 题目正文（完整展示，剥离题号前缀）
            Text(
                text = ExamContentCleaner.stripQuestionNumber(question.content),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = Spacing.sm),
            )

            // 5. 材料题原文（如有）
            question.materialText?.takeIf { it.isNotBlank() }?.let { material ->
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                Text(
                    text = "材料：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = material,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            // 6. 考查角度（如有）
            question.angle?.takeIf { it.isNotBlank() }?.let { angle ->
                Text(
                    text = "考查角度：$angle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            // 7. 答题区（NF-PP5: 答题输入 + 提交 + 自评 + 折叠/展开参考答案）
            AnswerSection(
                question = question,
                isExpanded = isExpanded,
                answerState = answerState,
                onToggleExpanded = onToggleExpanded,
                onUpdateAnswer = onUpdateAnswer,
                onSubmitAnswer = onSubmitAnswer,
                onSelfEvaluate = onSelfEvaluate,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            // 8. 底部操作行：关联知识点入口 + AI助手入口
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 关联知识点入口（如有）
                val relatedCount = question.relatedPointIds?.size ?: 0
                if (relatedCount > 0) {
                    OutlinedButton(
                        onClick = {
                            question.relatedPointIds?.firstOrNull()?.let(onNavigateToDetail)
                        },
                    ) {
                        Icon(
                            // P2-1 修复：Icons.Default.MenuBook 已 deprecated，改用 AutoMirrored 版本
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.padding(end = Spacing.xs),
                        )
                        Text("关联知识点 ($relatedCount)")
                    }
                }

                // AI助手入口
                TextButton(onClick = onNavigateToAiAssistant) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                    Text(stringResource(R.string.text_01))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = Spacing.xs),
                    )
                }
            }
        }
    }
}

/**
 * 答题区组件（NF-PP5 Wave 3.2 增强：答题输入 + 提交 + 自评 + 参考答案展示）。
 *
 * 三层状态机（由 [answerState] 驱动）：
 * 1. **未提交**([QuizAnswerState.isSubmitted] = false):
 *    - OutlinedTextField 答题输入框
 *    - "提交答案" Button（空白时禁用）
 * 2. **已提交未自评**([isSubmitted] = true, [isSelfEvaluated] = false):
 *    - 展示用户答案(锁定不可编辑)
 *    - 展示参考答案(答题框架)
 *    - "答对了" / "答错了" 两个 FilledTonalButton
 * 3. **已自评**([isSelfEvaluated] = true):
 *    - 展示用户答案 + 参考答案
 *    - 对错反馈(绿色"答对了" / 红色"答错了")
 *
 * 无参考答案(answerFramework 为空)时(P0 v0.7.2 修复):
 * - 显示非阻断提示,用户仍可输入答案并自评(原实现直接 return 导致 481 题无法答题)
 * - 展开参考答案区显示"暂无参考答案,请根据自身理解自评"
 * - 自评文案调整为"请根据你的理解自评"
 */
@Composable
private fun AnswerSection(
    question: QuizQuestionItem,
    isExpanded: Boolean,
    answerState: QuizAnswerState,
    onToggleExpanded: () -> Unit,
    onUpdateAnswer: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onSelfEvaluate: (Boolean) -> Unit,
) {
    val hasFramework = !question.answerFramework.isNullOrBlank()
    val hasReference = hasFramework
    val isNoAnswer = question.answerStatus == "NO_ANSWER"

    // P0 修复(v0.7.2):原实现在无参考答案时直接 return,导致 481 题全部无法答题/自评,
    // 错题本(真题来源)永不写入。现改为非阻断提示,用户仍可输入答案并自评。
    if (!hasReference && isNoAnswer) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
        ) {
            Text(
                text = "该真题暂无参考答案，可输入你的答案后自评，或使用AI助手辅助分析",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(Spacing.sm),
            )
        }
    }

    // ── 答题交互区(NF-PP5 Wave 3.2 新增)─────────────────────────
    // v0.8.4 修复：imePadding 移至 QuizScreen 顶层 Column，避免每个 QuestionCard 重复消费 insets
    Column(modifier = Modifier.padding(top = Spacing.sm)) {
        // ── 状态 1: 未提交 → 输入框 + 提交按钮 ──
        if (!answerState.isSubmitted) {
            OutlinedTextField(
                value = answerState.userAnswer,
                onValueChange = onUpdateAnswer,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.text_02)) },
                placeholder = { Text(stringResource(R.string.text_03)) },
                minLines = 3,
                maxLines = 8,
            )
            Button(
                onClick = onSubmitAnswer,
                enabled = answerState.userAnswer.isNotBlank() && !answerState.isSubmitted,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .align(Alignment.End),
            ) {
                Text(stringResource(R.string.text_04))
            }
        } else {
            // ── 状态 2/3: 已提交 → 展示用户答案(锁定) ──
            Text(
                text = "你的答案：",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
            ) {
                Text(
                    text = answerState.userAnswer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
        }

        // ── 参考答案区(展开/折叠) ───────────────────────────────
        TextButton(
            onClick = onToggleExpanded,
            modifier = Modifier.padding(top = Spacing.xs),
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.xs),
            )
            Text(if (isExpanded) "收起参考答案" else "查看参考答案")
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = Spacing.xs)) {
                // P0 修复:无参考答案时显示提示,而非空白
                if (!hasReference) {
                    Text(
                        text = "暂无参考答案，请根据自身理解自评",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 答题框架
                if (hasFramework) {
                    Text(
                        text = "答题框架：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        // P0-T6 修正：用 orEmpty() 替代 !!，更安全
                        text = question.answerFramework.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        }

        // ── 自评区(仅状态 2/3 显示) ─────────────────────────────
        if (answerState.isSubmitted) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.sm),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            if (!answerState.isSelfEvaluated) {
                // ── 状态 2: 已提交未自评 → 自评按钮 ──
                Text(
                    text = if (hasReference) "对照参考答案，请自评：" else "请根据你的理解自评：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    FilledTonalButton(
                        onClick = { onSelfEvaluate(true) },
                        // v0.8.3 修复：防抖，避免快速连点重复写入错题本
                        enabled = !answerState.isSelfEvaluated,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.text_05))
                    }
                    FilledTonalButton(
                        onClick = { onSelfEvaluate(false) },
                        enabled = !answerState.isSelfEvaluated,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.text_06))
                    }
                }
            } else {
                // ── 状态 3: 已自评 → 对错反馈 ──
                // v0.8.4 修复：原用 Unicode ✓/✗ 字符，改为 Material Icon 保持视觉一致
                val (feedbackText, feedbackColor, feedbackIcon) = if (answerState.isCorrect) {
                    Triple("自评：答对了", MaterialTheme.colorScheme.primary, Icons.Default.Check)
                } else {
                    Triple("自评：答错了（已加入错题本）", MaterialTheme.colorScheme.error, Icons.Default.Close)
                }
                Surface(
                    color = if (answerState.isCorrect) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Icon(
                            imageVector = feedbackIcon,
                            contentDescription = null,
                            tint = feedbackColor,
                        )
                        Text(
                            text = feedbackText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = feedbackColor,
                        )
                    }
                }
            }
        }
    }
}

// ── 辅助组件 ────────────────────────────────────────────────────

// InfoChip 已迁移至共享 WenyanInfoChip 组件（ChipVariant.SECONDARY）
// EmptyState 已迁移至共享 EmptyState 组件

// ── 映射工具 ────────────────────────────────────────────────────

/** 将题型代码映射为中文显示 */
private fun formatQuestionType(type: String): String = when (type) {
    "TERM_EXPLANATION" -> "名词解释"
    "SHORT_ANSWER" -> "简答题"
    "ESSAY" -> "论述题"
    "WRITING" -> "写作题"
    else -> type
}

/**
 * 将答案状态映射为 ContentSource 标签。
 *
 * - HAS_ANSWER → TEXTBOOK_NATIVE（绿色"资料"）
 * - NO_ANSWER → MISSING（红色"缺失"）
 * - AI_GENERATED → AI_GENERATED（蓝色"AI"）
 * - null → null（不显示）
 */
private fun mapAnswerStatus(answerStatus: String?): String? = when (answerStatus) {
    "HAS_ANSWER" -> ContentSource.TEXTBOOK_NATIVE
    "NO_ANSWER" -> ContentSource.MISSING
    "AI_GENERATED" -> ContentSource.AI_GENERATED
    else -> null
}
