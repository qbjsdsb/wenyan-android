package com.wenyan.app

import android.app.Application
import android.os.StrictMode
import com.wenyan.app.core.common.util.initTimber
import com.wenyan.app.core.data.seed.SeedDataLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

/**
 * 文研App Application 入口。
 *
 * 标注 @HiltAndroidApp 触发 Hilt 代码生成，创建依赖注入容器。
 * 整个应用的依赖图以此为根。
 *
 * 启动时异步加载种子数据（首次启动从 assets/seed_data.json 导入到 Room）。
 * 加载在 IO 调度器执行，不阻塞 onCreate；各 ViewModel 通过 Flow 观察数据库，
 * 数据导入完成后自动刷新 UI。
 *
 * 异常处理：用 CoroutineExceptionHandler 捕获种子加载异常，避免 App 崩溃。
 * SupervisorJob 只阻断异常向父 Job 传播，但不阻止异常本身被抛出；
 * launch 根协程未捕获异常会经 Thread.uncaughtExceptionHandler 处理，
 * Android 默认会导致 App 崩溃，因此必须显式加异常处理器。
 *
 * StrictMode（NF-S1 修复）：debug 构建启用，检测主线程 IO/网络违规与
 * 内存泄漏（Activity/SQLite cursor/closeable 未关闭等），违规只 penaltyLog
 * 不 penaltyDeath，避免开发期阻断调试。release 构建不启用，零运行时开销。
 *
 * Timber 结构化日志（v0.8.21）：onCreate 早期初始化，先于种子加载与其他模块日志。
 * Debug 构建 plant DebugTree（全级别打印 Logcat），Release 构建 plant ReleaseTree（仅 WARN/ERROR）。
 */
@HiltAndroidApp
class WenyanApplication : Application() {

    @Inject
    lateinit var seedDataLoader: SeedDataLoader

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "Seed data load failed")
    }

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + exceptionHandler,
    )

    override fun onCreate() {
        // v0.8.21: Timber 必须在 super.onCreate 之前初始化，
        // 否则 Application/Activity 早期初始化中的日志（如 StrictMode penaltyLog）
        // 仍走 android.util.Log，无法统一到 Timber 通道。
        initTimber(BuildConfig.DEBUG)

        // NF-S1 修复：StrictMode 必须在 super.onCreate 之前设置，
        // 否则 Application/Activity 早期初始化中的违规无法被捕获。
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
        }
        super.onCreate()
        applicationScope.launch {
            // P1 修正：种子加载加超时保护 + 重试机制。
            // v0.7.1 修正：30s → 120s。909 知识点（含 study_text）+ 909 写作素材 +
            // 481 真题的 JSON 解析 + 事务导入，30 秒在低端设备上不够，导致超时后
            // 异常被吞、知识点为空（详见 SESSION_LOG v0.7.1 修复）。
            // v0.9.19 修正：120s → 300s + 1 次重试。v0.9.18 用户反馈更新后知识点
            // 数据丢失（删除重装后恢复），根因是 120s 超时后直接失败，不重试，
            // 导致 App 启动后数据库为空。300s 覆盖低端设备首次加载，1 次重试
            // 覆盖偶发 I/O 抖动（如后台系统更新/媒体扫描占满闪存带宽）。
            var retryCount = 0
            val maxRetries = 1
            while (retryCount <= maxRetries) {
                try {
                    withTimeout(300_000L) { // 5 分钟
                        seedDataLoader.ensureSeedDataLoaded()
                    }
                    break
                } catch (e: TimeoutCancellationException) {
                    if (retryCount < maxRetries) {
                        Timber.i("Seed data load timed out, retrying (attempt ${retryCount + 1})")
                        retryCount++
                    } else {
                        Timber.e(e, "Seed data load failed after ${maxRetries + 1} attempts")
                        throw e
                    }
                }
            }
        }
    }
}
