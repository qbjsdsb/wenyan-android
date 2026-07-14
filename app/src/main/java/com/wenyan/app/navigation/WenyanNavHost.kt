package com.wenyan.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import com.wenyan.app.feature.aiassistant.AiAssistantScreen
import com.wenyan.app.feature.aiassistant.ApiConfigScreen
import com.wenyan.app.feature.cards.CardsScreen
import com.wenyan.app.feature.graph.GraphScreen
import com.wenyan.app.feature.knowledge.KnowledgePointDetailScreen
import com.wenyan.app.feature.knowledge.KnowledgeScreen
import com.wenyan.app.feature.quiz.QuizScreen
import com.wenyan.app.feature.settings.SettingsScreen

/**
 * 文研App 主导航图。
 *
 * 承载 5 个顶级路由的 composable 目的地（底部 NavigationBar）：
 * - knowledge：知识点列表
 * - quiz：真题练习
 * - cards：记忆卡片
 * - graph：知识图谱
 * - settings：设置（v0.6 起从子路由提升为顶级 Tab）
 *
 * 子路由（非顶级目的地）：
 * - aiassistant：AI 助手（v0.6 起从顶级 Tab 降为子路由，Push/Pop slide）
 * - knowledge_detail/{pointId}：知识点详情（Spec C1.27 多教材对照 + C7.2 来源溯源）
 * - api_config：API 配置（Spec C5.7a 设计文档 3.6.4 多服务商配置）
 *
 * 4 个主屏（knowledge/quiz/cards/graph）TopBar 右上角均提供 AI 入口（SmartToy 图标），
 * 点击后以子路由 Push 动画进入 AI 助手，避免与底部 NavigationBar 叠加冲突。
 */
@Composable
fun WenyanNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.ROUTE_KNOWLEDGE,
        modifier = modifier,
        // 顶级 Tab 切换：纯 fade，避免与 NavigationBar indicator 动画冲突产生抖动
        enterTransition = { WenyanMotion.TabEnterTransition },
        exitTransition = { WenyanMotion.TabExitTransition },
        popEnterTransition = { WenyanMotion.TabEnterTransition },
        popExitTransition = { WenyanMotion.TabExitTransition },
    ) {
        knowledgeDestination(
            onNavigateToAiAssistant = {
                // v0.6：AiAssistant 改为子路由，用 Push/Pop slide + launchSingleTop，
                // 不再 popUpTo startDestination（避免破坏 Tab 状态栈）。
                navController.navigate(ROUTE_AI_ASSISTANT) {
                    launchSingleTop = true
                }
            },
            onNavigateToDetail = { pointId ->
                // P0 修正：详情间跳转（detail→detail）时弹出现有 detail 入口，
                // 避免 back stack 无界增长（用户在关联知识点间跳转 N 次后需按 N 次返回）。
                // popUpTo 匹配 nav graph 中的 knowledge_detail/{pointId} 目标：
                // - 列表→详情（back stack 无 detail）：popUpTo 为 no-op，安全
                // - 详情→详情（back stack 有 detail）：弹出当前 detail，再压入新 detail
                navController.navigate("$ROUTE_KNOWLEDGE_DETAIL/$pointId") {
                    popUpTo("$ROUTE_KNOWLEDGE_DETAIL/{pointId}") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
        )
        quizDestination(
            onNavigateToAiAssistant = {
                navController.navigate(ROUTE_AI_ASSISTANT) {
                    launchSingleTop = true
                }
            },
            onNavigateToDetail = { pointId ->
                navController.navigate("$ROUTE_KNOWLEDGE_DETAIL/$pointId") {
                    popUpTo("$ROUTE_KNOWLEDGE_DETAIL/{pointId}") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
        )
        cardsDestination(
            onNavigateToAiAssistant = {
                navController.navigate(ROUTE_AI_ASSISTANT) {
                    launchSingleTop = true
                }
            },
        )
        graphDestination(
            onNavigateToAiAssistant = {
                navController.navigate(ROUTE_AI_ASSISTANT) {
                    launchSingleTop = true
                }
            },
        )
        settingsDestination(
            onNavigateToApiConfig = {
                navController.navigate(ROUTE_API_CONFIG) {
                    launchSingleTop = true
                }
            },
        )
        aiAssistantDestination(
            onNavigateToApiConfig = {
                // P1 修正：子路由需 launchSingleTop，防止快速双击重复压栈
                navController.navigate(ROUTE_API_CONFIG) {
                    launchSingleTop = true
                }
            },
        )
        apiConfigDestination(
            onBack = { navController.popBackStack() },
        )
        knowledgeDetailDestination(
            onBack = { navController.popBackStack() },
            onNavigateToDetail = { pointId ->
                // P0 修正：详情间跳转（detail→detail）时弹出现有 detail 入口，
                // 避免 back stack 无界增长（用户在关联知识点间跳转 N 次后需按 N 次返回）。
                navController.navigate("$ROUTE_KNOWLEDGE_DETAIL/$pointId") {
                    popUpTo("$ROUTE_KNOWLEDGE_DETAIL/{pointId}") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
        )
    }
}

// 各顶级目的地的 composable 注册，拆分为扩展函数便于后续扩展子路由
private fun NavGraphBuilder.knowledgeDestination(
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    composable(TopLevelDestination.ROUTE_KNOWLEDGE) {
        KnowledgeScreen(
            onNavigateToAiAssistant = onNavigateToAiAssistant,
            onNavigateToDetail = onNavigateToDetail,
        )
    }
}

private fun NavGraphBuilder.quizDestination(
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    composable(TopLevelDestination.ROUTE_QUIZ) {
        QuizScreen(
            onNavigateToAiAssistant = onNavigateToAiAssistant,
            onNavigateToDetail = onNavigateToDetail,
        )
    }
}

private fun NavGraphBuilder.cardsDestination(
    onNavigateToAiAssistant: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_CARDS) {
        CardsScreen(onNavigateToAiAssistant = onNavigateToAiAssistant)
    }
}

private fun NavGraphBuilder.graphDestination(
    onNavigateToAiAssistant: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_GRAPH) {
        GraphScreen(onNavigateToAiAssistant = onNavigateToAiAssistant)
    }
}

// v0.6：Settings 从子路由提升为顶级 Tab，用 NavHost 默认 Tab fade（无 Push/Pop slide）
private fun NavGraphBuilder.settingsDestination(
    onNavigateToApiConfig: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_SETTINGS) {
        SettingsScreen(
            onNavigateToApiConfig = onNavigateToApiConfig,
        )
    }
}

// v0.6：AiAssistant 从顶级 Tab 降为子路由，用 Push/Pop slide transition
private fun NavGraphBuilder.aiAssistantDestination(
    onNavigateToApiConfig: () -> Unit,
) {
    composable(
        route = ROUTE_AI_ASSISTANT,
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        AiAssistantScreen(
            onNavigateToApiConfig = onNavigateToApiConfig,
        )
    }
}

// API 配置子路由（Spec C5.7a 设计文档 3.6.4 多服务商配置）
// 子路由用 Push/Pop slide transition（覆盖 NavHost 默认的 Tab fade）
private fun NavGraphBuilder.apiConfigDestination(
    onBack: () -> Unit,
) {
    composable(
        route = ROUTE_API_CONFIG,
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        ApiConfigScreen(onBack = onBack)
    }
}

// 知识点详情子路由（Spec C1.27 多教材对照 + C7.2 来源溯源）
private fun NavGraphBuilder.knowledgeDetailDestination(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    composable(
        route = "$ROUTE_KNOWLEDGE_DETAIL/{pointId}",
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        KnowledgePointDetailScreen(
            onBack = onBack,
            onNavigateToDetail = onNavigateToDetail,
        )
    }
}

// 子路由常量
private const val ROUTE_API_CONFIG = "api_config"
private const val ROUTE_AI_ASSISTANT = "aiassistant"
private const val ROUTE_KNOWLEDGE_DETAIL = "knowledge_detail"
