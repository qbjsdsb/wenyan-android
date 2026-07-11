package com.wenyan.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wenyan.app.feature.aiassistant.AiAssistantScreen
import com.wenyan.app.feature.cards.CardsScreen
import com.wenyan.app.feature.graph.GraphScreen
import com.wenyan.app.feature.knowledge.KnowledgeScreen
import com.wenyan.app.feature.quiz.QuizScreen

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
 * 后续 Task 可在此扩展子路由（如知识点详情、真题详情等）。
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
        knowledgeDestination()
        quizDestination()
        cardsDestination()
        graphDestination()
        aiAssistantDestination()
    }
}

// 各顶级目的地的 composable 注册，拆分为扩展函数便于后续扩展子路由
private fun NavGraphBuilder.knowledgeDestination() {
    composable(TopLevelDestination.ROUTE_KNOWLEDGE) {
        KnowledgeScreen()
    }
}

private fun NavGraphBuilder.quizDestination() {
    composable(TopLevelDestination.ROUTE_QUIZ) {
        QuizScreen()
    }
}

private fun NavGraphBuilder.cardsDestination() {
    composable(TopLevelDestination.ROUTE_CARDS) {
        CardsScreen()
    }
}

private fun NavGraphBuilder.graphDestination() {
    composable(TopLevelDestination.ROUTE_GRAPH) {
        GraphScreen()
    }
}

private fun NavGraphBuilder.aiAssistantDestination() {
    composable(TopLevelDestination.ROUTE_AI_ASSISTANT) {
        AiAssistantScreen()
    }
}
