package com.wenyan.app.feature.settings

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.settings.R

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.repository.CardFrequencyFilter
import com.wenyan.app.core.data.repository.CardSettings
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeViewModel
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import com.wenyan.app.core.fsrs.ExamCountdownManager
import com.wenyan.app.core.fsrs.StudyPhase

/**
 * 设置页面。
 *
 * 作为“我的”下的子页面展示；从导航入口进入时提供返回箭头。
 * 包含：外观（主题模式/AMOLED）、动态色彩（开关/种子色/调色板风格）、AI 服务、关于。
 */

// NF-UP2 修复：seedColors 移到文件顶层 top-level private val，
// 避免每次 SettingsScreen 重组都创建新 List<Color>（5 个 Color 装箱）。
// 顶层 val 在 class loader 加载时初始化一次，全局共享。
// P0-5 修复：改为带色名的 SeedColorPreset，让 TalkBack 可朗读"种子色：紫色"。
private data class SeedColorPreset(val color: Color, val name: String)

private val SeedColors = listOf(
    SeedColorPreset(Color(0xFF6750A4), "紫色"),
    SeedColorPreset(Color(0xFF0061A4), "蓝色"),
    SeedColorPreset(Color(0xFF006C4C), "绿色"),
    SeedColorPreset(Color(0xFF9C4146), "红色"),
    SeedColorPreset(Color(0xFF7C5800), "棕色"),
)

/**
 * v0.9.25 新增：暗色模式下把种子色亮化为适合图标展示的浅色变体。
 * 原固定深色种子色在暗色/AMOLED 背景下作为 Icon tint 对比度低。
 * 通过向白色混合（lerp）提高亮度，保留色相、饱和度略降。
 */
private fun Color.forTheme(isDark: Boolean): Color {
    if (!isDark) return this
    return lerp(this, Color.White, 0.45f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToApiConfig: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToUpdateCheck: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ThemeViewModel = hiltViewModel(),
    cardSettingsViewModel: CardSettingsViewModel = hiltViewModel(),
) {
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
    val cardSettings by cardSettingsViewModel.cardSettings.collectAsStateWithLifecycle()
    // v0.9.25 修复：根据主题模式计算是否暗色（种子色图标需亮化变体）
    val isDarkTheme = when (themeConfig.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )
    val context = LocalContext.current
    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }
    // v0.9.22 P1-2 修复：消费 ThemeViewModel.errorEvents（主题保存失败提示）。
    // 此前 errorEvents 无任何订阅者，DataStore IOException 时错误被静默丢弃，
    // 用户改主题失败无感知（ThemeViewModel.kt 注释声称"UI 可订阅展示 Snackbar"但从未实现）。
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        // v0.8.15 Stage 1: 横屏/平板下限制内容最大宽度并居中，避免设置项行宽过宽阅读疲劳。
        // 竖屏（<600dp）下 widthIn(max=600) 不生效（屏幕宽 < max），不影响竖屏布局。
        val settingsListState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                    state = settingsListState,
                    modifier = Modifier.widthIn(max = MaxContentWidth.compact),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                    // v0.9.31 布局精修：首尾留白（此前首尾贴边）
                    // v0.9.37 P2：补齐水平边距（全 App 唯一贴边列表，与其他 Screen 一致）
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
                ) {
            // P0 v0.7.2: 考研倒计时卡片(接通 ExamCountdownManager,原完全未接入)
            // v0.9.35 审计修复:注入用户设置的考试日期(原两套真相——设置日期但倒计时不联动)
            item { ExamCountdownCard(examDateMillis = cardSettings.examDateMillis) }

            // P0 v0.7.2: 学习进度卡片(接通 study_progress 表,原死表)
            item { StudyProgressCard() }

            // 外观
            item {
                GroupedCard(title = stringResource(R.string.settings_appearance)) {
                    // 主题模式
                    GroupedCardItem(
                        title = stringResource(R.string.settings_theme_mode),
                        subtitle = when (themeConfig.colorMode) {
                            ColorMode.SYSTEM -> "跟随系统"
                            ColorMode.LIGHT -> "浅色"
                            ColorMode.DARK -> "深色"
                        },
                    )
                    GroupedCardDivider()
                    // v0.6：主题模式选择改用 SegmentedButton（互斥选择更紧凑专业）
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    ) {
                        ColorMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = themeConfig.colorMode == mode,
                                onClick = { viewModel.setColorMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ColorMode.entries.size,
                                ),
                                label = {
                                    Text(
                                        text = when (mode) {
                                            ColorMode.SYSTEM -> "跟随系统"
                                            ColorMode.LIGHT -> "浅色"
                                            ColorMode.DARK -> "深色"
                                        },
                                    )
                                },
                            )
                        }
                    }
                    GroupedCardDivider()
                    // AMOLED 开关
                    GroupedCardItem(
                        title = stringResource(R.string.settings_amoled),
                        description = "深色模式下使用纯黑背景，节省 OLED 电量",
                        trailing = {
                            Switch(
                                checked = themeConfig.amoledMode,
                                onCheckedChange = { viewModel.setAmoledMode(it) },
                            )
                        },
                    )
                }
            }

            // 卡片备考（v0.9.29）
            item {
                CardStudySettingsCard(
                    settings = cardSettings,
                    onDailyNewLimitChange = cardSettingsViewModel::setDailyNewLimit,
                    onFrequencyFilterChange = cardSettingsViewModel::setFrequencyFilter,
                    onSubjectToggle = cardSettingsViewModel::setSubjectFilters,
                    onExamDateChange = cardSettingsViewModel::setExamDate,
                )
            }

            // 动态色彩
            item {
                GroupedCard(title = stringResource(R.string.settings_dynamic_color)) {
                    GroupedCardItem(
                        title = "动态色彩",
                        description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "跟随系统壁纸自动生成色彩"
                        } else {
                            "需要 Android 12 及以上"
                        },
                        trailing = {
                            Switch(
                                checked = themeConfig.dynamicColor,
                                onCheckedChange = { viewModel.setDynamicColor(it) },
                                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            )
                        },
                    )
                    // 种子色 + 调色板风格（动态色彩关闭时显示）
                    AnimatedVisibility(
                        visible = !themeConfig.dynamicColor,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            GroupedCardDivider()
                            // 种子色选择
                            // v0.8.3 修复：原 Row 在窄屏（<360dp）下 5 个色块 + "种子色"标签会溢出裁切。
                            // 改用 FlowRow 自动换行，保证小屏设备完整可见。
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Text(
                                    text = "种子色",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )
                                SeedColors.forEach { preset ->
                                    FilterChip(
                                        selected = themeConfig.seedColor == preset.color,
                                        onClick = { viewModel.setSeedColor(preset.color) },
                                        label = {},
                                        // P0-5 修复：加 semantics contentDescription 让 TalkBack 朗读色名，
                                        // 原 label={} + contentDescription=null 导致视障用户无法区分 5 个色块
                                        modifier = Modifier.semantics {
                                            contentDescription = "种子色：${preset.name}"
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = null,
                                                // v0.9.25 修复：暗色模式下种子色加深、对比度低，
                                                // 用亮化变体保证图标在暗色/AMOLED 背景下可见
                                                tint = preset.color.forTheme(isDarkTheme),
                                            )
                                        },
                                    )
                                }
                            }
                            GroupedCardDivider()
                            // 调色板风格（v0.6：与主题模式选择一致，改用 SegmentedButton）
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                            ) {
                                Text(
                                    text = "风格",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = Spacing.sm),
                                )
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    WenyanPaletteStyle.entries.forEachIndexed { index, style ->
                                        SegmentedButton(
                                            selected = themeConfig.paletteStyle == style,
                                            onClick = { viewModel.setPaletteStyle(style) },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = WenyanPaletteStyle.entries.size,
                                            ),
                                            label = {
                                                Text(
                                                    // v0.8.3 修复：原英文标签与其他全中文 UI 不一致，
                                                    // 用户需猜测含义。改为 M3 中文术语：
                                                    // Tonal Spot=色调点（默认，种子色直接作主色）
                                                    // Neutral=中性（降低主色饱和度，偏灰）
                                                    // Vibrant=鲜艳（提升对比度，色彩浓郁）
                                                    // Expressive=表现力（多色调对比，最活泼）
                                                    text = when (style) {
                                                        WenyanPaletteStyle.TONAL_SPOT -> "色调点"
                                                        WenyanPaletteStyle.NEUTRAL -> "中性"
                                                        WenyanPaletteStyle.VIBRANT -> "鲜艳"
                                                        WenyanPaletteStyle.EXPRESSIVE -> "表现力"
                                                    },
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI 服务
            item {
                GroupedCard(title = stringResource(R.string.settings_ai_services)) {
                    GroupedCardItem(
                        title = stringResource(R.string.settings_api_config),
                        subtitle = stringResource(R.string.settings_api_providers),
                        onClick = onNavigateToApiConfig,
                    )
                }
            }

            // 关于
            item {
                GroupedCard(title = stringResource(R.string.settings_about)) {
                    GroupedCardItem(
                        title = stringResource(R.string.settings_version),
                        // P1-M2 修正：原硬编码 "v0.1.0" 与实际版本脱节，改读系统 packageManager
                        subtitle = "v$currentVersionName",
                    )
                    GroupedCardDivider()
                    // v0.9.11 新增：检查更新入口
                    GroupedCardItem(
                        title = stringResource(R.string.settings_check_update),
                        subtitle = stringResource(R.string.settings_check_update_desc),
                        onClick = onNavigateToUpdateCheck,
                    )
                    GroupedCardDivider()
                    // v0.9.5 新增：教程入口（v0.9.6 精简重构：5 节简洁版）
                    // 欢迎卡 / 快速上手 / 功能模块 / 学习原理（可折叠） / 关于
                    GroupedCardItem(
                        title = stringResource(R.string.settings_about_tutorial),
                        subtitle = stringResource(R.string.settings_about_tutorial_desc),
                        onClick = onNavigateToAbout,
                    )
                }
            }
            } // LazyColumn end
        } // Box end
    }
}

// ── 卡片备考设置卡片（v0.9.29）────────────────────────────────

/**
 * 卡片备考设置卡片（v0.9.29）。
 *
 * 包含：每日新卡数（滑杆 10-200）、考频筛选（三档）、复习科目（四科多选）、考试日期。
 * 数据由 [CardSettingsViewModel] 提供，变更实时写入 [CardSettingsRepository]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardStudySettingsCard(
    settings: CardSettings,
    onDailyNewLimitChange: (Int) -> Unit,
    onFrequencyFilterChange: (CardFrequencyFilter) -> Unit,
    onSubjectToggle: (Set<String>) -> Unit,
    onExamDateChange: (Long?) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    GroupedCard(title = stringResource(R.string.settings_cards)) {
        // 每日新卡数
        // v0.9.35 第四轮审计修复：滑杆本地 state + onValueChangeFinished 提交——
        // 原每次拖动都写 DataStore（快速拖动卡顿/抖动）；拖动过程仅更新本地显示，
        // 松手才持久化，与 settings.dailyNewLimit 变化时重置本地值
        var sliderValue by remember(settings.dailyNewLimit) {
            mutableStateOf(settings.dailyNewLimit.toFloat())
        }
        GroupedCardItem(
            title = stringResource(R.string.settings_daily_new_limit),
            subtitle = "${sliderValue.roundToInt()} 张/天",
            description = "每天最多学习的新卡数量（含复习由 FSRS 自动安排）",
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onDailyNewLimitChange(sliderValue.roundToInt()) },
            valueRange = 10f..200f,
            steps = 18, // 10-200 步进 10
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        )
        GroupedCardDivider()

        // 考频筛选
        Text(
            text = "考频筛选",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.sm,
            ),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
        ) {
            CardFrequencyFilter.entries.forEachIndexed { index, filter ->
                SegmentedButton(
                    selected = settings.frequencyFilter == filter,
                    onClick = { onFrequencyFilterChange(filter) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = CardFrequencyFilter.entries.size,
                    ),
                ) {
                    Text(filter.displayName)
                }
            }
        }
        GroupedCardDivider()

        // 复习科目（四科多选）
        Text(
            text = "复习科目",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.xs,
            ),
        )
        CardSettings.DEFAULT_SUBJECTS.forEach { subject ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = subject in settings.subjectFilters,
                    onCheckedChange = { checked ->
                        onSubjectToggle(
                            if (checked) {
                                settings.subjectFilters + subject
                            } else {
                                settings.subjectFilters - subject
                            },
                        )
                    },
                )
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Spacing.xs),
                )
            }
        }
        GroupedCardDivider()

        // 考试日期
        GroupedCardItem(
            title = stringResource(R.string.settings_exam_date),
            subtitle = settings.examDateMillis?.let { formatExamDate(it) } ?: "未设置（默认 12 月下旬）",
            description = "用于计算距离考试的剩余天数",
            onClick = { showDatePicker = true },
        )

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = settings.examDateMillis,
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onExamDateChange(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.settings_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

/** 格式化考试日期为"yyyy年M月d日"。 */
private fun formatExamDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return DateTimeFormatter.ofPattern("yyyy年M月d日").format(date)
}

// ── 考研倒计时卡片(P0 v0.7.2:接通 ExamCountdownManager) ──────────

/**
 * 考研倒计时与学习阶段展示卡片。
 *
 * 接通 [com.wenyan.app.core.fsrs.ExamCountdownManager](原完全未接入生产代码)。
 * 展示:距考研天数、当前学习阶段(基础/强化/冲刺)、目标保持率。
 */
@Composable
private fun ExamCountdownCard(examDateMillis: Long? = null) {
    // v0.9.35 审计修复:去 remember 固定 today——跨午夜/挂后台返回重组时自动刷新
    val today = java.time.LocalDate.now()
    val examDate = if (examDateMillis != null) {
        java.time.Instant.ofEpochMilli(examDateMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    } else {
        val currentYearExam = ExamCountdownManager.getExamDate(today.year)
        if (today.isAfter(currentYearExam)) {
            ExamCountdownManager.getExamDate(today.year + 1)
        } else {
            currentYearExam
        }
    }
    // 用户设置考试日期优先；未设置回退 ExamCountdownManager 默认（12 月下旬）
    val daysToExam = java.time.temporal.ChronoUnit.DAYS.between(today, examDate).toInt().coerceAtLeast(0)
    val phase = ExamCountdownManager.getStudyPhase(daysToExam)
    val retention = ExamCountdownManager.getGlobalRetention(daysToExam)

    val phaseLabel = when (phase) {
        StudyPhase.BASIC -> "基础阶段"
        StudyPhase.INTENSIVE -> "强化阶段"
        StudyPhase.SPRINT -> "冲刺阶段"
    }
    val phaseSubtitle = when (phase) {
        StudyPhase.BASIC -> "全面打牢基础，构建知识网络"
        StudyPhase.INTENSIVE -> "重点强化，提升答题能力"
        StudyPhase.SPRINT -> "最后冲刺，查漏补缺"
    }

    GroupedCard(title = stringResource(R.string.settings_countdown)) {
        GroupedCardItem(
            title = stringResource(R.string.settings_countdown_days, daysToExam),
            subtitle = "考试日期：${examDate.year}年${examDate.monthValue}月${examDate.dayOfMonth}日",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = phaseLabel,
            subtitle = phaseSubtitle,
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = stringResource(R.string.settings_retention),
            subtitle = "${"%.0f".format(retention * 100)}%（FSRS 调度依据此值动态调整复习间隔）",
        )
    }
}

// ── 学习进度卡片(P0 v0.7.2:接通 study_progress 死表) ──────────

/**
 * 学习进度展示卡片。
 *
 * 展示:连续学习天数、累计学习时长、上次学习的知识点 ID。
 * 数据由 [StudyProgressViewModel] 观察 study_progress 表,
 * 卡片复习评分时由 CardsViewModel 调用 StudyProgressRepository.recordStudySession 写入。
 *
 * v0.8.13 P2-1 修复:用 [StudyProgressUiState] 区分 Loading / Loaded。
 * 原实现 loading 时 progress=null 被当成"streak=0",显示
 * "连续学习 0 天 / 开始今天的学习吧",误导用户。现 loading 时显示占位文案。
 */
@Composable
private fun StudyProgressCard(
    viewModel: StudyProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupedCard(title = stringResource(R.string.settings_progress)) {
        when (val state = uiState) {
            StudyProgressUiState.Loading -> {
                GroupedCardItem(
                    title = stringResource(R.string.settings_loading),
                    subtitle = stringResource(R.string.settings_loading_desc),
                )
            }

            is StudyProgressUiState.Loaded -> {
                val entity = state.entity
                val streak = entity.streakDays ?: 0
                val totalSeconds = entity.totalStudyTime ?: 0
                val totalHours = totalSeconds / 3600
                val totalMinutes = (totalSeconds % 3600) / 60
                val timeText = if (totalHours > 0) {
                    "${totalHours}小时${totalMinutes}分钟"
                } else {
                    "${totalMinutes}分钟"
                }
                val lastVisited = entity.lastVisitedAt

                GroupedCardItem(
                    title = stringResource(R.string.settings_streak, streak),
                    subtitle = if (streak == 0) "开始今天的学习吧" else "保持下去！",
                )
                GroupedCardDivider()
                GroupedCardItem(
                    title = stringResource(R.string.settings_duration),
                    subtitle = timeText,
                )
                if (lastVisited != null) {
                    GroupedCardDivider()
                    GroupedCardItem(
                        title = stringResource(R.string.settings_last_study),
                        subtitle = formatRelativeTime(lastVisited),
                    )
                }
            }
        }
    }
}

/**
 * 将时间戳格式化为相对时间文本(如"3小时前"/"昨天"/"3天前")。
 *
 * v0.8.13 P1-1 修复:改为 internal 以便单元测试覆盖未来时间戳分支
 * (原 private 无法在测试中调用,导致该边界条件无测试保护)。
 */
internal fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp
    // v0.8.13 P1-1 修复:未来时间戳(时钟回拨或异常数据)直接返回"刚刚",
    // 避免下面计算结果为负数,显示"-3 分钟前"等异常文案。
    // 与 KnowledgePointDetailScreen.formatRelativeTime 保持一致(该处已修复)。
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
