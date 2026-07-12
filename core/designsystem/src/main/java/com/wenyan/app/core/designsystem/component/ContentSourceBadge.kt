package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 内容来源标注类型（Spec Task 24 五级标注 + 1 特殊状态）。
 */
object ContentSource {
    const val TEXTBOOK_NATIVE = "TEXTBOOK_NATIVE"
    const val TEXTBOOK_OCR = "TEXTBOOK_OCR"
    const val AI_GENERATED = "AI_GENERATED"
    const val HYBRID = "HYBRID"
    const val USER_CREATED = "USER_CREATED"
    const val MISSING = "MISSING"
}

/**
 * 内容来源五级颜色标签组件（M3 Expressive 主题角色色版）。
 *
 * 颜色映射：
 * - TEXTBOOK_NATIVE / TEXTBOOK_OCR → secondaryContainer / onSecondaryContainer
 * - AI_GENERATED → tertiaryContainer / onTertiaryContainer
 * - HYBRID → surfaceContainerHighest / onSurfaceVariant
 * - USER_CREATED → surfaceContainerHigh / onSurfaceVariant
 * - MISSING → errorContainer / onErrorContainer
 *
 * 若 [stageLabel] 非空，则显示苏格拉底引导阶段标签（tertiaryContainer），
 * 优先级高于 [contentSource]。
 */
@Composable
fun ContentSourceBadge(
    contentSource: String?,
    modifier: Modifier = Modifier,
    stageLabel: String? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val config = when {
        stageLabel != null -> BadgeConfig(
            text = stageLabel,
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
            showWarning = false,
        )
        contentSource == ContentSource.TEXTBOOK_NATIVE -> BadgeConfig(
            text = "资料",
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
            showWarning = false,
        )
        contentSource == ContentSource.TEXTBOOK_OCR -> BadgeConfig(
            text = "资料·OCR",
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
            showWarning = false,
        )
        contentSource == ContentSource.AI_GENERATED -> BadgeConfig(
            text = "AI",
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
            showWarning = false,
        )
        contentSource == ContentSource.HYBRID -> BadgeConfig(
            text = "资料+AI",
            containerColor = colorScheme.surfaceContainerHighest,
            contentColor = colorScheme.onSurfaceVariant,
            showWarning = false,
        )
        contentSource == ContentSource.USER_CREATED -> BadgeConfig(
            text = "我的",
            containerColor = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurfaceVariant,
            showWarning = false,
        )
        contentSource == ContentSource.MISSING -> BadgeConfig(
            text = "缺失",
            containerColor = colorScheme.errorContainer,
            contentColor = colorScheme.onErrorContainer,
            showWarning = true,
        )
        else -> return
    }

    Surface(
        color = config.containerColor,
        contentColor = config.contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (config.showWarning) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = config.text,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private data class BadgeConfig(
    val text: String,
    val containerColor: Color,
    val contentColor: Color,
    val showWarning: Boolean,
)
