package com.wenyan.app.feature.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
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
 * MEDIUM 窗口（640dp 宽，折叠 rail 占 80dp → 内容区 560dp）横屏双栏回归测试（v0.9.35）。
 *
 * 覆盖 H1 审计修复的关键场景：内容区 <600dp 时窗口宽度类 MEDIUM 仍应双栏激活。
 * 布局：左卡片 weight(1f) = 560-16(间距)-200(右栏) = 344dp；右栏 x=360..560。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w640dp-h360dp-land")
class CardsLandscapeMediumTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `MEDIUM窗口内容区560dp双栏激活且布局协调`() {
        composeRule.setContent {
            WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                Box(Modifier.requiredSize(560.dp, 340.dp)) {
                    CardReviewContent(
                        card = previewCardItem(),
                        uiState = previewUiState(isFlipped = true),
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

        // 左卡片区占满 344dp（widthIn 480 不生效，因为左栏 <480）
        composeRule.onNodeWithContentDescription("复习进度：第 1 张，共 1 张")
            .assertWidthIsEqualTo(344.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        // 右栏评分按钮位于右侧（x=360..560）：第一列 360..456、第二列 464..560，未溢出
        composeRule.onNodeWithContentDescription("不会")
            .assertLeftPositionInRootIsEqualTo(360.dp)
        composeRule.onNodeWithContentDescription("简单")
            .assertLeftPositionInRootIsEqualTo(464.dp)
    }
}
