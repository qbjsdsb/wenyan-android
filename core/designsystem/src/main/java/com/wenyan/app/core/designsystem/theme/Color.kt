package com.wenyan.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// 文研App 颜色系统
//
// 所有颜色角色由 materialkolor 的 dynamicColorScheme() 在运行时生成，
// 不再使用硬编码颜色常量。
//
// 内容来源五级标注的颜色映射在 ContentSourceBadge.kt 中通过
// MaterialTheme.colorScheme 角色色实现，不在此处定义常量。

/** 默认种子色（Material 3 经典紫色） */
val DefaultSeedColor = Color(0xFF6750A4)

// ---------------------------------------------------------------------------
// 临时保留：内容来源标注色
//
// 以下常量仍被 ContentSourceBadge.kt 引用，将在 Task 16 重构
// ContentSourceBadge 改用 MaterialTheme.colorScheme 角色色后移除。
// ---------------------------------------------------------------------------

/** 资料绿色（TEXTBOOK_NATIVE / TEXTBOOK_OCR） — 临时常量，Task 16 移除 */
val SourceTextbook = Color(0xFF4CAF50)

/** AI 蓝色（AI_GENERATED） — 临时常量，Task 16 移除 */
val SourceAi = Color(0xFF2196F3)

/** 资料+AI 黄色（HYBRID） — 临时常量，Task 16 移除 */
val SourceHybrid = Color(0xFFFFC107)

/** 我的 灰色（USER_CREATED） — 临时常量，Task 16 移除 */
val SourceUser = Color(0xFF9E9E9E)

/** 缺失 红色（MISSING） — 临时常量，Task 16 移除 */
val SourceMissing = Color(0xFFF44336)
