package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenyan.app.core.database.entity.ContentSource
import com.wenyan.app.core.designsystem.theme.SourceAi
import com.wenyan.app.core.designsystem.theme.SourceHybrid
import com.wenyan.app.core.designsystem.theme.SourceMissing
import com.wenyan.app.core.designsystem.theme.SourceTextbook
import com.wenyan.app.core.designsystem.theme.SourceUser

/**
 * 内容来源五级标注 + MISSING 特殊状态标签（Spec 第 43-47 行、第 201-205 行）。
 *
 * UI 展示规则：
 * - TEXTBOOK_NATIVE → 绿色"资料"标签
 * - TEXTBOOK_OCR    → 绿色"资料"标签（带 OCR 角标）
 * - AI_GENERATED    → 蓝色"AI"标签
 * - HYBRID          → 黄色"资料+AI"标签
 * - USER_CREATED    → 灰色"我的"标签
 * - MISSING         → 红色"缺失"标签（提示用户手动处理）
 *
 * @param source 内容来源类型
 */
@Composable
fun ContentSourceBadge(source: ContentSource) {
    val config = when (source) {
        ContentSource.TEXTBOOK_NATIVE -> BadgeConfig(
            text = "资料",
            backgroundColor = SourceTextbook,
            textColor = Color.White,
        )
        ContentSource.TEXTBOOK_OCR -> BadgeConfig(
            text = "资料",
            ocrSuffix = "OCR",
            backgroundColor = SourceTextbook,
            textColor = Color.White,
        )
        ContentSource.AI_GENERATED -> BadgeConfig(
            text = "AI",
            backgroundColor = SourceAi,
            textColor = Color.White,
        )
        ContentSource.HYBRID -> BadgeConfig(
            text = "资料+AI",
            backgroundColor = SourceHybrid,
            textColor = Color(0xFF3D3D3D),
        )
        ContentSource.USER_CREATED -> BadgeConfig(
            text = "我的",
            backgroundColor = SourceUser,
            textColor = Color.White,
        )
        ContentSource.MISSING -> BadgeConfig(
            text = "缺失",
            backgroundColor = SourceMissing,
            textColor = Color.White,
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(config.backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = config.text,
            color = config.textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        // OCR 角标（TEXTBOOK_OCR 时显示）
        config.ocrSuffix?.let { suffix ->
            Text(
                text = suffix,
                color = config.textColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x33000000))
                    .padding(horizontal = 2.dp, vertical = 0.dp),
            )
        }
    }
}

/** 标签配置 */
private data class BadgeConfig(
    val text: String,
    val ocrSuffix: String? = null,
    val backgroundColor: Color,
    val textColor: Color,
)
