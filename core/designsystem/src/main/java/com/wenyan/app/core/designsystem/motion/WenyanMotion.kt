package com.wenyan.app.core.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * 文研 App 统一动画 tokens。
 *
 * v0.6：Push/Pop 子路由过渡引入弹簧物理，让运动有"弹性重量感"，
 * 符合 M3 Expressive 的 MotionScheme.expressive() 设计语言。
 *
 * 遵循 Material Motion 准则：
 * - 短时长（150ms）用于微交互（按钮、状态切换）
 * - 中时长（300ms）用于页面切换、Tab 切换、fade 配合
 * - 弹簧（spring）用于子路由 push/pop 等需"重量感"的位移动画
 * - Emphasized 缓动保留用于非弹簧场景的备用
 *
 * 使用方式：
 * - 顶级 Tab 切换用 [TabEnterTransition] / [TabExitTransition]（纯 fade，无位移）
 * - 子路由 push 用 [PushEnterTransition] / [PushExitTransition]（弹簧滑入 + fade）
 * - 子路由 pop 用 [PopEnterTransition] / [PopExitTransition]（弹簧滑出 + fade）
 * - 自定义动画直接引用 [DurationShort] / [DurationMedium] + 缓动常量
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

    /** 中时长（页面切换、卡片翻转、Tab 切换、fade 配合） */
    const val DurationMedium = 300

    /** Push/Pop 位移弹簧：dampingRatio=0.8f 轻微过冲，StiffnessMediumLow 让运动稍慢有重量感 */
    private const val PushPopDampingRatio = 0.8f
    private val PushPopSpringSpec = spring<IntOffset>(
        dampingRatio = PushPopDampingRatio,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** 顶级 Tab 切换：纯 fade，无 scale/位移，避免与 NavigationBar indicator 动画冲突 */
    val TabEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(DurationMedium, easing = DecelerateEasing),
    )

    val TabExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(DurationMedium, easing = AccelerateEasing),
    )

    /**
     * 子路由 push：从右侧弹簧滑入（模拟"前进"到一个新页面）。
     * v0.6：slide 改用 spring（物理感），fade 保持 tween（alpha 不适合弹簧过冲）。
     */
    val PushEnterTransition: EnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = PushPopSpringSpec,
    ) + fadeIn(
        animationSpec = tween(DurationMedium, easing = DecelerateEasing),
    )

    val PushExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(DurationMedium, easing = AccelerateEasing),
    )

    /**
     * 子路由 pop：向右侧弹簧滑出（模拟"后退"回上一个页面）。
     * v0.6：slide 改用 spring（物理感），fade 保持 tween。
     */
    val PopEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(DurationMedium, easing = DecelerateEasing),
    )

    val PopExitTransition: ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = PushPopSpringSpec,
    ) + fadeOut(
        animationSpec = tween(DurationMedium, easing = AccelerateEasing),
    )
}
