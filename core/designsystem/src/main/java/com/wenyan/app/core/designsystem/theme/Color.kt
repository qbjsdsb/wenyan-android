package com.wenyan.app.core.designsystem.theme

// v0.8.4 清理：DefaultSeedColor 已废弃（NF-DS10 修复后默认种子色统一从 ThemeConfig.seedColor 取值）。
// 原定义 `val DefaultSeedColor = Color(0xFF6750A4)` 经 Grep 确认全项目无代码引用，
// 与 ThemeConfig 的 seedColor 默认值重复定义，存在单一来源真相问题。已删除。
//
// 如需引用默认种子色，使用 ThemeConfig().seedColor。
