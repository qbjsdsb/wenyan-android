package com.wenyan.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenyan.app.core.data.ThemeViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import com.wenyan.app.navigation.TopLevelDestination
import com.wenyan.app.navigation.WenyanNavHost

/**
 * 文研App 顶层 Composable。
 *
 * 接入 [ThemeViewModel] 获取主题配置，包裹 [WenyanTheme]。
 * 使用 [ExpressiveScaffold] 提供色调表面背景。
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

        ExpressiveScaffold(
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIndicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
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
