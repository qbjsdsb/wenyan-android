package com.wenyan.app.core.data.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber

/**
 * Flow 异常处理扩展函数（P1 审计修复）。
 *
 * 问题：Repository 层 23 个 Flow 方法无 .catch，DAO observe / combine / map lambda
 * 内异常会直接传播到 ViewModel collect，导致 UI 永久 failed 状态（白屏/错误界面）。
 *
 * 修复策略：对有数据处理逻辑的 Flow（非裸 DAO observe）加 .catch，
 * 记录日志 + emit 降级值，确保 UI 至少显示空状态而非崩溃。
 *
 * v0.8.21：内部 Log.e 改为 Timber.tag(tag).e，统一到 Timber 日志通道。
 * - 显式传入 tag：catchAndLog 是工具函数，若依赖 Timber 自动推断会得到 "FlowExt"
 *   而非调用者类名，丢失可读性。显式 tag 保证日志归属正确（如 "ReviewRepositoryImpl"）。
 * - Release 构建经 [com.wenyan.app.core.common.util.ReleaseTree] 过滤 V/D/I，
 *   仅记录 WARN/ERROR，降低性能开销。
 *
 * 使用方式：
 * ```kotlin
 * flow.catchAndLog(TAG, "getReviewQueue") { emptyList() }
 * ```
 *
 * @param tag 日志 tag（通常为调用类的 TAG 常量，用于日志归类定位）
 * @param operation 失败的操作名（通常为方法名），用于日志定位
 * @param fallback 降级值工厂（异常时 emit 的值）
 */
fun <T> Flow<T>.catchAndLog(
    tag: String,
    operation: String,
    fallback: () -> T,
): Flow<T> = catch { e ->
    Timber.tag(tag).e(e, "$operation failed: ${e.message}")
    emit(fallback())
}
