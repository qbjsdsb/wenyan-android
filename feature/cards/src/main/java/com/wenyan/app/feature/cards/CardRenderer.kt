package com.wenyan.app.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.data.cards.ClozeQuoteCard
import com.wenyan.app.core.data.cards.DistinctionCard
import com.wenyan.app.core.data.cards.EssayPointsCard
import com.wenyan.app.core.data.cards.SchoolComparisonCard
import com.wenyan.app.core.data.cards.SchoolInfo
import com.wenyan.app.core.data.cards.TermExplanationCard
import com.wenyan.app.core.data.cards.LearningUnitCard
import com.wenyan.app.core.data.cards.TermCategory
import com.wenyan.app.core.data.cards.WorkAuthorBidirectionalCard
import com.wenyan.app.core.designsystem.component.Spacing

/**
 * 卡片内容渲染入口（Task 17.8）。
 *
 * 根据 [card] 的 sealed class 子类类型分发到对应渲染函数，
 * 每种模板有专属渲染样式。卡片模板=内容组织维度，与设计文档3.3.2节
 * 5种背诵模式（复习方式维度）正交。
 *
 * @param card 卡片模板实例
 * @param isFlipped 是否已翻转（true 显示背面答案，false 显示正面问题）
 */
@Composable
fun CardContent(
    card: com.wenyan.app.core.data.cards.CardTemplate,
    isFlipped: Boolean,
) {
    when (card) {
        is TermExplanationCard -> TermExplanationContent(card, isFlipped)
        is ClozeQuoteCard -> ClozeQuoteContent(card, isFlipped)
        is WorkAuthorBidirectionalCard -> WorkAuthorContent(card, isFlipped)
        is EssayPointsCard -> EssayPointsContent(card, isFlipped)
        is SchoolComparisonCard -> SchoolComparisonContent(card, isFlipped)
        is DistinctionCard -> DistinctionContent(card, isFlipped)
        is LearningUnitCard -> Text(
            text = if (isFlipped) card.back else card.front,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// ---------- 名词解释卡：分条列表样式 ----------

/**
 * 名词解释卡渲染（社团类/作品类分条列表）。
 * - 正面：显示名词与维度
 * - 背面：按 [TermCategory] 分条列出对应字段
 */
@Composable
private fun TermExplanationContent(card: TermExplanationCard, isFlipped: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (!isFlipped) {
            Text(
                text = card.front,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "（名词解释 · ${categoryLabel(card.category)}）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = card.back,
                style = MaterialTheme.typography.bodyLarge,
            )
            // 若有结构化字段则分条列出
            card.society?.let { SocietyFieldsList(it) }
            card.work?.let { WorkFieldsList(it) }
            // 完整解释（coreConclusion），解决拆分后片段信息密度低的问题
            card.fullExplanation?.takeIf { it.isNotBlank() && it != card.back }?.let { explanation ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = Spacing.sm),
                )
                Text(
                    text = "完整解释",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 教材原文（studyText，袁行霈版）
            card.studyText?.takeIf { it.isNotBlank() && it != card.back && it != card.fullExplanation }?.let { study ->
                if (card.fullExplanation?.takeIf { it.isNotBlank() && it != card.back } == null) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = Spacing.sm),
                    )
                }
                Text(
                    text = "教材原文（袁行霈）",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = study,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SocietyFieldsList(fields: com.wenyan.app.core.data.cards.SocietyTermFields) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (fields.time.isNotBlank()) FieldRow("时间", fields.time)
        if (fields.place.isNotBlank()) FieldRow("地点", fields.place)
        if (fields.members.isNotBlank()) FieldRow("人物", fields.members)
        if (fields.publication.isNotBlank()) FieldRow("刊物", fields.publication)
        if (fields.proposition.isNotBlank()) FieldRow("主张", fields.proposition)
        if (fields.contribution.isNotBlank()) FieldRow("贡献", fields.contribution)
    }
}

@Composable
private fun WorkFieldsList(fields: com.wenyan.app.core.data.cards.WorkTermFields) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (fields.author.isNotBlank()) FieldRow("作者", fields.author)
        if (fields.era.isNotBlank()) FieldRow("年代", fields.era)
        if (fields.content.isNotBlank()) FieldRow("内容", fields.content)
        if (fields.feature.isNotBlank()) FieldRow("特色", fields.feature)
        if (fields.influence.isNotBlank()) FieldRow("影响", fields.influence)
    }
}

@Composable
private fun FieldRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = Spacing.sm),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            // 长内容（尤其是教材原文）必须在剩余宽度内换行；否则 Row 会把
            // 文本测量成无限宽，横屏和大字体下直接裁切出卡片边界。
            modifier = Modifier.weight(1f),
        )
    }
}

private fun categoryLabel(category: TermCategory): String = when (category) {
    TermCategory.SOCIETY -> "社团类"
    TermCategory.WORK -> "作品类"
}

// ---------- Cloze名句填空卡：填空样式 ----------

/**
 * Cloze名句填空卡渲染。
 * - 正面：名句挖空呈现 + 语法情感提示
 * - 背面：完整名句与填空答案
 */
@Composable
private fun ClozeQuoteContent(card: ClozeQuoteCard, isFlipped: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!isFlipped) {
            // v0.8.14 P0-6 修复:blank 为空字符串时,String.replace("", "＿＿＿＿")
            // 会在每个字符位置插入"＿＿＿＿",导致正面显示完全不可读的乱码。
            // 边界:OCR 提取失败或 seed 数据缺失时 blank 可能为空。
            // 修复:blank 为空时直接显示原 quote(用户至少能读到完整名句,虽无填空提示)。
            val displayQuote = if (card.blank.isBlank()) {
                card.quote
            } else {
                card.quote.replace(card.blank, "＿＿＿＿")
            }
            Text(
                text = displayQuote,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "提示：${card.hint}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
        } else {
            Text(
                text = card.quote,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "答案：${card.blank}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------- 作品-作者双向卡：简洁问答样式 ----------

/**
 * 作品-作者双向卡渲染。
 * - 正面：作品名或作者名（双向卡正反各一张）
 * - 背面：对应作者或作品
 */
@Composable
private fun WorkAuthorContent(card: WorkAuthorBidirectionalCard, isFlipped: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (!isFlipped) {
                Text(
                    text = card.front,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "对应的${if (card.front == card.work) "作者" else "作品"}是？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = card.back,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---------- 论述要点卡：关键词提纲样式 ----------

/**
 * 论述要点卡渲染。
 * - 正面：论述题问题
 * - 背面：关键词提示列表（非完整答案，适配 Outline 提纲背诵模式）
 */
@Composable
private fun EssayPointsContent(card: EssayPointsCard, isFlipped: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (!isFlipped) {
            Text(
                text = "论述题",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = card.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(
                text = "关键词提示：",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            // v0.8.13 P2-1:序号用 primary 色 + SemiBold,提升视觉层次
            // 原实现序号与正文同色同重,扫读时难以快速定位要点
            card.keyPoints.forEachIndexed { index, point ->
                Row(modifier = Modifier.fillMaxWidth().padding(start = Spacing.sm)) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = Spacing.sm),
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

// ---------- 流派对照卡：表格化样式 ----------

/**
 * 流派对照卡渲染（表格化对比京派/海派/新月派/象征派等）。
 * - 正面：对照主题
 * - 背面：各流派信息表格化展示
 */
@Composable
private fun SchoolComparisonContent(card: SchoolComparisonCard, isFlipped: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (!isFlipped) {
            Text(
                text = card.front,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "（流派对照 · 共${card.schools.size}个流派）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // v0.8.12 P2-8：修复尾部分割线，用 forEachIndexed 跳过最后一个
            card.schools.forEachIndexed { index, school ->
                SchoolRow(school)
                if (index < card.schools.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SchoolRow(school: SchoolInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        // NF-C10：2.dp 不在 Spacing 6 级 token 体系（最小 xs=4.dp），
        // 此处为流派对照表格的紧凑行内间距，刻意小于 xs 以呈现表格化密度，保留字面量。
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = school.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        // v0.8.16 P2-A 修复：空字段过滤，避免显示"时期： "等空标签。
        // 原实现 FieldRow 无条件渲染所有标签，SchoolInfo 的 period/representatives/
        // proposition/features 任一为空时仍显示"时期："，视觉空洞且误导用户以为流派信息缺失。
        // SocietyFieldsList/WorkFieldsList 已做 isNotBlank 过滤，SchoolRow 对齐修复。
        if (school.period.isNotBlank()) FieldRow("时期", school.period)
        if (school.representatives.isNotBlank()) FieldRow("代表", school.representatives)
        if (school.proposition.isNotBlank()) FieldRow("主张", school.proposition)
        if (school.features.isNotBlank()) FieldRow("特色", school.features)
    }
}

// ---------- 区分卡：对比样式 ----------

/**
 * 区分卡渲染（易混淆作家/作品对比，正反面都出）。
 * - 正面：两项对比标题
 * - 背面：区别要点列表
 */
@Composable
private fun DistinctionContent(card: DistinctionCard, isFlipped: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (!isFlipped) {
            Text(
                text = "易混淆区分",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // v0.8.13 P2-2:VS 排版优化为左右分栏 + VerticalDivider
            // 原实现用 SpaceEvenly 排列 item1 + "VS" + item2,
            // 当 item1/item2 文字长度差异大时视觉不平衡,且无视觉分隔。
            // 改为 Row + weight(1f) 左右等分,中间 VerticalDivider 强调对比关系。
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = card.item1,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
                Text(
                    text = card.item2,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Text(
                text = "${card.item1} 与 ${card.item2} 的区别：",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            card.differences.forEachIndexed { index, diff ->
                Text(
                    text = "${index + 1}. $diff",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}
