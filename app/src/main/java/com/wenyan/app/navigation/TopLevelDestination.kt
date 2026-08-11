package com.wenyan.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 顶级目的地定义。
 *
 * 当前底部导航栏的 4 个主入口：今日、知识点、训练、我的。
 * Essay、Cards、WrongAnswer、Settings 仍保留为稳定路由/子页面，
 * 由训练或我的 Hub 进入，不直接占用底部导航栏槽位。
 *
 * 变更历史：
 * - v0.6：AiAssistant 从顶级 Tab 降为子路由，由 4 个主屏 TopBar SmartToy 图标进入
 * - v0.9.0：移除 Graph 顶级 Tab（feature:graph 模块整体删除，知识点关联改走树结构）。
 * - v0.9.9：论述题与错题迁移到训练/我的 Hub，保留原路由以兼容已有导航与深链。
 *
 * 使用 sealed class 确保导航目的地穷举可控。
 */
sealed class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Today : TopLevelDestination(
        route = ROUTE_TODAY,
        label = "今日",
        icon = Icons.Filled.Today,
    )

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

    data object Training : TopLevelDestination(
        route = ROUTE_TRAINING,
        label = "训练",
        icon = Icons.Filled.FitnessCenter,
    )

    data object My : TopLevelDestination(
        route = ROUTE_MY,
        label = "我的",
        icon = Icons.Filled.Person,
    )

    companion object {
        // 顶级路由常量，供 NavHost 与导航调用共用
        const val ROUTE_TODAY = "today"
        const val ROUTE_TRAINING = "training"
        const val ROUTE_MY = "my"
        const val ROUTE_KNOWLEDGE = "knowledge"
        const val ROUTE_ESSAY = "essay"
        const val ROUTE_CARDS = "cards"
        const val ROUTE_WRONG_ANSWER = "wrong_answer"
        const val ROUTE_SETTINGS = "settings"

        // 全部顶级目的地，按底部导航顺序排列
        val destinations: List<TopLevelDestination> = listOf(
            Today,
            Knowledge,
            Training,
            My,
        )

        fun parentRouteFor(route: String?): String? = when {
            route == null -> null
            route == ROUTE_TODAY -> ROUTE_TODAY
            route == ROUTE_KNOWLEDGE || route.startsWith("knowledge_detail/") -> ROUTE_KNOWLEDGE
            route == "daily_cards_fullscreen" || route.startsWith("daily_cards/") -> ROUTE_TODAY
            route == ROUTE_TRAINING || route == ROUTE_ESSAY || route == ROUTE_CARDS ||
                route == "quiz_practice" || route.startsWith("quiz_practice_detail/") ||
                route == "essay_detail" || route.startsWith("essay_detail/") ||
                route == "writing_materials" || route == "writing_editor" ||
                route.startsWith("writing_editor?") || route == "cards_fullscreen" -> ROUTE_TRAINING
            route == ROUTE_MY || route == ROUTE_WRONG_ANSWER || route == ROUTE_SETTINGS ||
                route == "about" || route == "update_check" || route == "api_config" ||
                route == "aiassistant" -> ROUTE_MY
            else -> null
        }
    }
}
