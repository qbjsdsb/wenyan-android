package com.wenyan.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 文研App 字体排版（Material 3 Expressive 完整 15 样式）。
 *
 * v0.6：Display 与 Headline 改用 SemiBold 字重，与 Title/Body 的 Medium/Normal
 * 形成更鲜明的字重对比，制造 M3 Expressive 鼓励的"视觉张力"。
 *
 * 参考 M3 设计规范：
 * https://m3.material.io/styles/typography/type-scale-tokens
 */
val WenyanTypography = Typography(
    // Display — 超大标题（页面级，极少使用）。v0.6：SemiBold 制造视觉张力
    displayLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    // Headline — 大标题。v0.6：SemiBold 与 Body Normal 形成对比
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    // Title — 标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Body — 正文
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Label — 标签
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        // v0.7.4 修复：11.sp 低于 WCAG 推荐最小可读字号 12.sp，
        // 图例/统计/页码等关键信息视力不佳用户难阅读。升至 12.sp 与 labelMedium 一致字号、
        // 仅以字重差异区分；同时与 GraphCanvas 节点标签字号统一，避免视觉碎片化。
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
