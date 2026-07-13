package com.wenyan.app

import android.app.Application
import android.util.Log
import com.wenyan.app.core.data.seed.SeedDataLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
 */
@HiltAndroidApp
class WenyanApplication : Application() {

    @Inject
    lateinit var seedDataLoader: SeedDataLoader

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e("WenyanApplication", "Seed data load failed", e)
    }

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + exceptionHandler,
    )

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedDataLoader.ensureSeedDataLoaded()
        }
    }
}
