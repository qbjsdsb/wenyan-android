package com.wenyan.app.feature.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeViewModel
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import com.wenyan.app.core.fsrs.ExamCountdownManager
import com.wenyan.app.core.fsrs.StudyPhase
import com.wenyan.app.feature.settings.BuildConfig

/**
 * 设置页面。
 *
 * v0.6 起从子路由提升为顶级 Tab，不再需要 onBack 返回箭头。
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToApiConfig: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel(),
) {
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "设置",
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            // P0 v0.7.2: 考研倒计时卡片(接通 ExamCountdownManager,原完全未接入)
            item { ExamCountdownCard() }

            // P0 v0.7.2: 学习进度卡片(接通 study_progress 表,原死表)
            item { StudyProgressCard() }

            // 外观
            item {
                GroupedCard(title = "外观") {
                    // 主题模式
                    GroupedCardItem(
                        title = "主题模式",
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
                        title = "AMOLED 纯黑模式",
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

            // 动态色彩
            item {
                GroupedCard(title = "动态色彩") {
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
                                                tint = preset.color,
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
                GroupedCard(title = "AI 服务") {
                    GroupedCardItem(
                        title = "API 配置",
                        subtitle = "DeepSeek / 通义 / 智谱 / 月之暗面",
                        onClick = onNavigateToApiConfig,
                    )
                }
            }

            // 关于
            item {
                GroupedCard(title = "关于") {
                    GroupedCardItem(
                        title = "版本",
                        // P1-M2 修正：原硬编码 "v0.1.0" 与实际版本脱节，改读 BuildConfig.VERSION_NAME
                        subtitle = "v${BuildConfig.VERSION_NAME}",
                    )
                }
            }
        }
    }
}

// ── 考研倒计时卡片(P0 v0.7.2:接通 ExamCountdownManager) ──────────

/**
 * 考研倒计时与学习阶段展示卡片。
 *
 * 接通 [com.wenyan.app.core.fsrs.ExamCountdownManager](原完全未接入生产代码)。
 * 展示:距考研天数、当前学习阶段(基础/强化/冲刺)、目标保持率。
 */
@Composable
private fun ExamCountdownCard() {
    val today = remember { java.time.LocalDate.now() }
    val daysToExam = remember { ExamCountdownManager.getDaysToExam(today) }
    val phase = remember { ExamCountdownManager.getStudyPhase(daysToExam) }
    val retention = remember { ExamCountdownManager.getGlobalRetention(daysToExam) }
    val examDate = remember {
        val currentYearExam = ExamCountdownManager.getExamDate(today.year)
        if (today.isAfter(currentYearExam)) {
            ExamCountdownManager.getExamDate(today.year + 1)
        } else {
            currentYearExam
        }
    }

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

    GroupedCard(title = "考研倒计时") {
        GroupedCardItem(
            title = "距考研还有 $daysToExam 天",
            subtitle = "考试日期：${examDate.year}年${examDate.monthValue}月${examDate.dayOfMonth}日",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = phaseLabel,
            subtitle = phaseSubtitle,
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "目标保持率",
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
 */
@Composable
private fun StudyProgressCard(
    viewModel: StudyProgressViewModel = hiltViewModel(),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val streak = progress?.streakDays ?: 0
    val totalSeconds = progress?.totalStudyTime ?: 0
    val totalHours = totalSeconds / 3600
    val totalMinutes = (totalSeconds % 3600) / 60
    val timeText = if (totalHours > 0) {
        "${totalHours}小时${totalMinutes}分钟"
    } else {
        "${totalMinutes}分钟"
    }
    val lastVisited = progress?.lastVisitedAt

    GroupedCard(title = "学习进度") {
        GroupedCardItem(
            title = "连续学习 $streak 天",
            subtitle = if (streak == 0) "开始今天的学习吧" else "保持下去！",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "累计学习时长",
            subtitle = timeText,
        )
        if (lastVisited != null) {
            GroupedCardDivider()
            GroupedCardItem(
                title = "上次学习",
                subtitle = formatRelativeTime(lastVisited),
            )
        }
    }
}

/** 将时间戳格式化为相对时间文本(如"3小时前"/"昨天"/"3天前") */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp
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
