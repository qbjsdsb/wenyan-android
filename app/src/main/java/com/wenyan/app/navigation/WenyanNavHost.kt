package com.wenyan.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
 * 承载 5 个顶级路由的 composable 目的地：
 * - knowledge：知识点列表
 * - quiz：真题练习
 * - cards：记忆卡片
 * - graph：知识图谱
 * - aiassistant：AI助手
 *
 * 子路由（非顶级目的地）：
 * - knowledge_detail/{pointId}：知识点详情（Spec C1.27 多教材对照 + C7.2 来源溯源）
 * - api_config：API 配置（Spec C5.7a 设计文档 3.6.4 多服务商配置）
 *
 * 4 个主屏（knowledge/quiz/cards/graph）TopBar 右上角均提供 AI 入口（SmartToy 图标），
 * 与底部 NavigationBar 第 5 个 AI Tab 形成双入口，确保任何位置都能一键到达 AI 助手。
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
    ) {
        knowledgeDestination(
            onNavigateToAiAssistant = {
                navController.navigate(TopLevelDestination.ROUTE_AI_ASSISTANT)
            },
            onNavigateToDetail = { pointId ->
                navController.navigate("$ROUTE_KNOWLEDGE_DETAIL/$pointId")
            },
        )
        quizDestination(
            onNavigateToAiAssistant = {
                navController.navigate(TopLevelDestination.ROUTE_AI_ASSISTANT)
            },
            onNavigateToDetail = { pointId ->
                navController.navigate("$ROUTE_KNOWLEDGE_DETAIL/$pointId")
            },
        )
        cardsDestination(
            onNavigateToAiAssistant = {
                navController.navigate(TopLevelDestination.ROUTE_AI_ASSISTANT)
            },
        )
        graphDestination(
            onNavigateToAiAssistant = {
                navController.navigate(TopLevelDestination.ROUTE_AI_ASSISTANT)
            },
        )
        aiAssistantDestination(
            onNavigateToApiConfig = {
                navController.navigate(ROUTE_API_CONFIG)
            },
            onNavigateToSettings = {
                navController.navigate(ROUTE_SETTINGS)
            },
        )
        apiConfigDestination(
            onBack = { navController.popBackStack() },
        )
        settingsDestination(
            onBack = { navController.popBackStack() },
            onNavigateToApiConfig = {
                navController.navigate(ROUTE_API_CONFIG)
            },
        )
        knowledgeDetailDestination(
            onBack = { navController.popBackStack() },
            onNavigateToDetail = { pointId ->
                navController.navigate("$ROUTE_KNOWLEDGE_DETAIL/$pointId")
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

private fun NavGraphBuilder.aiAssistantDestination(
    onNavigateToApiConfig: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_AI_ASSISTANT) {
        AiAssistantScreen(
            onNavigateToApiConfig = onNavigateToApiConfig,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}

// API 配置子路由（Spec C5.7a 设计文档 3.6.4 多服务商配置）
private fun NavGraphBuilder.apiConfigDestination(
    onBack: () -> Unit,
) {
    composable(ROUTE_API_CONFIG) {
        ApiConfigScreen(onBack = onBack)
    }
}

// 设置子路由（主题/动态色彩/关于）
private fun NavGraphBuilder.settingsDestination(
    onBack: () -> Unit,
    onNavigateToApiConfig: () -> Unit,
) {
    composable(ROUTE_SETTINGS) {
        SettingsScreen(
            onBack = onBack,
            onNavigateToApiConfig = onNavigateToApiConfig,
        )
    }
}

// 知识点详情子路由（Spec C1.27 多教材对照 + C7.2 来源溯源）
private fun NavGraphBuilder.knowledgeDetailDestination(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    composable("$ROUTE_KNOWLEDGE_DETAIL/{pointId}") {
        KnowledgePointDetailScreen(
            onBack = onBack,
            onNavigateToDetail = onNavigateToDetail,
        )
    }
}

// 子路由常量
private const val ROUTE_API_CONFIG = "api_config"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_KNOWLEDGE_DETAIL = "knowledge_detail"
