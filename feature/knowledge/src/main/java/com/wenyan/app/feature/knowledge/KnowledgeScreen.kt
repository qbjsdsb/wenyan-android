package com.wenyan.app.feature.knowledge

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.knowledge.R

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import com.wenyan.app.core.designsystem.component.LocalLazyListState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import com.wenyan.app.core.designsystem.component.WenyanLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.ChipVariant
import com.wenyan.app.core.designsystem.component.EmptyState
import com.wenyan.app.core.designsystem.component.ErrorState
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanInfoChip
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar

/**
 * 知识点列表界面骨架。
 *
 * 布局参考 Web 原型 index.html（顶部搜索 + 分类标签 + 列表），
 * 采用 Material3 FilterChip 做分类、LazyColumn 渲染知识点卡片。
 *
 * v0.8.19 P1-UI-1 新增搜索框:
 * - 顶部 OutlinedTextField 实时搜索(debounce 300ms)
 * - 搜索范围:title / core_conclusion / full_content / study_text 四字段 LIKE
 * - 搜索 + 分类筛选可叠加(在搜索结果中再按科目筛选)
 * - 右侧 Clear 按钮一键清空搜索
 *
 * TopAppBar 右上角提供"AI助手"入口（与底部 NavigationBar 第 5 个 Tab 形成双入口）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToQuizPractice: () -> Unit = {},
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    // v0.8.17 修复 M1:订阅独立 selectedCategory StateFlow,error/loading 态下
    // 也能立即响应分类切换,与 uiState.selectedCategory(仅在 success 分支更新)解耦
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val frameworkState by viewModel.frameworkUiState.collectAsStateWithLifecycle()
    var browseModeName by rememberSaveable { mutableStateOf(KnowledgeBrowseMode.FRAMEWORK.name) }
    val browseMode = KnowledgeBrowseMode.entries.find { it.name == browseModeName }
        ?: KnowledgeBrowseMode.FRAMEWORK
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = stringResource(R.string.kp_list_title),
                actions = {
                    IconButton(onClick = onNavigateToAiAssistant) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = stringResource(R.string.kp_ai_assistant),
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
            // v0.9.34 横屏：顶部区（入口卡/搜索/分类）与下方列表对齐限宽居中，
            // 避免横屏下搜索框/分类 chips 全宽拉伸（列表 KnowledgeList 已限宽 comfortable）
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = MaxContentWidth.comfortable),
                ) {
                    // v0.9.33 新增：真题背题入口卡（名词解释/简答专项，位于搜索框上方）
                    QuizPracticeEntryCard(
                        onClick = onNavigateToQuizPractice,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    )

                    BrowseModeSwitcher(
                        mode = browseMode,
                        onModeChange = { browseModeName = it.name },
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    )

                    if (browseMode == KnowledgeBrowseMode.LIST) {
                        // v0.8.19 P1-UI-1: 搜索框
                        // v0.9.25 修复：错误态下禁用搜索/分类（原实现可输入/点击高亮，但数据流已终止不重载）
                        val filterEnabled = uiState.error == null
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::updateSearchQuery,
                            onClear = viewModel::clearSearch,
                            enabled = filterEnabled,
                        )

                        // 分类标签行
                        // v0.8.17 修复 M1:用独立 selectedCategory StateFlow,error/loading 态下也有反馈
                        CategoryChips(
                            selectedCategory = selectedCategory,
                            enabled = filterEnabled,
                            onCategorySelected = viewModel::selectCategory,
                        )
                    }
                }
            }

            Crossfade(
                targetState = Triple(browseMode, uiState.isLoading, uiState.error),
                animationSpec = tween(WenyanMotion.DurationMedium, easing = WenyanMotion.DecelerateEasing),
                label = "knowledge_state",
                modifier = Modifier.fillMaxSize(),
            ) { (mode, isLoading, error) ->
                when {
                    mode == KnowledgeBrowseMode.FRAMEWORK -> {
                        FrameworkBrowser(
                            state = frameworkState,
                            onNavigateToDetail = onNavigateToDetail,
                            onRetry = viewModel::retry,
                        )
                    }
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
                                title = stringResource(R.string.kp_load_failed),
                                message = error,
                                onRetry = viewModel::retry,
                            )
                        }
                    }
                    uiState.knowledgePoints.isEmpty() -> {
                        // v0.8.19 P1-UI-1: 区分"无搜索结果"和"无数据"两种空态
                        // v0.8.20 P1-3 修复:搜索 + 分类叠加下 0 结果时,提示用户切换分类
                        // (如"鲁迅"在"古代文学"分类下搜不到,但切换到"现当代文学"可找到)
                        // v0.8.17 修复 M1:用独立 selectedCategory StateFlow
                        val isFiltered = selectedCategory != KnowledgeCategory.ALL
                        val title = when {
                            searchQuery.isNotBlank() && isFiltered ->
                                "在“${selectedCategory.label}”中未找到“${searchQuery.trim()}”"
                            searchQuery.isNotBlank() ->
                                "未找到匹配“${searchQuery.trim()}”的知识点"
                            else -> "暂无知识点，等待种子数据加载"
                        }
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = title,
                            // 搜索 + 分类叠加下 0 结果时,提供"查看全部分类"快捷操作
                            action = {
                                if (searchQuery.isNotBlank() && isFiltered) {
                                    TextButton(onClick = { viewModel.selectCategory(KnowledgeCategory.ALL) }) {
                                        Text(stringResource(R.string.text_06))
                                    }
                                }
                            },
                        )
                    }
                    else -> {
                        KnowledgeList(
                            items = uiState.knowledgePoints,
                            onNavigateToDetail = onNavigateToDetail,
                            contentPadding = PaddingValues(Spacing.lg),
                        )
                    }
                }
            }
        }
    }
}

private enum class KnowledgeBrowseMode {
    FRAMEWORK,
    LIST,
}

@Composable
private fun BrowseModeSwitcher(
    mode: KnowledgeBrowseMode,
    onModeChange: (KnowledgeBrowseMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FilterChip(
            selected = mode == KnowledgeBrowseMode.FRAMEWORK,
            onClick = { onModeChange(KnowledgeBrowseMode.FRAMEWORK) },
            label = { Text(stringResource(R.string.kp_mode_framework)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.AccountTree, contentDescription = null)
            },
        )
        FilterChip(
            selected = mode == KnowledgeBrowseMode.LIST,
            onClick = { onModeChange(KnowledgeBrowseMode.LIST) },
            label = { Text(stringResource(R.string.kp_mode_list)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Inbox, contentDescription = null)
            },
        )
    }
}

/**
 * 框架浏览器：科目总览 → 根章节 → 子专题 → 知识点。
 *
 * 只展示当前层级，避免把四层树一次性铺在手机屏幕上；系统返回键与页面返回按钮
 * 共享同一条父级路径，进入详情后仍复用原有知识点详情导航。
 */
@Composable
private fun FrameworkBrowser(
    state: FrameworkUiState,
    onNavigateToDetail: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapterId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSubject = state.subjects.firstOrNull { it.id == selectedSubjectId }
    val rootChapterId = selectedSubject?.rootChapterId
    val currentChapterId = selectedChapterId ?: rootChapterId
    val currentChapter = selectedSubject?.chapters?.firstOrNull { it.id == currentChapterId }

    BackHandler(enabled = selectedSubject != null) {
        if (currentChapter != null && currentChapter.id != rootChapterId) {
            selectedChapterId = currentChapter.parentId ?: rootChapterId
        } else {
            selectedSubjectId = null
            selectedChapterId = null
        }
    }

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WenyanLoadingIndicator()
            }
        }
        state.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorState(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.kp_framework_load_failed),
                    message = state.error,
                    onRetry = onRetry,
                )
            }
        }
        state.subjects.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.AccountTree,
                    title = stringResource(R.string.kp_framework_no_data),
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = MaxContentWidth.comfortable),
                ) {
                    FrameworkHeader(
                        subject = selectedSubject,
                        currentChapter = currentChapter,
                        atRoot = currentChapter?.id == rootChapterId,
                        onBack = {
                            if (currentChapter != null && currentChapter.id != rootChapterId) {
                                selectedChapterId = currentChapter.parentId ?: rootChapterId
                            } else {
                                selectedSubjectId = null
                                selectedChapterId = null
                            }
                        },
                    )
                    AnimatedContent(
                        targetState = currentChapterId ?: "framework_home",
                        transitionSpec = {
                            fadeIn(animationSpec = tween(WenyanMotion.DurationMedium)) togetherWith
                                fadeOut(animationSpec = tween(WenyanMotion.DurationShort))
                        },
                        label = "framework_navigation",
                        modifier = Modifier.fillMaxSize(),
                    ) { chapterId ->
                        if (selectedSubject == null) {
                            FrameworkSubjectList(
                                subjects = state.subjects,
                                onSelect = { subject ->
                                    selectedSubjectId = subject.id
                                    selectedChapterId = subject.rootChapterId
                                },
                            )
                        } else {
                            FrameworkChapterList(
                                subject = selectedSubject,
                                chapterId = chapterId,
                                onSelectChapter = { selectedChapterId = it },
                                onNavigateToDetail = onNavigateToDetail,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameworkHeader(
    subject: FrameworkSubjectItem?,
    currentChapter: FrameworkChapterItem?,
    atRoot: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (subject != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.kp_framework_back),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentChapter?.title ?: stringResource(R.string.kp_framework_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (subject == null) {
                Text(
                    text = stringResource(R.string.kp_framework_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (atRoot) {
                Text(
                    text = stringResource(R.string.kp_framework_home),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FrameworkSubjectList(
    subjects: List<FrameworkSubjectItem>,
    onSelect: (FrameworkSubjectItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(subjects, key = { it.id }, contentType = { "frameworkSubject" }) { subject ->
            val root = subject.chapters.firstOrNull { it.id == subject.rootChapterId }
            Surface(
                onClick = { onSelect(subject) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                R.string.kp_framework_subject_desc,
                                root?.totalPointCount ?: 0,
                                root?.highFrequencyCount ?: 0,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameworkChapterList(
    subject: FrameworkSubjectItem,
    chapterId: String,
    onSelectChapter: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    val current = subject.chapters.firstOrNull { it.id == chapterId }
    val children = subject.chapters
        .filter { it.parentId == chapterId }
        .sortedBy { it.sortOrder }
    val points = subject.pointsByChapter[chapterId].orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (children.isNotEmpty()) {
            item(key = "chapter_heading") {
                Text(
                    text = stringResource(
                        R.string.kp_framework_subchapter_count,
                        current?.title.orEmpty(),
                        children.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(children, key = { it.id }, contentType = { "frameworkChapter" }) { chapter ->
                FrameworkChapterCard(
                    chapter = chapter,
                    onClick = { onSelectChapter(chapter.id) },
                )
            }
        }
        if (points.isNotEmpty()) {
            item(key = "point_heading") {
                Text(
                    text = stringResource(R.string.kp_framework_points_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
            items(points, key = { it.id }, contentType = { "knowledgeItem" }) { point ->
                KnowledgePointCard(
                    item = point,
                    onClick = { onNavigateToDetail(point.id) },
                )
            }
        }
        if (children.isEmpty() && points.isEmpty()) {
            item(key = "chapter_empty") {
                Text(
                    text = stringResource(R.string.kp_framework_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.xl),
                )
            }
        }
    }
}

@Composable
private fun FrameworkChapterCard(
    chapter: FrameworkChapterItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.kp_framework_chapter_desc,
                        chapter.totalPointCount,
                        chapter.childCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (chapter.highFrequencyCount > 0) {
                WenyanInfoChip(
                    text = stringResource(R.string.kp_framework_high_frequency, chapter.highFrequencyCount),
                    variant = ChipVariant.PRIMARY,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 搜索框(v0.8.19 P1-UI-1 新增)。
 *
 * 使用 [OutlinedTextField] + 前置 Search Icon + 后置 Clear 按钮(仅 query 非空时显示)。
 *
 * 设计要点:
 * - 搜索词实时同步到 ViewModel(经 SavedStateHandle 持久化),
 *   ViewModel 内 debounce 300ms 后触发 DB 查询,UI 无需手动防抖。
 * - Clear 按钮 onClick 调用 [viewModel.clearSearch],一键清空搜索恢复全部浏览。
 * - imeAction = Done:用户按键盘 Done 键收起键盘(不触发搜索,搜索由 debounce 自动触发)。
 * - singleLine = true:搜索框单行,避免多行输入导致布局抖动。
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    enabled: Boolean = true,
) {
    // v0.8.20 P0-1 修复:原注释声称 imeAction=Done 但代码未实现,
    // 用户按键盘 Done 键无法收起键盘。现补齐 keyboardOptions + keyboardActions。
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        placeholder = { Text(stringResource(R.string.text_07)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.kp_clear_search),
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
    )
}

// ── 真题背题入口卡（v0.9.33）────────────────────────────────

/**
 * 知识点页顶部"真题背题"入口卡（v0.9.33 新增）。
 *
 * 引导进入名词解释/简答背题专项。副标题明确标注"论述题见论述题 Tab"，
 * 与论述题板块划清边界（背题练"记得住"，论述题练"怎么写"）。
 */
@Composable
private fun QuizPracticeEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.kp_quiz_entry_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.kp_quiz_entry_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// 分类标签行
@Composable
private fun CategoryChips(
    selectedCategory: KnowledgeCategory,
    enabled: Boolean = true,
    onCategorySelected: (KnowledgeCategory) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // P2-LAZY-1 修正：LazyRow items 加 key（用 category.name 唯一标识），避免重组时丢失选中状态
        // NF-UP4 修正：加 contentType 让 LazyRow 复用同一类型 item 的 slot，提升滚动性能
        items(items = KnowledgeCategory.entries.toList(), key = { it.name }, contentType = { "category" }) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                enabled = enabled,
                label = { Text(category.label) },
            )
        }
    }
}

// 知识点列表
@Composable
private fun KnowledgeList(
    items: List<KnowledgePointItem>,
    onNavigateToDetail: (String) -> Unit,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
) {
    // v0.8.15 Stage 1: 横屏/平板下限制内容最大宽度并居中，避免知识点卡片行宽过宽阅读疲劳。
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
                items(items = items, key = { it.id }, contentType = { "knowledgeItem" }) { item ->
                    KnowledgePointCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

// 单个知识点卡片
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KnowledgePointCard(
    item: KnowledgePointItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalCard(
        modifier = modifier
            .fillMaxWidth()
            // P1-3 修复：mergeDescendants 合并卡片内 3 个 Text 为单一语义节点，
            // TalkBack 一次性朗读"标题，科目，摘要"而非逐个滑动 3 次。
            .semantics(mergeDescendants = true) {}
            // NF-UA4 修复：加 role=Role.Button 语义，TalkBack 朗读"按钮"，
            // 视障用户才能识别卡片可点击。原 .clickable 无 role，TalkBack 不朗读"按钮"。
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        // v0.8.3 修复（P2-K-1）：加 verticalArrangement.spacedBy 让 title/subject/summary 之间有呼吸感
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                // P1-1 修复：长标题限 2 行 + 省略号，保持列表卡片高度一致
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // v0.8.20 P1-2: 科目 + 考频 chip 同行展示(FlowRow 自动换行)。
            // 考频用 PRIMARY/SECONDARY/TERTIARY chip 突出高频考点,
            // 与详情页 HeaderSection 的 freqVariant 映射一致。
            // NEVER 不展示 chip(避免"未考"chip 干扰浏览,无考频信息比"未考"标签更克制)。
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = item.subject,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                val (freqLabel, freqVariant) = examFrequencyChip(item.examFrequency)
                if (freqLabel != null) {
                    WenyanInfoChip(text = freqLabel, variant = freqVariant)
                }
            }
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // P1-1 修复：长摘要限 3 行 + 省略号，点击进详情看全文
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 将考频原始值映射为列表卡片 chip 标签 + variant(v0.8.20 P1-2 新增)。
 *
 * 与 [KnowledgePointDetailScreen] 的 HeaderSection freqVariant 映射一致,
 * 保持列表页与详情页考频视觉表达统一(高频 PRIMARY / 中频 SECONDARY / 低频 TERTIARY)。
 *
 * NEVER / 未知值返回 null,UI 不展示 chip(避免"未考"标签干扰浏览,
 * 用户更关心"哪些是高频考点",无考频信息比"未考"标签更克制)。
 */
private fun examFrequencyChip(examFrequency: String): Pair<String?, ChipVariant> = when (examFrequency) {
    "HIGH" -> "高频" to ChipVariant.PRIMARY
    "MEDIUM" -> "中频" to ChipVariant.SECONDARY
    "LOW" -> "低频" to ChipVariant.TERTIARY
    else -> null to ChipVariant.NEUTRAL
}
