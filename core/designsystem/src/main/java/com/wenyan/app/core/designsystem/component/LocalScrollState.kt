package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal 共享当前顶级页面的 [LazyListState]。
 *
 * 由每个顶级 Screen（KnowledgeScreen / QuizScreen / WrongAnswerScreen / SettingsScreen）
 * 在 [LazyColumn] 创建时通过 [CompositionLocalProvider] 提供。
 *
 * 由 [WenyanAdaptiveNavigation] 读取，驱动底部导航栏的滚动感知显隐（scroll-aware visibility）。
 *
 * 设计参照 KernelSU Next 的 scroll-aware 底部导航栏模式：
 * - 下滑内容时隐藏导航栏（spring 动画移出屏幕）
 * - 上滑内容时显示导航栏（spring 动画移入屏幕）
 *
 * null 表示当前页面没有可滚动的 LazyColumn（如 CardsScreen），
 * 此时导航栏保持可见、不做滚动感知。
 */
val LocalLazyListState = compositionLocalOf<LazyListState?> { null }