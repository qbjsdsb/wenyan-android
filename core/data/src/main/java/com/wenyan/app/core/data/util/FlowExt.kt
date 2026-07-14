package com.wenyan.app.core.data.util

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Flow 异常处理扩展函数（P1 审计修复）。
 *
 * 问题：Repository 层 23 个 Flow 方法无 .catch，DAO observe / combine / map lambda
 * 内异常会直接传播到 ViewModel collect，导致 UI 永久 failed 状态（白屏/错误界面）。
 *
 * 修复策略：对有数据处理逻辑的 Flow（非裸 DAO observe）加 .catch，
 * 记录日志 + emit 降级值，确保 UI 至少显示空状态而非崩溃。
 *
 * 使用方式：
 * ```kotlin
 * flow.catchAndLog("ReviewRepository", "getReviewQueue") { emptyList() }
 * ```
 *
 * @param tag 日志 TAG（通常为类名）
 * @param operation 失败的操作名（通常为方法名），用于日志定位
 * @param fallback 降级值工厂（异常时 emit 的值）
 */
fun <T> Flow<T>.catchAndLog(
    tag: String,
    operation: String,
    fallback: () -> T,
): Flow<T> = catch { e ->
    Log.e(tag, "$operation failed: ${e.message}", e)
    emit(fallback())
}
