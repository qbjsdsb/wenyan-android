package com.wenyan.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 文研App 形状系统（Material 3 Expressive 圆角规范）。
 *
 * v0.6：extraLarge 从 28dp 提升到 32dp，让 BottomSheet / 大型 Dialog 圆角更夸张，
 * 符合 M3 Expressive 的"形状张力"理念（大容器用更明显的圆角，与 medium 拉开层次）。
 *
 * - [extraSmall]：4dp — InfoChip、小标签
 * - [small]：8dp — FAB、小按钮
 * - [medium]：12dp — Card、Dialog（M3 标准）
 * - [large]：16dp — TonalCard、大卡片
 * - [extraLarge]：32dp — BottomSheet、大型 Dialog（M3 Expressive 张力）
 */
val WenyanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
