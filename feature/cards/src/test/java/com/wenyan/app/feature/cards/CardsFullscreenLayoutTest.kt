package com.wenyan.app.feature.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
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
 * 全屏横屏双栏布局协调性回归测试（v0.9.36 全屏模式）。
 *
 * 基于语义树实测数据固化全屏横屏约束（1000dp 内容区，比普通横屏更宽以验证 560dp 卡片上限）：
 * - 卡片限宽 560dp 居中（全屏高度更大，比例 ~1.45:1 仍协调）
 * - 右操作栏 280dp 宽（比普通双栏 200dp 更宽，容纳单列竖排按钮）
 * - 4 档评分按钮单列竖排（用户"一个个竖着排列"偏好）
 *
 * 若未来布局改动破坏协调性（卡片过宽 / 按钮排列变化），本测试失败预警。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h450dp-land")
class CardsFullscreenLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun renderCard(
        isFlipped: Boolean,
        isSibling: Boolean = false,
        height: Int = 450,
    ) {
        composeRule.setContent {
            WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                Box(Modifier.requiredSize(1000.dp, height.dp)) {
                    CardReviewContent(
                        card = previewCardItem(),
                        uiState = previewUiState(isFlipped = isFlipped),
                        previews = emptyMap(),
                        isSiblingAlreadyRated = isSibling,
                        isInWrongBook = false,
                        isAddingBookmark = false,
                        useDualPane = true,
                        // v0.9.36 全屏模式：横屏放宽卡片 560dp + 右栏 280dp 单列竖排
                        fullscreenLandscape = true,
                        onFlip = {}, onRate = {}, onUndo = {}, onSkip = {}, onAddToWrongAnswerBook = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `全屏横屏 卡片与进度条限宽 560dp 居中`() {
        renderCard(isFlipped = false)
        // 左栏宽 = 1000 - 280(操作栏) - 16(间距 Spacing.lg) = 704dp；
        // 卡片限宽 560dp 居中 → 左缘 (704-560)/2 = 72dp（rotationY 语义 bounds 异常，用进度条代理验证）
        composeRule.onNodeWithContentDescription("复习进度：第 1 张，共 1 张")
            .assertWidthIsEqualTo(560.dp)
            .assertLeftPositionInRootIsEqualTo(72.dp)
    }

    @Test
    fun `全屏横屏 右栏操作面板 280dp 宽`() {
        renderCard(isFlipped = true)
        // 右操作栏左缘 = 704(左栏) + 16(间距 Spacing.lg) = 720dp，宽 280dp → 右缘贴屏幕 1000
        composeRule.onNodeWithContentDescription("不会")
            .assertLeftPositionInRootIsEqualTo(720.dp)
    }

    @Test
    fun `全屏横屏 4 档评分按钮单列竖排 左缘对齐`() {
        renderCard(isFlipped = true)
        // 单列竖排：四个按钮左缘全部对齐 720（面板内 fillMaxWidth）
        composeRule.onNodeWithContentDescription("不会")
            .assertLeftPositionInRootIsEqualTo(720.dp)
        composeRule.onNodeWithContentDescription("困难")
            .assertLeftPositionInRootIsEqualTo(720.dp)
        composeRule.onNodeWithContentDescription("良好")
            .assertLeftPositionInRootIsEqualTo(720.dp)
        composeRule.onNodeWithContentDescription("简单")
            .assertLeftPositionInRootIsEqualTo(720.dp)
        // 竖排顺序：不会 < 困难 < 良好 < 简单（top 依次递增）
        val againTop = composeRule.onNodeWithContentDescription("不会")
            .fetchSemanticsNode().boundsInRoot.top
        val hardTop = composeRule.onNodeWithContentDescription("困难")
            .fetchSemanticsNode().boundsInRoot.top
        val goodTop = composeRule.onNodeWithContentDescription("良好")
            .fetchSemanticsNode().boundsInRoot.top
        val easyTop = composeRule.onNodeWithContentDescription("简单")
            .fetchSemanticsNode().boundsInRoot.top
        assert(againTop < hardTop) { "AGAIN 应在 HARD 上方（实际 $againTop vs $hardTop）" }
        assert(hardTop < goodTop) { "HARD 应在 GOOD 上方（实际 $hardTop vs $goodTop）" }
        assert(goodTop < easyTop) { "GOOD 应在 EASY 上方（实际 $goodTop vs $easyTop）" }
    }

    @Test
    fun `全屏横屏 右栏操作面板垂直居中非悬顶`() {
        renderCard(isFlipped = true)
        // 面板内容垂直居中：首按钮 top=25（面板顶 8 + 内容区 (442-408)/2=17），
        // 显著大于 0（非 top 对齐贴顶），与普通横屏 89dp 相比更紧凑但居中协调
        composeRule.onNodeWithContentDescription("不会")
            .assertTopPositionInRootIsEqualTo(25.dp)
    }
}
