package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * 文研 Large 风格 TopAppBar（M3 Expressive）。
 *
 * 基于 material3 1.5.0-alpha18 的 [LargeFlexibleTopAppBar]。注意：在 alpha18 中
 * 该 API 仍标记为 [ExperimentalMaterial3ExpressiveApi]（@RequiresOptIn），
 * 计划中提到的 "alpha23 graduated Stable" 我们未使用，故本封装必须显式 OptIn。
 *
 * 与 [LargeFlexibleTopAppBar] 原生 API 的差异：
 * - title 接收 String 而非 @Composable lambda，简化调用方
 * - subtitle 同样接收 String?
 * - onBack 为 null 时不显示返回按钮（与 [WenyanTopAppBar] 行为一致）
 * - 容器色统一用 surfaceContainer，标题色 onSurface
 *
 * 与 [WenyanTopAppBar] 的关键差异：
 * - 支持滚动折叠（透传 [scrollBehavior]）
 * - 支持副标题（[subtitle]），展开时显示在标题下方
 * - 展开时为大标题样式（headlineMedium），收起时为标准标题样式
 *
 * @param title 标题文本
 * @param modifier 修饰符
 * @param subtitle 副标题文本，可选（如知识点分类、章节归属）
 * @param onBack 返回按钮回调，为 null 时不显示返回按钮
 * @param actions 右侧操作区
 * @param scrollBehavior 滚动行为，配合 LazyColumn / Column 的 nestedScroll 使用；
 *        为 null 时不响应滚动（适用于内容不滚动的页面，仅享受 Large 标题样式）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WenyanLargeTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    LargeFlexibleTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        subtitle = subtitle?.let {
            @Composable {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            }
        },
        actions = actions,
        titleHorizontalAlignment = Alignment.Start,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        scrollBehavior = scrollBehavior,
    )
}
