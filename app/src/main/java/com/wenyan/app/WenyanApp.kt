package com.wenyan.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wenyan.app.navigation.TopLevelDestination
import com.wenyan.app.navigation.WenyanNavHost

/**
 * 文研App 顶层 Composable。
 *
 * 采用 Scaffold + NavigationBar + NavHost 的标准结构：
 * - Scaffold 提供整体布局槽位
 * - NavigationBar 渲染 5 个顶级目的地的底部导航
 * - NavHost 承载各目的地 composable
 *
 * 当前界面根据当前路由高亮对应底部导航项。
 */
@Composable
fun WenyanApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTopLevelDestination(navController, destination.route) },
                        icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                        label = { Text(text = destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        WenyanNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

// 底部导航切换：恢复状态、弹出至起始目的地、避免重复实例
private fun navigateToTopLevelDestination(
    navController: androidx.navigation.NavHostController,
    route: String,
) {
    navController.navigate(route) {
        // 弹出至起始目的地，避免回退栈堆积
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // 避免重复创建同一目的地
        launchSingleTop = true
        // 切换时恢复状态
        restoreState = true
    }
}
