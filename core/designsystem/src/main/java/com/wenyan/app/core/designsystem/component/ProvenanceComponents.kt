package com.wenyan.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.designsystem.theme.WenyanTheme

enum class ProvenanceTone { REVIEWED, LEGACY, DRAFT, REJECTED }

data class ProvenancePresentation(
    val label: String,
    val supportingText: String,
    val tone: ProvenanceTone,
)

/** Unknown values deliberately inherit the restrained legacy presentation. */
fun provenancePresentation(status: String?): ProvenancePresentation = when (status) {
    "REVIEWED" -> ProvenancePresentation("已审校", "来源信息已人工核对", ProvenanceTone.REVIEWED)
    "AI_DRAFT" -> ProvenancePresentation("AI 草稿", "仅供整理，暂不进入正式学习", ProvenanceTone.DRAFT)
    "REJECTED" -> ProvenancePresentation("已退回", "内容未通过审校", ProvenanceTone.REJECTED)
    else -> ProvenancePresentation("历史资料", "尚未完成新版来源审校", ProvenanceTone.LEGACY)
}

@Composable
fun ProvenanceBadge(status: String?, modifier: Modifier = Modifier) {
    val presentation = provenancePresentation(status)
    val colors = provenanceColors(presentation.tone)
    Surface(
        modifier = modifier,
        color = colors.first,
        contentColor = colors.second,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(provenanceIcon(presentation.tone), contentDescription = null)
            Text(presentation.label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

data class ProvenanceSourceUiModel(
    val title: String,
    val evidenceStatus: String? = null,
    val edition: String? = null,
    val pageStart: Int? = null,
    val pageEnd: Int? = null,
    val reviewNote: String? = null,
)

fun sourceEvidenceLabel(status: String?): String = when (status) {
    "OFFICIAL_ORIGINAL" -> "官方原始资料"
    "USER_CONFIRMED" -> "用户已确认"
    "SECONDARY_RECOLLECTION" -> "二手转述"
    else -> "来源类型待确认"
}

fun pageRangeLabel(start: Int?, end: Int?): String? = when {
    start == null && end == null -> null
    start != null && end != null && start == end -> "第 $start 页"
    start != null && end != null -> "第 $start–$end 页"
    start != null -> "第 $start 页起"
    else -> "至第 $end 页"
}

/** Empty or blank sources render nothing, preventing invented titles/page labels. */
@Composable
fun SourceSection(
    sources: List<ProvenanceSourceUiModel>,
    modifier: Modifier = Modifier,
    title: String = "来源（${sources.count { it.title.isNotBlank() }}）",
) {
    val visibleSources = sources.filter { it.title.isNotBlank() }
    if (visibleSources.isEmpty()) return
    GroupedCard(title = title, modifier = modifier) {
        visibleSources.forEachIndexed { index, source ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(source.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(sourceEvidenceLabel(source.evidenceStatus), style = MaterialTheme.typography.labelSmall)
                    source.edition?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall)
                    }
                    pageRangeLabel(source.pageStart, source.pageEnd)?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall)
                    }
                }
                source.reviewNote?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (index != visibleSources.lastIndex) GroupedCardDivider()
        }
    }
}

@Composable
private fun provenanceColors(tone: ProvenanceTone): Pair<Color, Color> = when (tone) {
    ProvenanceTone.REVIEWED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    ProvenanceTone.LEGACY -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    ProvenanceTone.DRAFT -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    ProvenanceTone.REJECTED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}

private fun provenanceIcon(tone: ProvenanceTone): ImageVector = when (tone) {
    ProvenanceTone.REVIEWED -> Icons.Default.CheckCircle
    ProvenanceTone.LEGACY -> Icons.Default.History
    ProvenanceTone.DRAFT -> Icons.Default.Science
    ProvenanceTone.REJECTED -> Icons.Default.Source
}

@Preview(name = "Multiple long sources", widthDp = 420, fontScale = 1.5f, showBackground = true)
@Preview(name = "Landscape large type", widthDp = 840, heightDp = 360, fontScale = 2f, showBackground = true)
@Composable
private fun SourceSectionPreview() {
    WenyanTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProvenanceBadge("LEGACY_UNVERIFIED")
            SourceSection(
                listOf(
                    ProvenanceSourceUiModel(
                        title = "一部名称非常长、用于验证大字体与横屏自动换行能力的教材资料",
                        evidenceStatus = "OFFICIAL_ORIGINAL",
                        edition = "第三版",
                        pageStart = 120,
                        pageEnd = 128,
                    ),
                    ProvenanceSourceUiModel("用户核对的补充资料", "USER_CONFIRMED"),
                ),
            )
        }
    }
}
