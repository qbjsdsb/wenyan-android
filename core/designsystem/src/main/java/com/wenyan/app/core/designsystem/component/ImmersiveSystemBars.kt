package com.wenyan.app.core.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 沉浸式系统栏控制（v0.9.36 全屏模式，项目首个沉浸式先例）。
 *
 * [enabled]=true 时隐藏状态栏 + 导航栏（进入沉浸式），离开组合（onDispose）自动恢复。
 * - 基于 [WindowCompat.getInsetsController]（core-ktx，已在依赖链）
 * - window 级生效：隐藏后系统栏 insets 归零，所有 Scaffold 同步无 padding
 * - `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`：滑动边缘临时显示系统栏后自动隐藏，
 *   保留用户临时查看状态栏的能力
 *
 * 用法（配合全屏页 `contentWindowInsets = WindowInsets(0,0,0,0)`）：
 * ```kotlin
 * ImmersiveSystemBars(enabled = true)
 * ```
 */
@Composable
fun ImmersiveSystemBars(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = view.context.findActivity()?.window
            ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        if (enabled) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/** 从任意 Context 解包到宿主 [Activity]（处理 ContextThemeWrapper 等包装）。 */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
