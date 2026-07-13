package com.wenyan.app.core.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * 文研 App 统一动画 tokens。
 *
 * 遵循 Material Motion 准则：
 * - 短时长（150ms）用于微交互（按钮、状态切换）
 * - 中时长（300ms）用于页面切换、Tab 切换
 * - 长时长（450ms）用于子路由 push/pop 等需"重量感"的过渡
 * - Emphasized 缓动让运动有"重量感"，Decelerate 用于入场，Accelerate 用于退场
 *
 * 使用方式：
 * - 顶级 Tab 切换用 [TabEnterTransition] / [TabExitTransition]（纯 fade，无位移）
 * - 子路由 push 用 [PushEnterTransition] / [PushExitTransition]（从右滑入 + fade）
 * - 子路由 pop 用 [PopEnterTransition] / [PopExitTransition]（向右滑出 + fade）
 * - 自定义动画直接引用 [DurationShort] / [DurationMedium] / [DurationLong] + 缓动常量
 */
object WenyanMotion {
    /** Emphasized 缓动（标准 Material 缓动，有轻微过冲感） */
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Decelerate 缓动（用于入场：起步快，结束慢） */
    val DecelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Accelerate 缓动（用于退场：起步慢，结束快） */
    val AccelerateEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** 短时长（微交互：按钮、状态切换） */
    const val DurationShort = 150

    /** 中时长（页面切换、卡片翻转、Tab 切换） */
    const val DurationMedium = 300

    /** 长时长（子路由 push/pop 等复杂过渡） */
    const val DurationLong = 450

    /** 顶级 Tab 切换：纯 fade，无 scale/位移，避免与 NavigationBar indicator 动画冲突 */
    val TabEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(DurationMedium, easing = DecelerateEasing),
    )

    val TabExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(DurationMedium, easing = AccelerateEasing),
    )

    /** 子路由 push：从右侧滑入（模拟"前进"到一个新页面） */
    val PushEnterTransition: EnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(DurationLong, easing = EmphasizedEasing),
    ) + fadeIn(
        animationSpec = tween(DurationLong, easing = DecelerateEasing),
    )

    val PushExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(DurationLong, easing = AccelerateEasing),
    )

    /** 子路由 pop：向右侧滑出（模拟"后退"回上一个页面） */
    val PopEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(DurationLong, easing = DecelerateEasing),
    )

    val PopExitTransition: ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(DurationLong, easing = EmphasizedEasing),
    ) + fadeOut(
        animationSpec = tween(DurationLong, easing = AccelerateEasing),
    )
}
