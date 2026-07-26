package com.wenyan.app.core.common.util

/**
 * 将异常映射为面向用户的中文友好错误消息(v0.8.20 P1-2 抽取到 core/common)。
 *
 * 历史:
 * - v0.8.19 P1-1(KnowledgeViewModel)首次定义 internal fun,仅 feature/knowledge 模块可见。
 * - v0.8.20 P1-2 抽取到 core/common,作为 public API 供 feature/knowledge、feature/cards 等模块复用,
 *   避免每个 feature 模块各自实现一套错误映射(语义碎片化、维护成本高)。
 *
 * 设计原则:
 * - 不暴露原始英文堆栈或异常类名给用户
 * - 区分常见异常类型,提供针对性中文提示
 * - 兜底文案"加载失败,请重试"适用于未知异常
 *
 * 用法示例:
 * ```kotlin
 * .catch { e ->
 *     Log.e("XXX", "load failed", e)
 *     emit(UiState(error = friendlyErrorMessage(e)))
 * }
 * ```
 *
 * @param e 任意 Throwable
 * @return 面向用户的中文错误提示
 */
fun friendlyErrorMessage(e: Throwable): String = when {
    // 网络异常:SocketTimeoutException(连接/读取超时) + UnknownHostException(DNS 解析失败)
    // 提示用户检查网络后重试,而非展示英文堆栈
    e is java.net.SocketTimeoutException || e is java.net.UnknownHostException ->
        "网络超时,请检查网络后重试"

    // 本地数据库异常:SQLiteException(Room/SQLite 任意错误,如表不存在、约束冲突、磁盘满)
    // 提示重启 App,数据库错误通常需要冷启动恢复
    e is android.database.sqlite.SQLiteException ->
        "本地数据异常,请重启 App"

    // 协程超时:TimeoutCancellationException(withTimeout 超时)
    // 与 SocketTimeoutException 不同,这是协程级超时,通常是业务逻辑慢
    e is kotlinx.coroutines.TimeoutCancellationException ->
        "加载超时,请重试"

    // 数据库表不存在的特殊情况(message 包含 "no such table")
    // 通常发生在 DB schema 升级失败或 seed 数据未正确导入
    e.message != null && e.message!!.contains("no such table", ignoreCase = true) ->
        "数据库版本异常,请重启 App"

    // 兜底:未知异常统一显示友好提示
    else -> "加载失败,请重试"
}
