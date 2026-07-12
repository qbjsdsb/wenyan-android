package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.designsystem.theme.SourceAi
import com.wenyan.app.core.designsystem.theme.SourceHybrid
import com.wenyan.app.core.designsystem.theme.SourceMissing
import com.wenyan.app.core.designsystem.theme.SourceTextbook
import com.wenyan.app.core.designsystem.theme.SourceUser

/**
 * 内容来源标注类型（Spec Task 24 五级标注 + 1 特殊状态）。
 *
 * - [TEXTBOOK_NATIVE]：原生电子文本 → 绿色"资料"
 * - [TEXTBOOK_OCR]：扫描OCR文本 → 绿色"资料·OCR"
 * - [AI_GENERATED]：AI生成 → 蓝色"AI"
 * - [HYBRID]：混合 → 黄色"资料+AI"
 * - [USER_CREATED]：用户创建 → 灰色"我的"
 * - [MISSING]：OCR失败/资料缺失 → 红色"缺失"
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
 * 内容来源五级颜色标签组件（Spec C5.8-C5.13a）。
 *
 * 根据 [contentSource] 显示对应颜色的标签：
 * - 绿色（资料）：TEXTBOOK_NATIVE / TEXTBOOK_OCR
 * - 蓝色（AI）：AI_GENERATED
 * - 黄色（资料+AI）：HYBRID
 * - 灰色（我的）：USER_CREATED
 * - 红色（缺失）：MISSING + 警告图标
 *
 * 若 [stageLabel] 非空，则显示苏格拉底引导阶段标签（蓝色），
 * 优先级高于 [contentSource]。
 *
 * @param contentSource 内容来源类型（见 [ContentSource] 常量）
 * @param stageLabel 苏格拉底引导阶段标签（如"论证分析 · AI引导"），为空时忽略
 * @param modifier 修饰符
 */
@Composable
fun ContentSourceBadge(
    contentSource: String?,
    modifier: Modifier = Modifier,
    stageLabel: String? = null,
) {
    val config = when {
        stageLabel != null -> BadgeConfig(
            text = stageLabel,
            backgroundColor = SourceAi,
            textColor = Color.White,
            showWarning = false,
        )
        contentSource == ContentSource.TEXTBOOK_NATIVE -> BadgeConfig(
            text = "资料",
            backgroundColor = SourceTextbook,
            textColor = Color.White,
            showWarning = false,
        )
        contentSource == ContentSource.TEXTBOOK_OCR -> BadgeConfig(
            text = "资料·OCR",
            backgroundColor = SourceTextbook,
            textColor = Color.White,
            showWarning = false,
        )
        contentSource == ContentSource.AI_GENERATED -> BadgeConfig(
            text = "AI",
            backgroundColor = SourceAi,
            textColor = Color.White,
            showWarning = false,
        )
        contentSource == ContentSource.HYBRID -> BadgeConfig(
            text = "资料+AI",
            backgroundColor = SourceHybrid,
            textColor = Color(0xFF1A1A1A),
            showWarning = false,
        )
        contentSource == ContentSource.USER_CREATED -> BadgeConfig(
            text = "我的",
            backgroundColor = SourceUser,
            textColor = Color.White,
            showWarning = false,
        )
        contentSource == ContentSource.MISSING -> BadgeConfig(
            text = "缺失",
            backgroundColor = SourceMissing,
            textColor = Color.White,
            showWarning = true,
        )
        else -> return
    }

    Surface(
        color = config.backgroundColor,
        shape = RoundedCornerShape(4.dp),
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
                    tint = config.textColor,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = config.text,
                color = config.textColor,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private data class BadgeConfig(
    val text: String,
    val backgroundColor: Color,
    val textColor: Color,
    val showWarning: Boolean,
)
