package com.wenyan.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wenyan.app.core.designsystem.motion.WenyanMotion
import com.wenyan.app.feature.aiassistant.AiAssistantScreen
import com.wenyan.app.feature.aiassistant.ApiConfigScreen
import com.wenyan.app.feature.cards.CardsFullscreenScreen
import com.wenyan.app.feature.cards.CardsScreen
import com.wenyan.app.feature.cards.CardsViewModel
import com.wenyan.app.feature.knowledge.EssayDetailScreen
import com.wenyan.app.feature.knowledge.EssayListScreen
import com.wenyan.app.feature.knowledge.KnowledgePointDetailScreen
import com.wenyan.app.feature.knowledge.KnowledgeScreen
import com.wenyan.app.feature.knowledge.QuizPracticeDetailScreen
import com.wenyan.app.feature.knowledge.QuizPracticeListScreen
import com.wenyan.app.feature.quiz.WrongAnswerScreen
import com.wenyan.app.feature.settings.AboutTutorialScreen
import com.wenyan.app.feature.settings.SettingsScreen
import com.wenyan.app.feature.settings.UpdateCheckScreen

/**
 * 文研App 主导航图。
 *
 * 承载 5 个顶级路由的 composable 目的地（底部 NavigationBar）：
 * - knowledge：知识点列表
 * - essay：论述题
 * - cards：记忆卡片
 * - wrong_answer：错题本（v0.9.0 起从 quiz 子路由提升为顶级 Tab，占据原 graph 位置）
 * - settings：设置（v0.6 起从子路由提升为顶级 Tab）
 *
 * 子路由（非顶级目的地）：
 * - aiassistant：AI 助手（v0.6 起从顶级 Tab 降为子路由，Push/Pop slide）
 * - knowledge_detail/{pointId}：知识点详情（Spec C1.27 多教材对照 + C7.2 来源溯源）
 * - api_config：API 配置（Spec C5.7a 设计文档 3.6.4 多服务商配置）
 * - about：关于与教程（v0.9.5 新增，7 章深度教程：定位/模块/FSRS-6/三档记忆/RAG/使用指南/致谢）
 *
 * v0.9.0 变更：
 * - 移除 graph 顶级 Tab（feature:graph 模块整体删除，知识点关联改走树结构）
 * - WrongAnswer 从子路由提升为顶级 Tab，删除 quiz TopBar Inbox 入口
 *
 * 3 个主屏（knowledge/essay/cards）TopBar 右上角均提供 AI 入口（SmartToy 图标），
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
            onNavigateToQuizPractice = {
                navController.navigate(ROUTE_QUIZ_PRACTICE) {
                    launchSingleTop = true
                }
            },
        )
        // v0.9.33：真题背题子路由（名词解释/简答专项）
        quizPracticeDestination(
            onBack = { navController.popBackStack() },
            onNavigateToDetail = { questionId, type, subject, year ->
                val typeParam = type ?: FILTER_ALL
                val subjectParam = subject ?: FILTER_ALL
                val yearParam = year?.toString() ?: FILTER_ALL
                navController.navigate(
                    "$ROUTE_QUIZ_PRACTICE_DETAIL/$questionId" +
                        "?type=$typeParam&subject=$subjectParam&year=$yearParam",
                ) {
                    launchSingleTop = true
                }
            },
        )
        quizPracticeDetailDestination(
            onBack = { navController.popBackStack() },
        )
        // v0.9.9：真题 → 论述题迁移，essayTabDestination 替换 quizDestination
        // 顶级 Tab 使用 NavHost 默认 Tab fade transition（与 cards/wrongAnswer/settings 一致）
        essayTabDestination(
            onNavigateToEssayDetail = { essayId ->
                navController.navigate("$ROUTE_ESSAY_DETAIL/$essayId") {
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
            onNavigateToKnowledge = {
                // v0.8.7：完成态"返回学习"按钮，切换到知识点列表 Tab
                navController.navigate(TopLevelDestination.ROUTE_KNOWLEDGE) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onNavigateToDetail = { pointId ->
                // v0.8.8 P0：Leech 警告"查看知识点"按钮跳转详情
                // （原 cardsDestination 漏传此参数，导致按钮点击后对话框关闭但不导航）
                navController.navigate("$ROUTE_KNOWLEDGE_DETAIL/$pointId") {
                    popUpTo("$ROUTE_KNOWLEDGE_DETAIL/{pointId}") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            // v0.9.36 全屏模式：Push 进入全屏沉浸页（共享卡片页 ViewModel）
            onNavigateToFullscreen = {
                navController.navigate(ROUTE_CARDS_FULLSCREEN) {
                    launchSingleTop = true
                }
            },
        )
        // v0.9.36 全屏沉浸页子路由（共享卡片页 ViewModel，保持同一复习会话）
        cardsFullscreenDestination(
            onBack = { navController.popBackStack() },
            onNavigateToAiAssistant = {
                navController.navigate(ROUTE_AI_ASSISTANT) {
                    launchSingleTop = true
                }
            },
            onNavigateToKnowledge = {
                navController.navigate(TopLevelDestination.ROUTE_KNOWLEDGE) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
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
            // 关键：与卡片页共享同一 CardsViewModel（同 backStackEntry → 同一会话状态）。
            // 通过 @Composable provider 延迟到 composable 内容内求值（NavHost builder 非 composable 上下文）
            viewModelProvider = {
                hiltViewModel(
                    navController.getBackStackEntry(TopLevelDestination.ROUTE_CARDS),
                )
            },
        )
        // v0.9.0：WrongAnswer 提升为顶级 Tab（原 graphDestination 位置）
        // 不传 onBack → WrongAnswerScreen 顶级模式（无返回箭头）
        wrongAnswerDestination()
        settingsDestination(
            onNavigateToApiConfig = {
                navController.navigate(ROUTE_API_CONFIG) {
                    launchSingleTop = true
                }
            },
            onNavigateToAbout = {
                // v0.9.5：教程子路由，Push/Pop slide + launchSingleTop 防双击重复压栈
                navController.navigate(ROUTE_ABOUT) {
                    launchSingleTop = true
                }
            },
            onNavigateToUpdateCheck = {
                // v0.9.11：检查更新子路由，Push/Pop slide + launchSingleTop 防双击重复压栈
                navController.navigate(ROUTE_UPDATE_CHECK) {
                    launchSingleTop = true
                }
            },
        )
        aiAssistantDestination(
            onBack = { navController.popBackStack() },
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
        aboutDestination(
            onBack = { navController.popBackStack() },
        )
        updateCheckDestination(
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
            onNavigateToEssay = { essayId ->
                // v0.9.8：知识点详情 → 论述题详情，Push/Pop slide + launchSingleTop
                navController.navigate("$ROUTE_ESSAY_DETAIL/$essayId") {
                    launchSingleTop = true
                }
            },
        )
        essayDetailDestination(
            onBack = { navController.popBackStack() },
            onNavigateToKnowledgeDetail = { pointId ->
                // v0.9.8：论述题详情 → 知识点详情（双向串联）
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
    onNavigateToQuizPractice: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_KNOWLEDGE) {
        KnowledgeScreen(
            onNavigateToAiAssistant = onNavigateToAiAssistant,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToQuizPractice = onNavigateToQuizPractice,
        )
    }
}

// v0.9.33：真题背题列表子路由（名词解释/简答专项）
private fun NavGraphBuilder.quizPracticeDestination(
    onBack: () -> Unit,
    onNavigateToDetail: (questionId: String, type: String?, subject: String?, year: Int?) -> Unit,
) {
    composable(
        route = ROUTE_QUIZ_PRACTICE,
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        QuizPracticeListScreen(
            onBack = onBack,
            onNavigateToQuizPracticeDetail = onNavigateToDetail,
        )
    }
}

// v0.9.33：真题背题详情子路由（纯背诵模式）
private fun NavGraphBuilder.quizPracticeDetailDestination(
    onBack: () -> Unit,
) {
    composable(
        route = "$ROUTE_QUIZ_PRACTICE_DETAIL/{questionId}?type={type}&subject={subject}&year={year}",
        arguments = listOf(
            navArgument("questionId") { type = NavType.StringType },
            // 筛选条件（"ALL" 表示不筛选），背题页按相同条件重建前后题列表
            navArgument("type") { type = NavType.StringType; defaultValue = FILTER_ALL },
            navArgument("subject") { type = NavType.StringType; defaultValue = FILTER_ALL },
            navArgument("year") { type = NavType.StringType; defaultValue = FILTER_ALL },
        ),
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        QuizPracticeDetailScreen(onBack = onBack)
    }
}

// v0.9.9：真题→论述题迁移，essayTabDestination 替换 quizDestination
// 顶级 Tab 使用 NavHost 默认 Tab fade transition（与 cards/wrongAnswer/settings 一致）
private fun NavGraphBuilder.essayTabDestination(
    onNavigateToEssayDetail: (String) -> Unit,
) {
    composable(TopLevelDestination.ROUTE_ESSAY) {
        EssayListScreen(
            onBack = null,
            onNavigateToEssayDetail = onNavigateToEssayDetail,
        )
    }
}

private fun NavGraphBuilder.cardsDestination(
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToKnowledge: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToFullscreen: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_CARDS) {
        CardsScreen(
            onNavigateToAiAssistant = onNavigateToAiAssistant,
            onNavigateToKnowledge = onNavigateToKnowledge,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToFullscreen = onNavigateToFullscreen,
        )
    }
}

// v0.9.36：全屏沉浸复习子路由（共享卡片页 CardsViewModel，保持同一复习会话）
// 子路由用 Push/Pop slide transition（与 AiAssistant 等子路由一致）
private fun NavGraphBuilder.cardsFullscreenDestination(
    onBack: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToKnowledge: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModelProvider: @Composable () -> CardsViewModel,
) {
    composable(
        route = ROUTE_CARDS_FULLSCREEN,
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        CardsFullscreenScreen(
            onBack = onBack,
            onNavigateToAiAssistant = onNavigateToAiAssistant,
            onNavigateToKnowledge = onNavigateToKnowledge,
            onNavigateToDetail = onNavigateToDetail,
            viewModel = viewModelProvider(),
        )
    }
}

// v0.9.0：WrongAnswer 顶级 Tab，用 NavHost 默认 Tab fade（无 Push/Pop slide）
// onBack 为 null 时 WrongAnswerScreen 隐藏返回箭头（顶级模式）
private fun NavGraphBuilder.wrongAnswerDestination() {
    composable(TopLevelDestination.ROUTE_WRONG_ANSWER) {
        WrongAnswerScreen()
    }
}

// v0.6：Settings 从子路由提升为顶级 Tab，用 NavHost 默认 Tab fade（无 Push/Pop slide）
// v0.9.5：新增 onNavigateToAbout，进入"关于与教程"子路由
// v0.9.11：新增 onNavigateToUpdateCheck，进入"检查更新"子路由
private fun NavGraphBuilder.settingsDestination(
    onNavigateToApiConfig: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToUpdateCheck: () -> Unit,
) {
    composable(TopLevelDestination.ROUTE_SETTINGS) {
        SettingsScreen(
            onNavigateToApiConfig = onNavigateToApiConfig,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToUpdateCheck = onNavigateToUpdateCheck,
        )
    }
}

// v0.9.5：关于与教程子路由（7 章深度教程）
// 子路由用 Push/Pop slide transition（覆盖 NavHost 默认的 Tab fade）
private fun NavGraphBuilder.aboutDestination(
    onBack: () -> Unit,
) {
    composable(
        route = ROUTE_ABOUT,
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        AboutTutorialScreen(onBack = onBack)
    }
}

// v0.9.11：检查更新子路由
// 子路由用 Push/Pop slide transition（覆盖 NavHost 默认的 Tab fade）
private fun NavGraphBuilder.updateCheckDestination(
    onBack: () -> Unit,
) {
    composable(
        route = ROUTE_UPDATE_CHECK,
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        UpdateCheckScreen(onBack = onBack)
    }
}

// v0.6：AiAssistant 从顶级 Tab 降为子路由，用 Push/Pop slide transition
private fun NavGraphBuilder.aiAssistantDestination(
    onBack: () -> Unit,
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
            onBack = onBack,
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
// v0.9.8：新增 onNavigateToEssay，知识点详情 → 论述题详情（知识点串联器）
private fun NavGraphBuilder.knowledgeDetailDestination(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEssay: (String) -> Unit,
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
            onNavigateToEssay = onNavigateToEssay,
        )
    }
}

// v0.9.8：论述题详情子路由（10 区块结构：题目/审题/论证/框架/依据/交叉验证/链接/盲点/关联知识点）
private fun NavGraphBuilder.essayDetailDestination(
    onBack: () -> Unit,
    onNavigateToKnowledgeDetail: (String) -> Unit,
) {
    composable(
        route = "$ROUTE_ESSAY_DETAIL/{examQuestionId}",
        enterTransition = { WenyanMotion.PushEnterTransition },
        exitTransition = { WenyanMotion.PushExitTransition },
        popEnterTransition = { WenyanMotion.PopEnterTransition },
        popExitTransition = { WenyanMotion.PopExitTransition },
    ) {
        EssayDetailScreen(
            onBack = onBack,
            onNavigateToKnowledgeDetail = onNavigateToKnowledgeDetail,
        )
    }
}

// 子路由常量
private const val ROUTE_API_CONFIG = "api_config"
private const val ROUTE_AI_ASSISTANT = "aiassistant"
private const val ROUTE_KNOWLEDGE_DETAIL = "knowledge_detail"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_ESSAY_DETAIL = "essay_detail"
// v0.9.11：检查更新子路由
private const val ROUTE_UPDATE_CHECK = "update_check"
// v0.9.33：真题背题子路由
private const val ROUTE_QUIZ_PRACTICE = "quiz_practice"
private const val ROUTE_QUIZ_PRACTICE_DETAIL = "quiz_practice_detail"
// v0.9.36：知识卡片全屏沉浸页子路由
private const val ROUTE_CARDS_FULLSCREEN = "cards_fullscreen"
private const val FILTER_ALL = "ALL"
