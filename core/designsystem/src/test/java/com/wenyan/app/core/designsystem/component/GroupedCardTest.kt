package com.wenyan.app.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupedCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun title_isDisplayed_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观设置") {
                        GroupedCardItem(title = "主题模式")
                    }
                }
            }
        }
        composeRule.onNodeWithText("外观设置").assertIsDisplayed()
    }

    @Test
    fun itemTitle_isDisplayed() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(title = "AMOLED 纯黑模式")
                    }
                }
            }
        }
        composeRule.onNodeWithText("AMOLED 纯黑模式").assertIsDisplayed()
    }

    @Test
    fun itemSubtitle_isDisplayed_onRightSide() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "关于") {
                        GroupedCardItem(title = "版本", subtitle = "v0.1.0")
                    }
                }
            }
        }
        composeRule.onNodeWithText("版本").assertIsDisplayed()
        composeRule.onNodeWithText("v0.1.0").assertIsDisplayed()
    }

    @Test
    fun itemDescription_isDisplayed_belowTitle() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(
                            title = "AMOLED 纯黑模式",
                            description = "深色模式下使用纯黑背景，节省 OLED 电量",
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText("AMOLED 纯黑模式").assertIsDisplayed()
        composeRule.onNodeWithText("深色模式下使用纯黑背景，节省 OLED 电量").assertIsDisplayed()
    }

    @Test
    fun clickableItem_exposesClickAction_andInvokesCallback() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "关联知识点") {
                        GroupedCardItem(
                            title = "《新诗集》与早期白话诗的生成",
                            onClick = { clicked = true },
                        )
                    }
                }
            }
        }

        composeRule
            .onNodeWithText("《新诗集》与早期白话诗的生成")
            .assertHasClickAction()
            .performClick()
        assertTrue("可点击关联项应触发 onClick", clicked)
    }

    @Test
    fun itemLeadingIcon_isDecorative_byDefault() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(
                            title = "主题模式",
                            leadingIcon = Icons.Default.Palette,
                        )
                    }
                }
            }
        }
        // title 文字显示
        composeRule.onNodeWithText("主题模式").assertIsDisplayed()
        // leadingIcon 默认为装饰性（contentDescription = null），不应有 "主题模式" 的 contentDescription
        composeRule.onNodeWithContentDescription("主题模式").assertDoesNotExist()
    }

    @Test
    fun itemLeadingIcon_hasContentDescription_whenExplicitlySet() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(
                            title = "主题模式",
                            leadingIcon = Icons.Default.Palette,
                            leadingIconContentDescription = "调色板",
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText("主题模式").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("调色板").assertIsDisplayed()
    }

    @Test
    fun itemTrailing_switch_isDisplayed() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    GroupedCard(title = "外观") {
                        GroupedCardItem(
                            title = "AMOLED",
                            trailing = {
                                Switch(checked = false, onCheckedChange = {})
                            },
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText("AMOLED").assertIsDisplayed()
    }
}
