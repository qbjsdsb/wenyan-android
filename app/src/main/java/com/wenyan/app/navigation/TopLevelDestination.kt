package com.wenyan.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 顶级目的地定义。
 *
 * 对应底部导航栏的 5 个主入口：
 * - 知识点（knowledge）
 * - 真题（quiz）
 * - 卡片（cards）
 * - 图谱（graph）
 * - 设置（settings）
 *
 * AI 助手（aiassistant）不再是顶级目的地，改为子路由，
 * 由 4 个主屏 TopBar 右上角 SmartToy 图标进入。
 *
 * 使用 sealed class 确保导航目的地穷举可控。
 */
sealed class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Knowledge : TopLevelDestination(
        route = ROUTE_KNOWLEDGE,
        label = "知识点",
        icon = Icons.Filled.AutoStories,
    )

    data object Quiz : TopLevelDestination(
        route = ROUTE_QUIZ,
        label = "真题",
        icon = Icons.Filled.Quiz,
    )

    data object Cards : TopLevelDestination(
        route = ROUTE_CARDS,
        label = "卡片",
        icon = Icons.Filled.Style,
    )

    data object Graph : TopLevelDestination(
        route = ROUTE_GRAPH,
        label = "图谱",
        icon = Icons.Filled.Hub,
    )

    data object Settings : TopLevelDestination(
        route = ROUTE_SETTINGS,
        label = "设置",
        icon = Icons.Filled.Settings,
    )

    companion object {
        // 顶级路由常量，供 NavHost 与导航调用共用
        const val ROUTE_KNOWLEDGE = "knowledge"
        const val ROUTE_QUIZ = "quiz"
        const val ROUTE_CARDS = "cards"
        const val ROUTE_GRAPH = "graph"
        const val ROUTE_SETTINGS = "settings"

        // 全部顶级目的地，按底部导航顺序排列
        val destinations: List<TopLevelDestination> = listOf(
            Knowledge,
            Quiz,
            Cards,
            Graph,
            Settings,
        )
    }
}
