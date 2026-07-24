package com.wenyan.app.core.designsystem.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/**
 * 文研App 主题入口（Material 3 Expressive）。
 *
 * 使用 [MaterialExpressiveTheme] + [MotionScheme.expressive] 实现 M3 Expressive 设计语言。
 * 颜色方案由以下优先级生成：
 * 1. Android 12+ 且 [ThemeConfig.dynamicColor] 开启 → 系统壁纸动态色彩
 * 2. 其他情况 → materialkolor 从种子色生成（SPEC_2025 规范）
 *
 * AMOLED 模式在深色模式下将底层表面替换为纯黑，节省 OLED 电量。
 *
 * @param config 主题配置
 * @param content 可组合内容
 */
@Composable
fun WenyanTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = when (config.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }

    // 生成基础 ColorScheme
    // NF-UC2 修复：dynamicLightColorScheme/dynamicDarkColorScheme 内部读取系统资源，
    // 不 remember 时每次重组都重新构建，Android 12+ 用户产生不必要的 GC 压力。
    // rememberDynamicColorScheme 本身已 remember（materialkolor 库），
    // 但 dynamicLightColorScheme/dynamicDarkColorScheme 是普通函数，需显式 remember。
    // 因 remember 的 value lambda 不能调用 @Composable 函数，
    // 用 if 分支分别 remember 对应方案。
    val baseScheme = if (config.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+：使用系统壁纸提取的动态色彩
        remember(context, isDark) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    } else {
        // Android 11- 或手动种子色：用 materialkolor 生成（内部已 remember）
        val paletteStyle = config.paletteStyle.toMaterialKolorStyle()
        rememberDynamicColorScheme(
            seedColor = config.seedColor,
            isDark = isDark,
            style = paletteStyle,
            specVersion = if (paletteStyle.supportsSpec2025) {
                ColorSpec.SpecVersion.SPEC_2025
            } else {
                ColorSpec.SpecVersion.SPEC_2021
            },
        )
    }

    // AMOLED 模式：将底层表面替换为纯黑
    // NF-UP1 修复：baseScheme.copy 创建 36 字段的新 ColorScheme，每次重组都重建。
    // remember(baseScheme, isDark, config.amoledMode) 保证仅在依赖变化时重建。
    //
    // v0.8.4 修复（P1）：AMOLED 模式替换不完整。
    // 原仅替换 6 个 surface 字段（background/surface/surfaceDim/surfaceContainerLowest/Low/Container），
    // 缺失 surfaceContainerHigh/Highest/Bright。导致 TonalCard（用 surfaceBright）、
    // ContentSourceBadge（用 surfaceContainerHigh）在 AMOLED 纯黑背景下仍显示 M3 默认深灰，
    // 与全黑背景对比突兀，破坏 AMOLED 一致性。
    // 现补充三个高层 surface 为深灰渐变（非纯黑），保持卡片层次可见性同时省电。
    val finalScheme = remember(baseScheme, isDark, config.amoledMode) {
        if (isDark && config.amoledMode) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color.Black,
                surfaceContainer = Color.Black,
                // v0.8.4 新增：高层容器保持微亮深灰，确保卡片在纯黑背景上仍可区分层次
                surfaceContainerHigh = Color(0xFF1A1A1A),
                surfaceContainerHighest = Color(0xFF242424),
                surfaceBright = Color(0xFF2E2E2E),
            )
        } else {
            baseScheme
        }
    }

    // v0.6：颜色切换动画。主题切换（深色↔浅色、种子色变化、AMOLED 开关）时
    // 颜色平滑过渡而非瞬间跳变，符合 M3 Expressive 的"持续运动"原则。
    //
    // v0.8.4 优化（P2）：原 LowBouncy(0.75) 有过冲 + StiffnessLow(200f) ~600ms 过长，
    // 用户感觉迟钝。改为 NoBouncy(1.0) 无过冲 + StiffnessMediumLow(400f) ~300ms，
    // 符合 M3 DurationMedium4 推荐时长，过渡更干脆。
    val animatedScheme = animateColorScheme(finalScheme)

    MaterialExpressiveTheme(
        colorScheme = animatedScheme,
        motionScheme = MotionScheme.expressive(),
        typography = WenyanTypography,
        shapes = WenyanShapes,
        content = content,
    )
}

/**
 * 对 [ColorScheme] 的每个颜色角色做弹簧动画过渡。
 *
 * v0.8.4 优化：
 * - dampingRatio 从 LowBouncy(0.75) 改为 NoBouncy(1.0)，去除过冲（主题切换不应"弹"）
 * - stiffness 从 Low(200f) 改为 MediumLow(400f)，过渡 ~300ms，符合 M3 DurationMedium4
 *
 * 注意：调用时需在 @Composable 上下文中，每个 [animateColorAsState] 独立 remember。
 */
@Composable
private fun animateColorScheme(scheme: ColorScheme): ColorScheme {
    val spec = spring<Color>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    val primary by animateColorAsState(scheme.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(scheme.onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(scheme.primaryContainer, spec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(scheme.onPrimaryContainer, spec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(scheme.secondary, spec, label = "secondary")
    val onSecondary by animateColorAsState(scheme.onSecondary, spec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(scheme.secondaryContainer, spec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(scheme.onSecondaryContainer, spec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(scheme.tertiary, spec, label = "tertiary")
    val onTertiary by animateColorAsState(scheme.onTertiary, spec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(scheme.tertiaryContainer, spec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(scheme.onTertiaryContainer, spec, label = "onTertiaryContainer")
    val error by animateColorAsState(scheme.error, spec, label = "error")
    val onError by animateColorAsState(scheme.onError, spec, label = "onError")
    val errorContainer by animateColorAsState(scheme.errorContainer, spec, label = "errorContainer")
    val onErrorContainer by animateColorAsState(scheme.onErrorContainer, spec, label = "onErrorContainer")
    val background by animateColorAsState(scheme.background, spec, label = "background")
    val onBackground by animateColorAsState(scheme.onBackground, spec, label = "onBackground")
    val surface by animateColorAsState(scheme.surface, spec, label = "surface")
    val onSurface by animateColorAsState(scheme.onSurface, spec, label = "onSurface")
    val surfaceVariant by animateColorAsState(scheme.surfaceVariant, spec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(scheme.onSurfaceVariant, spec, label = "onSurfaceVariant")
    val surfaceTint by animateColorAsState(scheme.surfaceTint, spec, label = "surfaceTint")
    val inverseSurface by animateColorAsState(scheme.inverseSurface, spec, label = "inverseSurface")
    val inversePrimary by animateColorAsState(scheme.inversePrimary, spec, label = "inversePrimary")
    val outline by animateColorAsState(scheme.outline, spec, label = "outline")
    val outlineVariant by animateColorAsState(scheme.outlineVariant, spec, label = "outlineVariant")
    val scrim by animateColorAsState(scheme.scrim, spec, label = "scrim")
    val surfaceDim by animateColorAsState(scheme.surfaceDim, spec, label = "surfaceDim")
    val surfaceBright by animateColorAsState(scheme.surfaceBright, spec, label = "surfaceBright")
    val surfaceContainerLowest by animateColorAsState(scheme.surfaceContainerLowest, spec, label = "surfaceContainerLowest")
    val surfaceContainerLow by animateColorAsState(scheme.surfaceContainerLow, spec, label = "surfaceContainerLow")
    val surfaceContainer by animateColorAsState(scheme.surfaceContainer, spec, label = "surfaceContainer")
    val surfaceContainerHigh by animateColorAsState(scheme.surfaceContainerHigh, spec, label = "surfaceContainerHigh")
    val surfaceContainerHighest by animateColorAsState(scheme.surfaceContainerHighest, spec, label = "surfaceContainerHighest")

    return scheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inversePrimary = inversePrimary,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceDim = surfaceDim,
        surfaceBright = surfaceBright,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}
