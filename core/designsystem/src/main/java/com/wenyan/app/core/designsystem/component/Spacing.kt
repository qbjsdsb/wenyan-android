package com.wenyan.app.core.designsystem.component

import androidx.compose.ui.unit.dp

/**
 * 间距 tokens（6 级），统一全 App 间距体系。
 *
 * - [xs]：4dp — 图标与文字间距
 * - [sm]：8dp — 卡片内元素间距
 * - [md]：12dp — 卡片间距、列表项间距
 * - [lg]：16dp — 屏幕边距、卡片内 padding
 * - [xl]：24dp — 区块间距
 * - [xxl]：32dp — 页面级间距
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/**
 * 内容最大宽度 tokens — 用于横屏/平板下限制内容宽度，避免行宽过宽导致阅读疲劳。
 *
 * v0.8.15 新增（Stage 1: 横屏内容宽度限制）。
 *
 * 设计依据：
 * - 经典排版学：单行 45-75 字符（含空格）为最佳阅读区，对应 400-600dp。
 * - Material 3 大屏指南：compact ≤ 600dp 不限制；medium/expanded 应限制内容宽度并居中。
 *
 * 差异化阈值（按内容性质）：
 * - [compact]：600dp — 设置项、聊天对话（窄内容）
 * - [comfortable]：720dp — 题目、知识点详情、配置卡片（中等密度内容）
 *
 * 使用模式：
 * ```
 * Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
 *     LazyColumn(modifier = Modifier.widthIn(max = MaxContentWidth.comfortable)) { ... }
 * }
 * ```
 *
 * 竖屏（<600dp）下 widthIn(max) 不生效，因为屏幕宽度 < max，所以**不影响竖屏布局**。
 */
object MaxContentWidth {
    /** 紧凑型：设置项、聊天对话（窄内容） */
    val compact = 600.dp

    /** 舒适型：题目、知识点、配置卡片（中等密度内容） */
    val comfortable = 720.dp
}
