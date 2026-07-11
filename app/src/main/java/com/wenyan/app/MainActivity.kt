package com.wenyan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 文研App 单 Activity 入口。
 *
 * 标注 @AndroidEntryPoint 以支持 Hilt 字段注入。
 * 所有界面通过 Compose Navigation 在此 Activity 内切换。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WenyanTheme {
                WenyanApp()
            }
        }
    }
}
