package com.wenyan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // NF-S3 修复：必须在 super.onCreate 前安装 SplashScreen，
        // 否则 Android 12+ 系统会用默认主题绘制 splash 而非 Theme.Wenyan.Splash。
        // 当前为最小化集成：仅消除冷启动白屏。
        // 后续 1.J 种子加载链路重构完成后，可配合：
        //   installSplashScreen().setKeepOnScreenCondition { seedState == Loading }
        // 让 splash 持续显示直到种子数据加载完成。
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WenyanApp()
        }
    }
}
