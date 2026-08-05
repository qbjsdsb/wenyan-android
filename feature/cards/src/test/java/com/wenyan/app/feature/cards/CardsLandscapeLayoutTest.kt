package com.wenyan.app.feature.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.designsystem.theme.ColorMode
import com.wenyan.app.core.designsystem.theme.ThemeConfig
import com.wenyan.app.core.designsystem.theme.WenyanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 横屏双栏布局协调性回归测试（v0.9.35）。
 *
 * 基于语义树实测数据固化横屏协调性约束（800dp 内容区）：
 * - 卡片限宽 480dp 居中（比例 ~1.42:1，行文舒适）
 * - 右栏操作面板垂直居中（消除底部悬空空白）
 * - 2×2 评分按钮网格存在
 *
 * 若未来布局改动破坏协调性（卡片过宽 / 操作面板悬顶），本测试失败预警。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h400dp-land")
class CardsLandscapeLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun renderCard(isFlipped: Boolean) {
        composeRule.setContent {
            WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                Box(Modifier.requiredSize(800.dp, 400.dp)) {
                    CardReviewContent(
                        card = previewCardItem(),
                        uiState = previewUiState(isFlipped = isFlipped),
                        previews = emptyMap(),
                        isSiblingAlreadyRated = false,
                        isInWrongBook = false,
                        isAddingBookmark = false,
                        useDualPane = true,
                        onFlip = {}, onRate = {}, onUndo = {}, onSkip = {}, onAddToWrongAnswerBook = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `横屏卡片与进度条限宽 480dp 居中`() {
        renderCard(isFlipped = false)
        // 进度条与卡片同宽 480dp（卡片因 rotationY 语义 bounds 异常，用进度条代理验证）
        composeRule.onNodeWithContentDescription("复习进度：第 1 张，共 1 张")
            .assertWidthIsEqualTo(480.dp)
            .assertLeftPositionInRootIsEqualTo(52.dp)
    }

    @Test
    fun `横屏翻转后 右栏操作面板垂直居中`() {
        renderCard(isFlipped = true)
        // 操作面板首元素"不会" top=89，末元素"跳过" bottom=319 → 中心 204 ≈ 容器中心 200
        composeRule.onNodeWithContentDescription("不会")
            .assertTopPositionInRootIsEqualTo(89.dp)
        // 末元素"跳过" top=267 + 高 52 → bottom=319，与首元素 89 共同居中于 204≈200
        composeRule.onNodeWithContentDescription("跳过当前卡片，不评分")
            .assertPositionInRootIsEqualTo(704.dp, 267.dp)
    }

    @Test
    fun `横屏翻转前 右栏操作面板同样垂直居中`() {
        renderCard(isFlipped = false)
        // 翻转前内容组首"加入错题本" top=170、末"跳过" bottom=282 → 中心 226
        // 内容更矮（155dp），居中余量更大，断言不再悬顶（top 远大于 8）
        composeRule.onNodeWithContentDescription("加入错题本")
            .assertTopPositionInRootIsEqualTo(170.dp)
        composeRule.onNodeWithContentDescription("跳过当前卡片，不评分")
            .assertPositionInRootIsEqualTo(704.dp, 230.dp)
    }

    @Test
    fun `横屏翻转后 2x2 评分按钮网格存在`() {
        renderCard(isFlipped = true)
        // 同列：'不会'与'良好'左缘均在 600
        composeRule.onNodeWithContentDescription("不会")
            .assertLeftPositionInRootIsEqualTo(600.dp)
        composeRule.onNodeWithContentDescription("良好")
            .assertLeftPositionInRootIsEqualTo(600.dp)
        // 不同行：'良好' top(148) > '不会' bottom(140)
        composeRule.onNodeWithContentDescription("良好")
            .assertTopPositionInRootIsEqualTo(148.dp)
        // 右列：'困难'左缘 704
        composeRule.onNodeWithContentDescription("困难")
            .assertLeftPositionInRootIsEqualTo(704.dp)
    }
}
