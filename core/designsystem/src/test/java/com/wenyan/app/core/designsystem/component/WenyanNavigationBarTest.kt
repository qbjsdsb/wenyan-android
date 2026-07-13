package com.wenyan.app.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WenyanNavigationBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleItems = listOf(
        WenyanNavItem("knowledge", "知识点", Icons.Default.LibraryBooks),
        WenyanNavItem("quiz", "真题", Icons.Default.BarChart),
    )

    @Test
    fun labels_areDisplayed_forAllItems() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanNavigationBar(
                        items = sampleItems,
                        currentRoute = "knowledge",
                        onNavigate = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("知识点").assertIsDisplayed()
        composeRule.onNodeWithText("真题").assertIsDisplayed()
    }

    @Test
    fun items_haveClickAction_forAccessibility() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanNavigationBar(
                        items = sampleItems,
                        currentRoute = "knowledge",
                        onNavigate = {},
                    )
                }
            }
        }
        // NavigationBarItem 合并语义后，label 节点应具有点击行为（供 TalkBack 等读屏软件触发）
        composeRule.onNodeWithText("知识点").assertHasClickAction()
        composeRule.onNodeWithText("真题").assertHasClickAction()
    }

    @Test
    fun onNavigate_invoked_whenItemClicked() {
        var clickedRoute: String? = null
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WenyanNavigationBar(
                        items = sampleItems,
                        currentRoute = "knowledge",
                        onNavigate = { clickedRoute = it },
                    )
                }
            }
        }
        composeRule.onNodeWithText("真题").performClick()
        assert(clickedRoute == "quiz") { "Expected clickedRoute to be 'quiz', was $clickedRoute" }
    }
}
