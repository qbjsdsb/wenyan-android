package com.wenyan.app.feature.knowledge

import androidx.compose.runtime.Immutable
import com.wenyan.app.core.database.entity.LearningUnitWithRecord

@Immutable
data class KnowledgeProgressUiModel(
    val seen: String,
    val remembered: String,
    val writable: String,
    val explanation: String,
)

/**
 * Produces deliberately coarse, reproducible progress labels. It does not turn
 * FSRS values into a made-up percentage and does not infer writing ability from
 * browsing or card ratings.
 */
internal fun calculateKnowledgeProgress(
    units: List<LearningUnitWithRecord>,
    nowMillis: Long,
): KnowledgeProgressUiModel {
    val activeRecords = units.mapNotNull(LearningUnitWithRecord::record)
    val practiced = activeRecords.count { it.reviewCount > 0 }
    val currentlyRemembered = activeRecords.count {
        it.reviewCount > 0 && it.nextReviewAt > nowMillis && it.state != "NEW"
    }
    val total = units.size
    return KnowledgeProgressUiModel(
        seen = when {
            total == 0 -> "尚未生成学习单元"
            practiced == 0 -> "尚未学习"
            practiced < total -> "已学习 $practiced/$total 个单元"
            else -> "已学习全部 $total 个单元"
        },
        remembered = when {
            practiced == 0 -> "尚无回忆记录"
            currentlyRemembered == 0 -> "当前均待复习"
            currentlyRemembered < practiced -> "$currentlyRemembered/$practiced 个已练单元未到期"
            else -> "全部已练单元未到期"
        },
        writable = "尚未练习",
        explanation = "见过来自实际学习记录；记得依据学习单元的真实复习与到期状态；写得出需真实作答或写作记录。",
    )
}
