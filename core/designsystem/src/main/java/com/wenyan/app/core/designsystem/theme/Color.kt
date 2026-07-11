package com.wenyan.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// 文研App 主色调：以"墨色 + 宣纸"为视觉基调，呼应文学考研主题

// 主色（墨色系）
val WenyanPrimary = Color(0xFF2C2C2C)
val WenyanOnPrimary = Color(0xFFF5F1E8)
val WenyanPrimaryContainer = Color(0xFF454545)
val WenyanOnPrimaryContainer = Color(0xFFE8E0D0)

// 次要色（赭石 / 印章红）
val WenyanSecondary = Color(0xFF9C5A3C)
val WenyanOnSecondary = Color(0xFFF5F1E8)
val WenyanSecondaryContainer = Color(0xFFE8D5C4)
val WenyanOnSecondaryContainer = Color(0xFF3D2418)

// 第三色（竹青）
val WenyanTertiary = Color(0xFF5A7A5A)
val WenyanOnTertiary = Color(0xFFF5F1E8)

// 背景 / 表面（宣纸色）
val WenyanBackground = Color(0xFFF5F1E8)
val WenyanOnBackground = Color(0xFF2C2C2C)
val WenyanSurface = Color(0xFFFAF7F0)
val WenyanOnSurface = Color(0xFF2C2C2C)
val WenyanSurfaceVariant = Color(0xFFE8E0D0)
val WenyanOnSurfaceVariant = Color(0xFF4A4A4A)

// 错误 / 状态色
val WenyanError = Color(0xFFB3261E)
val WenyanOnError = Color(0xFFFFFFFF)

// 内容来源标注色（参考 Spec 五级标注 + MISSING 特殊状态）
val SourceTextbook = Color(0xFF4CAF50)   // 资料绿色（TEXTBOOK_NATIVE / TEXTBOOK_OCR）
val SourceAi = Color(0xFF2196F3)         // AI 蓝色（AI_GENERATED）
val SourceHybrid = Color(0xFFFFC107)     // 资料+AI 黄色（HYBRID）
val SourceUser = Color(0xFF9E9E9E)       // 我的 灰色（USER_CREATED）
val SourceMissing = Color(0xFFF44336)    // 缺失 红色（MISSING）
