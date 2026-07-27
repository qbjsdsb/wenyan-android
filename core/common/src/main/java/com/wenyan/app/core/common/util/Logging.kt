package com.wenyan.app.core.common.util

import android.util.Log
import timber.log.Timber

/**
 * Timber 结构化日志统一入口（v0.8.21 引入，替换分散的 android.util.Log 调用）。
 *
 * ## 设计目标
 *
 * 1. **统一日志入口**：所有模块通过 `Timber.e/w/i/d` 写日志，不再直接调用 `android.util.Log`。
 *    - 调用栈自动推断 tag（默认取调用类名，无需每个文件定义 `private val TAG = "XXX"`）。
 *    - 异常与消息一体化：`Timber.e(e, "load failed")` 自动拼接 throwable 堆栈。
 *
 * 2. **Debug/Release 差异化**：
 *    - Debug 构建：[Timber.DebugTree] 全量打印到 Logcat（V/D/I/W/E 全级别）。
 *    - Release 构建：[ReleaseTree] 仅记录 WARN/ERROR，过滤 verbose 噪声，降低性能开销。
 *      后续可在此扩展为转发 Crashlytics / Firebase 等崩溃上报通道。
 *
 * 3. **单元测试零依赖**：
 *    - 测试不调用 [initTimber]，则 `Timber.forest()` 为空，所有 `Timber.x()` 调用为 no-op。
 *    - 这从根本上解决了 JVM 单元测试中 `android.util.Log` "not mocked" 抛 RuntimeException 的问题，
 *      无需在每个模块的 `testOptions.unitTests.isReturnDefaultValues` 中加 `true`。
 *
 * ## 使用方式
 *
 * ```kotlin
 * // Application.onCreate 中初始化（仅需一次）
 * initTimber(BuildConfig.DEBUG)
 *
 * // 任意位置调用（无需定义 TAG，自动推断）
 * Timber.e(e, "Seed data load failed")
 * Timber.w("Cache miss for key=$key")
 * Timber.d("Card flipped: index=$index")
 * ```
 *
 * @param isDebug 是否为 Debug 构建。传入 `BuildConfig.DEBUG`（app 模块可见）。
 */
fun initTimber(isDebug: Boolean) {
    if (isDebug) {
        Timber.plant(Timber.DebugTree())
    } else {
        Timber.plant(ReleaseTree())
    }
}

/**
 * Release 构建 Tree：仅记录 WARN/ERROR，避免 verbose 日志泄漏性能与隐私信息。
 *
 * 实现要点：
 * - [log] 仅在 priority ≥ WARN 时调用 `Log.println`，其余直接 return。
 * - 后续接入 Crashlytics 时，可在此处 `FirebaseCrashlytics.getInstance().recordException(t)`。
 * - 不重写 [log] 的 tag 推断：Timber 默认通过调用栈自动取类名作为 tag。
 */
private class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Release 构建只记录 WARN 及以上级别
        if (priority < Log.WARN) return
        Log.println(priority, tag, message)
        // TODO(v0.9.x): 接入 Crashlytics 后在此处上报
        // if (priority == Log.ERROR && t != null) {
        //     FirebaseCrashlytics.getInstance().recordException(t)
        // }
    }
}
