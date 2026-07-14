package com.wenyan.app.feature.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.ThemeViewModel
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle
import com.wenyan.app.feature.settings.BuildConfig

/**
 * 设置页面。
 *
 * v0.6 起从子路由提升为顶级 Tab，不再需要 onBack 返回箭头。
 *
 * 包含：外观（主题模式/AMOLED）、动态色彩（开关/种子色/调色板风格）、AI 服务、关于。
 */
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
                    // 主题模式选择 chips（在卡片内独立一行）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        ColorMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeConfig.colorMode == mode,
                                onClick = { viewModel.setColorMode(mode) },
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "种子色",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val seedColors = listOf(
                                    Color(0xFF6750A4), // 紫
                                    Color(0xFF0061A4), // 蓝
                                    Color(0xFF006C4C), // 绿
                                    Color(0xFF9C4146), // 红
                                    Color(0xFF7C5800), // 棕
                                )
                                seedColors.forEach { color ->
                                    FilterChip(
                                        selected = themeConfig.seedColor == color,
                                        onClick = { viewModel.setSeedColor(color) },
                                        label = {},
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = null,
                                                tint = color,
                                            )
                                        },
                                    )
                                }
                            }
                            GroupedCardDivider()
                            // 调色板风格
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "风格",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                WenyanPaletteStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = themeConfig.paletteStyle == style,
                                        onClick = { viewModel.setPaletteStyle(style) },
                                        label = {
                                            Text(
                                                text = when (style) {
                                                    WenyanPaletteStyle.TONAL_SPOT -> "Tonal Spot"
                                                    WenyanPaletteStyle.NEUTRAL -> "Neutral"
                                                    WenyanPaletteStyle.VIBRANT -> "Vibrant"
                                                    WenyanPaletteStyle.EXPRESSIVE -> "Expressive"
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
