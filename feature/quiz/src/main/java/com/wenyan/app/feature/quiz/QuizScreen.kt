package com.wenyan.app.feature.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.designsystem.component.ContentSource
import com.wenyan.app.core.designsystem.component.ContentSourceBadge
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
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
 * - 折叠展开答题框架/范文（范文标注"非标准答案"）
 * - 关联知识点入口（跳转知识点详情）
 * - AI助手入口（跳转AI助手，苏格拉底式引导）
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "真题练习",
                actions = {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            // 年份选择行
            YearSelector(
                years = uiState.availableYears,
                selectedYear = uiState.selectedYear,
                onYearSelected = viewModel::selectYear,
            )

            Crossfade(
                targetState = uiState.isLoading to uiState.questions.isEmpty(),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "quiz_state",
                modifier = Modifier.fillMaxSize(),
            ) { (isLoading, isEmpty) ->
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
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
                            onToggleExpanded = viewModel::toggleExpanded,
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
        items(years) { year ->
            FilterChip(
                selected = selectedYear == year,
                onClick = { onYearSelected(year) },
                label = { Text("${year}年") },
                leadingIcon = if (selectedYear == year) {
                    { Text("✓") }
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
    onToggleExpanded: (String) -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(items = questions, key = { it.id }) { question ->
            QuestionCard(
                question = question,
                isExpanded = question.id in expandedIds,
                onToggleExpanded = { onToggleExpanded(question.id) },
                onNavigateToAiAssistant = onNavigateToAiAssistant,
                onNavigateToDetail = onNavigateToDetail,
                modifier = Modifier.animateItem(),
            )
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
 *    - HAS_ANSWER → 展示答题框架 + 范文（标注"范文，非标准答案"）
 *    - NO_ANSWER → 提示"暂无参考答案，可使用AI助手辅助分析"
 *    - AI_GENERATED → 展示AI生成的答题框架
 * 8. 底部操作行：关联知识点入口 + AI助手入口
 */
@Composable
private fun QuestionCard(
    question: QuizQuestionItem,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
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

            // 4. 题目正文（完整展示）
            Text(
                text = question.content,
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

            // 7. 答题区（折叠/展开）
            AnswerSection(
                question = question,
                isExpanded = isExpanded,
                onToggleExpanded = onToggleExpanded,
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
                            imageVector = Icons.Default.MenuBook,
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
                    Text("问AI")
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
 * 答题区组件（折叠/展开）。
 *
 * - HAS_ANSWER 且有答题框架/范文 → 展示"查看答题框架"按钮，展开后显示答题框架 + 范文
 * - NO_ANSWER → 提示"暂无参考答案，可使用AI助手辅助分析"
 * - AI_GENERATED → 展示"查看AI解析"按钮，展开后显示答题框架（标注AI生成）
 */
@Composable
private fun AnswerSection(
    question: QuizQuestionItem,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val hasFramework = !question.answerFramework.isNullOrBlank()
    val hasEssay = !question.sampleEssay.isNullOrBlank()
    val isNoAnswer = question.answerStatus == "NO_ANSWER"

    if (isNoAnswer && !hasFramework) {
        // Spec：NO_ANSWER 且无答题框架 → 提示使用AI助手
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
        ) {
            Text(
                text = "该真题暂无参考答案，可使用AI助手辅助分析（AI生成内容标注为AI_GENERATED）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(Spacing.sm),
            )
        }
        return
    }

    if (!hasFramework && !hasEssay) return

    // 展开/收起按钮
    TextButton(
        onClick = onToggleExpanded,
        modifier = Modifier.padding(top = Spacing.xs),
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.padding(end = Spacing.xs),
        )
        Text(if (isExpanded) "收起答案" else "查看答题框架")
    }

    // 展开内容：答题框架 + 范文
    AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.padding(top = Spacing.xs)) {
            // 答题框架
            if (hasFramework) {
                Text(
                    text = "答题框架：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    // P0-T6 修正：原 `question.answerFramework!!`，已由 hasFramework 守护非 null，
                    // 但用 orEmpty() 更安全（避免重构时守护条件被改而遗留 NPE）。
                    text = question.answerFramework.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            // 范文（标注"范文，非标准答案"）
            if (hasEssay) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = "范文",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    ContentSourceBadge(
                        contentSource = ContentSource.TEXTBOOK_NATIVE,
                    )
                }
                Text(
                    text = "（范文，非标准答案）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    // P0-T6 修正：原 `question.sampleEssay!!`，已由 hasEssay 守护非 null，
                    // 用 orEmpty() 更安全。
                    text = question.sampleEssay.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
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
