package com.wenyan.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.ThemeViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.WenyanNavigationBar
import com.wenyan.app.core.designsystem.component.WenyanNavItem
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import com.wenyan.app.navigation.TopLevelDestination
import com.wenyan.app.navigation.WenyanNavHost

/**
 * 文研App 顶层 Composable。
 *
 * 接入 [ThemeViewModel] 获取主题配置，包裹 [WenyanTheme]。
 * 使用 [ExpressiveScaffold] 提供色调表面背景。
 * 底部导航栏用 [WenyanNavigationBar]（药丸风格，配色统一）。
 */
@Composable
fun WenyanApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    WenyanTheme(config = themeConfig) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination

        // 计算当前高亮的顶级 Tab 路由。
        // 用 hierarchy 检查：当用户在子路由（如知识点详情）时，
        // 其所属的顶级 Tab（如"知识点"）仍保持高亮。
        val selectedTopLevelRoute = TopLevelDestination.destinations
            .firstOrNull { dest ->
                currentDestination?.hierarchy?.any { it.route == dest.route } == true
            }
            ?.route

        // P2-REC-5 修正：用 remember 缓存静态映射，避免每次重组都创建新 List 分配内存。
        // TopLevelDestination.destinations 是静态列表（companion object val），映射结果不变。
        val topLevelRoutes = remember {
            TopLevelDestination.destinations.map { it.route }
        }

        // P0-N1 修正：仅顶级路由显示外层 NavigationBar。
        // - 子路由（knowledge_detail/{pointId} / settings / api_config）不显示，
        //   避免遮挡子页面的内容与返回按钮。
        // - AiAssistant 虽是顶级路由，但其内部 ExpressiveScaffold 自带 InputBar，
        //   外层 NavigationBar 会与之叠加冲突，故同样隐藏。
        val currentRoute = currentDestination?.route
        val showBottomBar = currentRoute != null &&
            currentRoute in topLevelRoutes &&
            currentRoute != TopLevelDestination.ROUTE_AI_ASSISTANT

        // P2-REC-5 修正：用 remember 缓存 WenyanNavItem 列表（静态数据，不随重组变化）
        val navItems = remember {
            TopLevelDestination.destinations.map { destination ->
                WenyanNavItem(
                    route = destination.route,
                    label = destination.label,
                    icon = destination.icon,
                )
            }
        }

        ExpressiveScaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    WenyanNavigationBar(
                        items = navItems,
                        currentRoute = selectedTopLevelRoute,
                        onNavigate = { route -> navigateToTopLevelDestination(navController, route) },
                    )
                }
            },
        ) { innerPadding ->
            WenyanNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

private fun navigateToTopLevelDestination(
    navController: androidx.navigation.NavHostController,
    route: String,
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
