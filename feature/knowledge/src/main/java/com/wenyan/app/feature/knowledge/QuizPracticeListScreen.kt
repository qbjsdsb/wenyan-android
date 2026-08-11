package com.wenyan.app.feature.knowledge

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import com.wenyan.app.core.database.entity.SubjectEntity

/**
 * 真题背题列表界面（v0.9.33 新增）。
 *
 * 入口：知识点 Tab 顶部"真题背题"入口卡 → 本页。
 *
 * 功能：
 * - 三维筛选：题型（全部/名词解释/简答）/ 科目（LazyRow FilterChip）/ 年份（LazyRow FilterChip）
 * - 列表卡片：题型标签 + 科目 + 年份 + 答案字数 + 题干预览
 * - 点击进入背题页（携带当前筛选条件，前后题在同一筛选集内导航）
 *
 * 与论述题板块差异化：本页只承载名词解释 + 简答（[QuizPracticeTypes.ALL]），
 * 数据层已排除 ESSAY，不会与论述题 Tab 重复。
 *
 * 数据流：[QuizPracticeListViewModel] combine(题目, 科目, 3筛选) 内存过滤。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizPracticeListScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToQuizPracticeDetail: (
        questionId: String,
        selectedType: String?,
        selectedSubjectId: String?,
        selectedYear: Int?,
        selectedPaperCode: String?,
    ) -> Unit = { _, _, _, _, _ -> },
    viewModel: QuizPracticeListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedPaperCode by viewModel.selectedPaperCode.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = stringResource(R.string.kp_quiz_list_title),
                subtitle = if (uiState.totalCount > 0) {
                    if (uiState.filteredCount != uiState.totalCount) {
                        stringResource(R.string.kp_quiz_list_filtered_count, uiState.filteredCount, uiState.totalCount)
                    } else {
                        stringResource(R.string.kp_quiz_list_count, uiState.totalCount)
                    }
                } else {
                    null
                },
                onBack = onBack,
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
            // ── 筛选区（错误态下禁用交互，与 EssayList 一致）──
            QuizPracticeFilterBar(
                selectedType = selectedType,
                subjects = uiState.subjects,
                selectedSubjectId = selectedSubjectId,
                years = uiState.years,
                selectedYear = selectedYear,
                paperCodes = uiState.paperCodes,
                selectedPaperCode = selectedPaperCode,
                enabled = uiState.error == null,
                onTypeSelected = viewModel::selectType,
                onSubjectSelected = viewModel::selectSubject,
                onYearSelected = viewModel::selectYear,
                onPaperCodeSelected = viewModel::selectPaperCode,
            )

            // ── 列表区 ──
            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.error, uiState.questions.isEmpty()),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "quiz_practice_list_state",
                // 筛选栏位于同一 Column 中，列表区只填充剩余高度。
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { (isLoading, error, isEmpty) ->
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
                                onRetry = viewModel::retry,
                                message = error,
                            )
                        }
                    }
                    isEmpty -> {
                        val hasFilter = selectedType != null || selectedSubjectId != null || selectedYear != null ||
                            selectedPaperCode != null
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                icon = Icons.Default.Inbox,
                                title = stringResource(
                                    when {
                                        hasFilter -> R.string.kp_quiz_empty_filter
                                        else -> R.string.kp_quiz_empty_all
                                    },
                                ),
                                description = stringResource(R.string.kp_quiz_empty_desc),
                            )
                        }
                    }
                    else -> {
                        QuizPracticeList(
                            items = uiState.questions,
                            onNavigateToDetail = { questionId ->
                                onNavigateToQuizPracticeDetail(
                                    questionId,
                                    selectedType,
                                    selectedSubjectId,
                                    selectedYear,
                                    selectedPaperCode,
                                )
                            },
                            contentPadding = PaddingValues(Spacing.lg),
                        )
                    }
                }
            }
        }
    }
}

// ── 筛选栏 ──────────────────────────────────────────────────

/**
 * 背题三维筛选栏（v0.9.33）。
 *
 * - 题型：横向滚动 FilterChip（全部/名词解释/简答）
 * - 科目 + 年份：横向滚动 FilterChip（科目 4 个 + 年份最多 16 个，LazyRow 不换行溢出）
 */
@Composable
private fun QuizPracticeFilterBar(
    selectedType: String?,
    subjects: List<SubjectEntity>,
    selectedSubjectId: String?,
    years: List<Int>,
    selectedYear: Int?,
    paperCodes: List<String>,
    selectedPaperCode: String?,
    enabled: Boolean,
    onTypeSelected: (String?) -> Unit,
    onSubjectSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onPaperCodeSelected: (String?) -> Unit,
) {
    // v0.9.34 横屏：筛选栏与下方列表对齐限宽居中（列表已 widthIn comfortable），
    // 避免横屏下题型/科目/年份 LazyRow 全宽拉伸
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
    Column(
        modifier = Modifier.widthIn(max = MaxContentWidth.comfortable),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // 题型 Tab
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "type_all") {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onTypeSelected(null) },
                    enabled = enabled,
                    label = { Text(stringResource(R.string.kp_quiz_type_all)) },
                )
            }
            item(key = "type_term") {
                FilterChip(
                    selected = selectedType == QuizPracticeTypes.TERM_EXPLANATION,
                    onClick = { onTypeSelected(QuizPracticeTypes.TERM_EXPLANATION) },
                    enabled = enabled,
                    label = { Text(stringResource(R.string.kp_quiz_type_term)) },
                )
            }
            item(key = "type_short") {
                FilterChip(
                    selected = selectedType == QuizPracticeTypes.SHORT_ANSWER,
                    onClick = { onTypeSelected(QuizPracticeTypes.SHORT_ANSWER) },
                    enabled = enabled,
                    label = { Text(stringResource(R.string.kp_quiz_type_short)) },
                )
            }
        }

        // 科目筛选
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "subject_all") {
                FilterChip(
                    selected = selectedSubjectId == null,
                    onClick = { onSubjectSelected(null) },
                    enabled = enabled,
                    label = { Text(stringResource(R.string.kp_quiz_all_subjects)) },
                )
            }
            items(items = subjects, key = { it.id }) { subject ->
                FilterChip(
                    selected = selectedSubjectId == subject.id,
                    onClick = { onSubjectSelected(subject.id) },
                    enabled = enabled,
                    label = { Text(subject.name) },
                )
            }
        }

        // 年份筛选
        if (years.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item(key = "year_all") {
                    FilterChip(
                        selected = selectedYear == null,
                        onClick = { onYearSelected(null) },
                        enabled = enabled,
                        label = { Text(stringResource(R.string.kp_quiz_all_years)) },
                    )
                }
                items(items = years, key = { it }) { year ->
                    FilterChip(
                        selected = selectedYear == year,
                        onClick = { onYearSelected(year) },
                        enabled = enabled,
                        label = { Text(stringResource(R.string.kp_quiz_year_format, year)) },
                    )
                }
            }
        }

        if (paperCodes.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item(key = "paper_all") {
                    FilterChip(
                        selected = selectedPaperCode == null,
                        onClick = { onPaperCodeSelected(null) },
                        enabled = enabled,
                        label = { Text("全部试卷") },
                    )
                }
                items(paperCodes, key = { it }) { code ->
                    FilterChip(
                        selected = selectedPaperCode == code,
                        onClick = { onPaperCodeSelected(code) },
                        enabled = enabled,
                        label = { Text(code) },
                    )
                }
            }
        }
    }
    }
}

// ── 列表 ────────────────────────────────────────────────────

@Composable
private fun QuizPracticeList(
    items: List<QuizPracticeListItem>,
    onNavigateToDetail: (String) -> Unit,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = MaxContentWidth.comfortable),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items = items, key = { it.id }, contentType = { "quizPracticeItem" }) { item ->
                    QuizPracticeListItemCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
    }
}

// ── 列表项卡片 ──────────────────────────────────────────────

@Composable
private fun QuizPracticeListItemCard(
    item: QuizPracticeListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabel = when (item.questionType) {
        QuizPracticeTypes.TERM_EXPLANATION -> stringResource(R.string.kp_quiz_type_term)
        QuizPracticeTypes.SHORT_ANSWER -> stringResource(R.string.kp_quiz_type_short)
        else -> item.questionType
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                // 题型标签（primaryContainer 底色区分题型）
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
                Text(
                    text = item.subjectName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.kp_quiz_year_format, item.year),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.kp_quiz_answer_len, item.answerLength),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = item.contentPreview,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
