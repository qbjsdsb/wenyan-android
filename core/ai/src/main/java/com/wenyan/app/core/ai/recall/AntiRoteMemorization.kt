package com.wenyan.app.core.ai.recall

import com.wenyan.app.core.database.dao.ReviewLogDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 防"背关键词但不懂含义"检测器（Spec 第 419-423 行新增，设计文档无此功能）。
 *
 * 检测逻辑：
 * - 检测到某卡片始终"正确"但其关联卡片频繁出错时，降低该卡片置信度
 * - 安排变体出题（同一知识点不同问法）
 * - 安排反向提问
 * - 关联检测
 *
 * 典型场景：用户死记硬背了"建安风骨"的关键词，但无法回答关联的
 * "建安七子有哪些""建安文学的时代背景"等问题。
 *
 * 阶段4实现：
 * - 注入 [ReviewLogDao] 查询复习历史
 * - [getConsecutiveCorrectCount] 从最新记录开始数连续 GOOD/EASY
 * - [getRelatedErrorRate] 计算关联卡片中 AGAIN 评级占比
 * - 判定条件：连续正确 ≥ [STREAK_THRESHOLD] 且关联错误率 ≥ [RELATED_ERROR_THRESHOLD]
 *
 * ⚠ P1-AUDIT-3 已知差距（未实现，需作为独立 feature 排期）：
 * - **仅检测不干预**：Spec 要求疑似时"降低置信度 + 安排变体出题 + 反向提问"，
 *   当前仅返回 suggestion 文本，无任何干预动作。
 * - **生产链路未接通**：[checkRoteMemorization] 在生产 UI 中无调用方，
 *   AiAssistantViewModel 有包装方法但无 Screen 触发。
 * - 完整实现需：LLM 变体题生成 + FSRS 置信度调整 + UI 接通触发时机。
 *
 * NF-T6 防御性编码：[ReviewLogEntity.rating] 声明为非空 String（Room 生成 NOT NULL 约束），
 * `?.uppercase()` 作为防御性编程，保护潜在 schema 变更或 DB 边界情况下的 NPE。
 *
 * NF-BB6 已知限制：无关联卡片时返回 0f（被判 false），新卡无法检测死记硬背。
 * 这是保守策略（无证据 → 不标记），完整修复需增加其他检测信号（如响应时间分析）。
 */
@Singleton
class AntiRoteMemorization @Inject constructor(
    private val reviewLogDao: ReviewLogDao,
) {

    /**
     * 检测某知识点是否疑似死记硬背。
     *
     * 判定依据：
     * 1. 该知识点自身连续正确次数 ≥ [STREAK_THRESHOLD]（始终"正确"）
     * 2. 其关联知识点频繁出错
     * 3. 关联知识点错误率 ≥ [RELATED_ERROR_THRESHOLD]
     *
     * P1-AUDIT-3 修复：参数名 `cardId` → `pointId`，`relatedCardIds` → `relatedPointIds`，
     * 与 DAO（[ReviewLogDao.getByPointOrderByCreatedDesc]）和
     * [com.wenyan.app.core.data.repository.SchedulingRepository] 命名对齐。
     * 原命名误导读者以为存在独立"卡片表"，实际查的是 review_logs.point_id（知识点维度）。
     *
     * @param pointId 待检测知识点 ID
     * @param relatedPointIds 关联知识点 ID 列表（同一知识点的其他卡片）
     * @return 检测结果，若疑似死记硬背则建议安排变体出题/反向提问
     */
    fun checkRoteMemorization(
        pointId: String,
        relatedPointIds: List<String>,
    ): Flow<RoteCheckResult> = flow {
        val isSuspected = detectRotePattern(pointId, relatedPointIds)

        val suggestion = if (isSuspected) {
            "该卡片可能存在死记硬背，建议安排变体出题（同一知识点不同问法）/ 反向提问 / 关联检测"
        } else {
            "复习表现正常"
        }

        emit(RoteCheckResult(
            isSuspected = isSuspected,
            suggestion = suggestion,
        ))
    }

    /**
     * 检测死记硬背模式。
     *
     * 条件：
     * - 知识点自身连续正确 ≥ [STREAK_THRESHOLD]
     * - 关联知识点错误率 ≥ [RELATED_ERROR_THRESHOLD]
     *
     * 两个条件必须同时满足才判定为疑似死记硬背。
     */
    private suspend fun detectRotePattern(pointId: String, relatedPointIds: List<String>): Boolean {
        val consecutiveCorrect = getConsecutiveCorrectCount(pointId)
        val relatedErrorRate = getRelatedErrorRate(relatedPointIds)
        return consecutiveCorrect >= STREAK_THRESHOLD && relatedErrorRate >= RELATED_ERROR_THRESHOLD
    }

    /**
     * 获取知识点自身连续正确次数。
     *
     * 从最新复习记录开始往前遍历：
     * - 遇到 GOOD 或 EASY → 计数 +1
     * - 遇到 AGAIN 或 HARD → 停止计数
     * - rating 为 null（防御性，当前不会发生）→ 停止计数
     *
     * @param pointId 知识点 ID
     * @return 连续正确次数
     */
    private suspend fun getConsecutiveCorrectCount(pointId: String): Int {
        val logs = reviewLogDao.getByPointOrderByCreatedDesc(pointId)
        var count = 0
        for (log in logs) {
            // 防御性 ?.uppercase()：rating 字段非空，但保护潜在 schema 变更
            when (log.rating?.uppercase()) {
                RATING_GOOD, RATING_EASY -> count++
                else -> break  // null / AGAIN / HARD，停止计数
            }
        }
        return count
    }

    /**
     * 获取关联知识点错误率。
     *
     * 统计关联知识点所有复习记录中 AGAIN 评级的占比。
     *
     * @param relatedPointIds 关联知识点 ID 列表
     * @return 错误率（0-1），无关联或无记录时返回 0（保守策略：无证据 → 不标记）
     */
    private suspend fun getRelatedErrorRate(relatedPointIds: List<String>): Float {
        if (relatedPointIds.isEmpty()) return 0f
        val logs = reviewLogDao.getByPointIds(relatedPointIds)
        if (logs.isEmpty()) return 0f
        // 防御性 ?.uppercase()：rating 字段非空，但保护潜在 schema 变更
        val againCount = logs.count { it.rating?.uppercase() == RATING_AGAIN }
        return againCount.toFloat() / logs.size
    }

    companion object {
        /** 自身连续正确次数阈值（≥此值视为"始终正确"） */
        private const val STREAK_THRESHOLD = 5

        /** 关联知识点错误率阈值（≥此值视为"频繁出错"） */
        private const val RELATED_ERROR_THRESHOLD = 0.4f

        /** Rating 字段值常量（与 Rating enum 的 name 一致，大写） */
        private const val RATING_AGAIN = "AGAIN"
        private const val RATING_GOOD = "GOOD"
        private const val RATING_EASY = "EASY"
    }
}

/**
 * 死记硬背检测结果。
 *
 * @param isSuspected 是否疑似死记硬背
 * @param suggestion 建议（"安排变体出题/反向提问"）
 */
data class RoteCheckResult(
    val isSuspected: Boolean,
    val suggestion: String,
)
