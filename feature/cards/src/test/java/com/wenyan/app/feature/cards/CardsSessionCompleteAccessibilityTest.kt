package com.wenyan.app.feature.cards

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
 * 完成态可访问性回归测试（v0.9.37 P0-3）。
 *
 * 背景：原 [SessionCompleteState] 把 `mergeDescendants=true` 放在整个 Column 上，
 * 将"再复习一轮 / 撤销最后一张 / 返回知识点列表"3 个独立按钮并入单一语义节点，
 * TalkBack 用户无法分别聚焦/触发（多 onClick 合并后仅剩一个可激活）。
 *
 * 修复后：仅统计信息区（图标+标题+用时+统计卡+鼓励）合并朗读 fullDescription，
 * 3 个按钮各自独立。本测试固化该契约，防止未来改动回退。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-port")
class CardsSessionCompleteAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun renderComplete() {
        composeRule.setContent {
            WenyanTheme(config = ThemeConfig(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                androidx.compose.foundation.layout.Box(Modifier.requiredSize(411.dp, 891.dp)) {
                    SessionCompleteState(
                        reviewedCount = 10,
                        againCount = 2,
                        sessionDurationMinutes = 5,
                        onRetry = {},
                        onUndo = {},
                        onExit = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `完成态 再复习按钮可独立触发`() {
        renderComplete()
        composeRule.onNodeWithText("再复习一轮")
            .assertHasClickAction()
    }

    @Test
    fun `完成态 撤销最后一张按钮可独立触发`() {
        renderComplete()
        composeRule.onNodeWithText("撤销最后一张")
            .assertHasClickAction()
    }

    @Test
    fun `完成态 返回知识点列表按钮可独立触发`() {
        renderComplete()
        composeRule.onNodeWithText("返回知识点列表")
            .assertHasClickAction()
    }

    @Test
    fun `完成态 统计信息区合并为单一朗读节点`() {
        renderComplete()
        // masteryRate = 10/(10+2) ≈ 0.83 → >=0.6 → "稳步进步，下次再战"
        composeRule.onNodeWithContentDescription(
            "本次复习完成，用时 5 分钟，共 10 张，其中 2 张需要重新记忆，稳步进步，下次再战",
        ).assertExists()
    }
}
