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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
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
 * - onBack 为 null 时不显示返回按钮
 * - 容器色统一用 surfaceContainer，标题色 onSurface
 *
 * 特性：
 * - 支持滚动折叠（透传 [scrollBehavior]）
 * - 支持副标题（[subtitle]），展开时显示在标题下方
 * - 展开时为大标题样式（headlineMedium），收起时为标准标题样式
 *
 * **v0.8.15 Stage 2: 横屏自动降级**
 * - Compact（手机竖屏 < 600dp）：保持 [LargeFlexibleTopAppBar]（展开大标题，体验不变）
 * - Medium/Expanded（横屏/平板 ≥ 600dp）：自动降级为 [TopAppBar]（标准高度 64dp，
 *   节省垂直空间，避免横屏下 Large 标题挤压内容区）
 * - subtitle 在 Compact 模式下显示在标题下方，在降级模式下合并到标题行尾
 *   （用 " · " 分隔，例如 "知识点详情 · 高频 · 难度3/5"），保留信息不丢失
 * - 9 个调用点无需任何改动，全部自动适配
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
    // v0.8.15 Stage 2: 横屏/平板（Medium/Expanded）下 Large 标题展开态过高（152dp），
    // 挤压本就紧张的垂直内容区。降级为标准 TopAppBar（64dp）节省 88dp 垂直空间。
    // v0.9.35 审计修复（H2）：COMPACT 窄横屏（如 540×360 小折叠屏）宽高类均为
    // COMPACT，若仅按宽度类判 Large，152dp 大标题 + 80dp 底栏会吃光内容区
    // （仅剩 ~100dp）。补高度类：高度 COMPACT（<480dp）一律降级标准栏。
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val useLarge = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT &&
        windowSizeClass.windowHeightSizeClass != WindowHeightSizeClass.COMPACT

    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    )

    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                )
            }
        }
    }

    if (useLarge) {
        // Compact（手机竖屏）：保持 LargeFlexibleTopAppBar 体验不变
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
            navigationIcon = navigationIcon,
            actions = actions,
            titleHorizontalAlignment = Alignment.Start,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    } else {
        // Medium/Expanded（横屏/平板）：降级为标准 TopAppBar，subtitle 合并到标题行
        // 用 " · " 分隔保留信息（如 "知识点详情 · 高频 · 难度3/5"）
        val combinedTitle = if (subtitle != null) "$title · $subtitle" else title
        TopAppBar(
            title = {
                Text(
                    text = combinedTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    }
}
