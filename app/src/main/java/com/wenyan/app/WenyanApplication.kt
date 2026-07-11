package com.wenyan.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 文研App Application 入口。
 *
 * 标注 @HiltAndroidApp 触发 Hilt 代码生成，创建依赖注入容器。
 * 整个应用的依赖图以此为根。
 */
@HiltAndroidApp
class WenyanApplication : Application()
