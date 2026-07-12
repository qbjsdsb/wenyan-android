package com.wenyan.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 文研App 形状系统（Material 3 Expressive 圆角规范）。
 *
 * - [extraSmall]：4dp — InfoChip、小标签
 * - [small]：8dp — FAB、小按钮
 * - [medium]：12dp — Card、Dialog（M3 标准）
 * - [large]：16dp — TonalCard、大卡片
 * - [extraLarge]：28dp — BottomSheet、大型 Dialog
 */
val WenyanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
