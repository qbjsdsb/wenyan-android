package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [WenyanLargeTopAppBar] 单元测试。
 *
 * 用 Robolectric 在 JVM 跑（无需 emulator），验证标题、副标题、返回按钮
 * 的渲染逻辑。SDK 锁定 34（Robolectric 对 35 支持有限，34 已足够覆盖 TopAppBar 渲染）。
 *
 * 注：测试调用 [WenyanLargeTopAppBar]（已 @OptIn ExperimentalMaterial3ExpressiveApi），
 * 调用方无需再次 OptIn —— opt-in 在声明位点生效。
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    // v0.9.35 审计修复（H2）：TopAppBar 按高度类降级——竖屏手机（高度 MEDIUM）
    // 用 Large 大标题栏；默认 Robolectric 设备（470dp 高）高度 COMPACT 会降级，
    // 指定竖屏手机尺寸保证测试验证 Large 模式
    qualifiers = "w411dp-h891dp-port",
)
class WenyanLargeTopAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun title_isDisplayed_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanLargeTopAppBar(title = "知识点")
                }
            }
        }
        composeRule.onNodeWithText("知识点").assertIsDisplayed()
    }

    @Test
    fun subtitle_isDisplayed_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanLargeTopAppBar(
                        title = "知识点详情",
                        subtitle = "鲁迅《狂人日记》",
                    )
                }
            }
        }
        composeRule.onNodeWithText("鲁迅《狂人日记》").assertIsDisplayed()
    }

    @Test
    fun backButton_isNotDisplayed_whenOnBackIsNull() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanLargeTopAppBar(title = "知识点")
                }
            }
        }
        // contentDescription = "返回" 的 IconButton 在 onBack=null 时不渲染
        composeRule.onNodeWithContentDescription("返回").assertDoesNotExist()
    }

    @Test
    fun backButton_isDisplayed_whenOnBackProvided() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanLargeTopAppBar(
                        title = "知识点",
                        onBack = { },
                    )
                }
            }
        }
        // 用 onNodeWithContentDescription 匹配 Icon 的 contentDescription
        // （onNodeWithText 只匹配 Text 节点，不匹配 contentDescription）
        composeRule.onNodeWithContentDescription("返回").assertIsDisplayed()
    }
}
