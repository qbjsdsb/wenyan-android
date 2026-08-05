package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [WenyanLargeTopAppBar] 窄横屏降级测试（v0.9.35）。
 *
 * 覆盖 H2 审计修复：高度 COMPACT（<480dp）的窄横屏（如 411×360）应降级为
 * 标准 64dp TopAppBar 而非 152dp Large 大标题——subtitle 合并进标题行
 * （"title · subtitle"），避免大标题吃光本就紧张的垂直内容区。
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h360dp-land",
)
class WenyanLargeTopAppBarLandscapeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `高度COMPACT窄横屏降级为标准栏并合并subtitle`() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanLargeTopAppBar(
                        title = "知识点",
                        subtitle = "鲁迅《狂人日记》",
                    )
                }
            }
        }
        // 降级模式 subtitle 合并到标题行（" · " 分隔）；Large 模式 subtitle 单独显示
        composeRule.onNodeWithText("知识点 · 鲁迅《狂人日记》").assertIsDisplayed()
    }
}
