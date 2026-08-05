package com.wenyan.app.feature.knowledge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.common.util.ExamContentCleaner
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import kotlinx.coroutines.withTimeout

// v0.9.33：对齐 CardsScreen v0.9.23 / WrongAnswerScreen v0.9.25 模式——
// material3 1.5.0-alpha18 的 duration 计时若异常导致 showSnackbar 挂起不返回，
// 状态永远清不掉、Snackbar 永远显示。withTimeout(5s) 兜底即使挂起也强制返回。
private const val SNACKBAR_TIMEOUT_MS = 5_000L

/**
 * 真题背题详情界面（v0.9.33 新增）。
 *
 * 纯背诵模式：
 * - 题干大字展示（`ExamContentCleaner` 清洗题号）
 * - 点"显示答案" → [ExamQuestionEntity.answerFramework] 结构化要点展示
 * - 显示答案后可标记"不会"（进错题本 + FSRS）/ "会了"（推进）
 * - 上一题/下一题在同一筛选集内导航（进度"第 X / N 题"）
 *
 * 与论述题详情差异化：无 AI 审题、无自评输入，聚焦"记得住"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPracticeDetailScreen(
    onBack: (() -> Unit)? = null,
    viewModel: QuizPracticeDetailViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val question by viewModel.currentQuestion.collectAsStateWithLifecycle()
    val showAnswer by viewModel.showAnswer.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )
    val snackbarHostState = remember { SnackbarHostState() }

    // 提示消息 → Snackbar（先清空再展示，避免重复；withTimeout 防挂起）
    LaunchedEffect(message) {
        message?.let {
            viewModel.clearMessage()
            withTimeout(SNACKBAR_TIMEOUT_MS) {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = stringResource(R.string.kp_quiz_detail_title),
                subtitle = if (progress.total > 0) {
                    stringResource(R.string.kp_quiz_progress, progress.index + 1, progress.total)
                } else {
                    null
                },
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
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
                            title = stringResource(R.string.kp_load_failed),
                            message = error,
                            onRetry = viewModel::retry,
                        )
                    }
                }
                question != null -> {
                    PracticeQuestionContent(
                        question = question!!,
                        showAnswer = showAnswer,
                        onToggleShowAnswer = viewModel::toggleShowAnswer,
                        modifier = Modifier.weight(1f),
                    )
                    PracticeActionBar(
                        showAnswer = showAnswer,
                        canPrevious = progress.index > 0,
                        canNext = progress.index < progress.total - 1,
                        onPrevious = viewModel::previous,
                        onNext = viewModel::next,
                        onToggleShowAnswer = viewModel::toggleShowAnswer,
                        onMarkKnow = viewModel::markKnow,
                        onMarkDontKnow = viewModel::markDontKnow,
                    )
                }
                else -> {
                    // 理论上不会到达（question == null 且非 loading/error）
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.kp_quiz_empty_all),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── 题干 + 答案区 ───────────────────────────────────────────

@Composable
private fun PracticeQuestionContent(
    question: com.wenyan.app.core.database.entity.ExamQuestionEntity,
    showAnswer: Boolean,
    onToggleShowAnswer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabel = when (question.questionType) {
        QuizPracticeTypes.TERM_EXPLANATION -> stringResource(R.string.kp_quiz_type_term)
        QuizPracticeTypes.SHORT_ANSWER -> stringResource(R.string.kp_quiz_type_short)
        else -> question.questionType
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = MaxContentWidth.compact),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "meta") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    question.let {
                        Text(
                            text = stringResource(R.string.kp_quiz_year_format, it.year),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item(key = "question") {
                Text(
                    text = ExamContentCleaner.stripQuestionNumber(question.content),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            item(key = "answer") {
                AnimatedVisibility(
                    visible = showAnswer,
                    enter = androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(
                            WenyanMotion.DurationMedium,
                            easing = WenyanMotion.DecelerateEasing,
                        ),
                    ) + androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.shrinkVertically() +
                        androidx.compose.animation.fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = stringResource(R.string.kp_quiz_answer_hint),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = question.answerFramework ?: stringResource(R.string.kp_quiz_no_answer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.xs),
                        )
                    }
                }
            }
        }
    }
}

// ── 底部操作栏 ──────────────────────────────────────────────

/**
 * 背题操作栏（v0.9.33）。
 *
 * 未显示答案：主操作"显示答案"，辅以上一题/下一题。
 * 已显示答案：主操作"不会"(error) + "会了"(primary)，辅以收起/上/下。
 * 所有按钮 ≥48dp 触控目标。
 */
@Composable
private fun PracticeActionBar(
    showAnswer: Boolean,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShowAnswer: () -> Unit,
    onMarkKnow: () -> Unit,
    onMarkDontKnow: () -> Unit,
) {
    // v0.9.34 横屏：操作栏与题干区对齐限宽居中（题干已 widthIn compact），
    // 避免横屏下按钮全宽拉伸
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = MaxContentWidth.compact)
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // 上一题 / 下一题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = onPrevious,
                enabled = canPrevious,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Spacing.xs),
                )
                Text(stringResource(R.string.kp_quiz_prev))
            }
            TextButton(
                onClick = onNext,
                enabled = canNext,
            ) {
                Text(stringResource(R.string.kp_quiz_next))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = Spacing.xs),
                )
            }
        }

        if (!showAnswer) {
            // 未显示答案：显示答案按钮（主操作，全宽）
            Button(
                onClick = onToggleShowAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
            ) {
                Text(stringResource(R.string.kp_quiz_show_answer))
            }
        } else {
            // 已显示答案：不会（红）+ 会了（绿）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onMarkDontKnow,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Spacing.xs),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.kp_quiz_dont_know))
                }
                Button(
                    onClick = onMarkKnow,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Spacing.xs),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(stringResource(R.string.kp_quiz_know))
                }
            }
        }
    }
    }
}
