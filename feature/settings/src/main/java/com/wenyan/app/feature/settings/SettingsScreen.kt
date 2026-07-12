package com.wenyan.app.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.ThemeViewModel
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.SectionHeader
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanTopAppBar
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.WenyanPaletteStyle

/**
 * 设置页面。
 *
 * 包含：外观（主题模式/AMOLED）、动态色彩（开关/种子色/调色板风格）、AI 服务、关于。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToApiConfig: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel(),
) {
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()

    ExpressiveScaffold(
        topBar = {
            WenyanTopAppBar(
                title = "设置",
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            // 外观
            item { SectionHeader(title = "外观") }

            // 主题模式选择
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = "主题模式",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
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
                }
            }

            // AMOLED 开关
            item {
                SwitchItem(
                    title = "AMOLED 纯黑模式",
                    description = "深色模式下使用纯黑背景，节省 OLED 电量",
                    checked = themeConfig.amoledMode,
                    onCheckedChange = { viewModel.setAmoledMode(it) },
                )
            }

            // 动态色彩
            item { SectionHeader(title = "动态色彩") }

            // 动态色彩开关
            item {
                SwitchItem(
                    title = "动态色彩",
                    description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "跟随系统壁纸自动生成色彩"
                    } else {
                        "需要 Android 12 及以上"
                    },
                    checked = themeConfig.dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                )
            }

            // 种子色选择（动态色彩关闭时可用）
            if (!themeConfig.dynamicColor) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = "种子色",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
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
                    }
                }
            }

            // 调色板风格
            if (!themeConfig.dynamicColor) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = "调色板风格",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
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

            // AI 服务
            item { SectionHeader(title = "AI 服务") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "API 配置",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = onNavigateToApiConfig) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "API 配置",
                        )
                    }
                }
            }

            // 关于
            item { SectionHeader(title = "关于") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "版本",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "v0.1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
