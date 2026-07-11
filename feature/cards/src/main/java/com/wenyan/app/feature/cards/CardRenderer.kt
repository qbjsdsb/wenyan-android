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
import com.wenyan.app.core.data.cards.TermCategory
import com.wenyan.app.core.data.cards.WorkAuthorBidirectionalCard

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isFlipped) {
            Text(
                text = card.front,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
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
        }
    }
}

@Composable
private fun SocietyFieldsList(fields: com.wenyan.app.core.data.cards.SocietyTermFields) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!isFlipped) {
            Text(
                text = card.quote.replace(card.blank, "＿＿＿＿"),
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
                    modifier = Modifier.padding(8.dp),
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
                fontWeight = FontWeight.Bold,
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
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!isFlipped) {
                Text(
                    text = card.front,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.Bold,
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text(
                text = "关键词提示：",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            card.keyPoints.forEachIndexed { index, point ->
                Text(
                    text = "${index + 1}. $point",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isFlipped) {
            Text(
                text = card.front,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "（流派对照 · 共${card.schools.size}个流派）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            card.schools.forEach { school ->
                SchoolRow(school)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SchoolRow(school: SchoolInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = school.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        FieldRow("时期", school.period)
        FieldRow("代表", school.representatives)
        FieldRow("主张", school.proposition)
        FieldRow("特色", school.features)
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!isFlipped) {
            Text(
                text = "易混淆区分",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = card.item1,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = card.item2,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
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
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
