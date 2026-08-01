package com.wenyan.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 顶级目的地定义。
 *
 * 对应底部导航栏的 5 个主入口：
 * - 知识点（knowledge）
 * - 论述题（essay）— v0.9.9：从子路由提升为顶级 Tab，替换原"真题"位置
 * - 卡片（cards）
 * - 错题本（wrong_answer）— v0.9.0：从 quiz 子路由提升为顶级 Tab，原"图谱"位置
 * - 设置（settings）
 *
 * 变更历史：
 * - v0.6：AiAssistant 从顶级 Tab 降为子路由，由 4 个主屏 TopBar SmartToy 图标进入
 * - v0.9.0：移除 Graph 顶级 Tab（feature:graph 模块整体删除，知识点关联改走树结构），
 *           WrongAnswer 从 quiz 子路由提升为顶级 Tab，占据原 Graph 位置
 * - v0.9.9：移除 Quiz 顶级 Tab（真题 → 论述题迁移），新增 Essay 顶级 Tab
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

    data object Essay : TopLevelDestination(
        route = ROUTE_ESSAY,
        label = "论述题",
        icon = Icons.AutoMirrored.Filled.MenuBook,
    )

    data object Cards : TopLevelDestination(
        route = ROUTE_CARDS,
        label = "卡片",
        icon = Icons.Filled.Style,
    )

    data object WrongAnswer : TopLevelDestination(
        route = ROUTE_WRONG_ANSWER,
        label = "错题本",
        icon = Icons.Filled.ErrorOutline,
    )

    data object Settings : TopLevelDestination(
        route = ROUTE_SETTINGS,
        label = "设置",
        icon = Icons.Filled.Settings,
    )

    companion object {
        // 顶级路由常量，供 NavHost 与导航调用共用
        const val ROUTE_KNOWLEDGE = "knowledge"
        const val ROUTE_ESSAY = "essay"
        const val ROUTE_CARDS = "cards"
        const val ROUTE_WRONG_ANSWER = "wrong_answer"
        const val ROUTE_SETTINGS = "settings"

        // 全部顶级目的地，按底部导航顺序排列
        val destinations: List<TopLevelDestination> = listOf(
            Knowledge,
            Essay,
            Cards,
            WrongAnswer,
            Settings,
        )
    }
}
