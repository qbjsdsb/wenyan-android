package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [shouldUseDualPane] 与 [AdaptiveWindowLayout] 单元测试（v0.9.34 横屏适配）。
 *
 * 判据：`maxWidth > maxHeight && maxWidth >= 600.dp`——
 * 精确捕获"横屏手机"（宽 > 高），不误触发平板竖屏（高 > 宽）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdaptiveWindowLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── shouldUseDualPane 纯函数边界 ──

    @Test
    fun `典型横屏手机 800x400 应启用双栏`() {
        assertTrue(shouldUseDualPane(maxWidth = 800.dp, maxHeight = 400.dp))
    }

    @Test
    fun `横屏边界 600x400 应启用双栏`() {
        assertTrue(shouldUseDualPane(maxWidth = 600.dp, maxHeight = 400.dp))
    }

    @Test
    fun `横屏但宽度不足 599x400 不应启用`() {
        // 599dp < 600dp：过窄窗口双栏后每栏不可用
        assertFalse(shouldUseDualPane(maxWidth = 599.dp, maxHeight = 400.dp))
    }

    @Test
    fun `竖屏 400x800 不应启用`() {
        assertFalse(shouldUseDualPane(maxWidth = 400.dp, maxHeight = 800.dp))
    }

    @Test
    fun `方形窗口 600x600 不应启用`() {
        // 宽 = 高：不满足宽 > 高
        assertFalse(shouldUseDualPane(maxWidth = 600.dp, maxHeight = 600.dp))
    }

    @Test
    fun `大平板竖屏 720x1000 不应启用`() {
        // 高度充足，单栏上下排布合理，无需双栏
        assertFalse(shouldUseDualPane(maxWidth = 720.dp, maxHeight = 1000.dp))
    }

    @Test
    fun `平板横屏 1000x700 应启用`() {
        assertTrue(shouldUseDualPane(maxWidth = 1000.dp, maxHeight = 700.dp))
    }

    // ── AdaptiveWindowLayout Compose 容器：尺寸注入验证 ──

    @Test
    fun `800x400 内容区内 isLandscape 为 true`() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    Box(modifier = Modifier.requiredSize(800.dp, 400.dp)) {
                        AdaptiveWindowLayout { layout ->
                            Text(if (layout.isLandscape) "landscape" else "portrait")
                        }
                    }
                }
            }
        }
        // assertExists：requiredSize 强制 800x400 约束传递，节点存在即证明分支执行
        composeRule.onNodeWithText("landscape").assertExists()
    }

    @Test
    fun `400x800 内容区内 isLandscape 为 false`() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    Box(modifier = Modifier.requiredSize(400.dp, 800.dp)) {
                        AdaptiveWindowLayout { layout ->
                            Text(if (layout.isLandscape) "landscape" else "portrait")
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText("portrait").assertExists()
    }

    @Test
    fun `AdaptiveWindowLayout 暴露内容区尺寸`() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    Box(modifier = Modifier.requiredSize(800.dp, 400.dp)) {
                        AdaptiveWindowLayout { layout ->
                            Text("w=${layout.maxWidth.value.toInt()} h=${layout.maxHeight.value.toInt()}")
                        }
                    }
                }
            }
        }
        // Robolectric 密度为 1（mdpi），800.dp → 800px，400.dp → 400px
        composeRule.onNodeWithText("w=800 h=400").assertExists()
    }
}
