package com.wenyan.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
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
class HierarchicalListItemTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun title_isDisplayed_rootNode() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(title = "现代文学史", depth = 0)
                }
            }
        }
        composeRule.onNodeWithText("现代文学史").assertIsDisplayed()
    }

    @Test
    fun title_isDisplayed_childNode() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(title = "第一章", depth = 1)
                }
            }
        }
        composeRule.onNodeWithText("第一章").assertIsDisplayed()
    }

    @Test
    fun trailingContent_isDisplayed_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(
                        title = "现代文学史",
                        depth = 0,
                        trailing = { Text("已掌握") },
                    )
                }
            }
        }
        composeRule.onNodeWithText("现代文学史").assertIsDisplayed()
        composeRule.onNodeWithText("已掌握").assertIsDisplayed()
    }

    @Test
    fun onClick_invoked_whenClicked() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(
                        title = "可点击节点",
                        depth = 0,
                        onClick = { clicked = true },
                    )
                }
            }
        }
        composeRule.onNodeWithText("可点击节点").performClick()
        assert(clicked) { "onClick was not invoked" }
    }

    @Test
    fun trailingNotShown_whenOnClickProvided_andNoTrailing() {
        // 当有 onClick 但无 trailing 时，应显示 ChevronRight 箭头（不报错即可）
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    HierarchicalListItem(
                        title = "带箭头节点",
                        depth = 0,
                        onClick = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("带箭头节点").assertIsDisplayed()
    }
}
