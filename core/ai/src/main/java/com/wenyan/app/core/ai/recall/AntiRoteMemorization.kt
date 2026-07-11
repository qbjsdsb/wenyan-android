package com.wenyan.app.core.ai.recall

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

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
 */
class AntiRoteMemorization @Inject constructor(

) {

    /**
     * 检测某卡片是否疑似死记硬背。
     *
     * 判定依据：
     * 1. 该卡片自身连续正确次数 ≥ [STREAK_THRESHOLD]（始终"正确"）
     * 2. 其关联卡片（同一知识点的其他卡片）频繁出错
     * 3. 关联卡片错误率 ≥ [RELATED_ERROR_THRESHOLD]
     *
     * @param cardId 待检测卡片 ID
     * @param relatedCardIds 关联卡片 ID 列表（同一知识点的其他卡片）
     * @return 检测结果，若疑似死记硬背则建议安排变体出题/反向提问
     */
    fun checkRoteMemorization(
        cardId: String,
        relatedCardIds: List<String>,
    ): Flow<RoteCheckResult> = flow {
        // TODO: 查询卡片复习历史，获取连续正确次数和关联卡片错误率
        // 当前为骨架实现，后续接入复习日志查询

        val isSuspected = detectRotePattern(cardId, relatedCardIds)

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
     * - 卡片自身连续正确 ≥ 阈值
     * - 关联卡片错误率 ≥ 阈值
     */
    private suspend fun detectRotePattern(cardId: String, relatedCardIds: List<String>): Boolean {
        // TODO: 查询复习日志获取实际数据
        // val consecutiveCorrect = getConsecutiveCorrectCount(cardId)
        // val relatedErrorRate = getRelatedErrorRate(relatedCardIds)
        // return consecutiveCorrect >= STREAK_THRESHOLD && relatedErrorRate >= RELATED_ERROR_THRESHOLD
        return false
    }

    companion object {
        /** 自身连续正确次数阈值（≥此值视为"始终正确"） */
        private const val STREAK_THRESHOLD = 5

        /** 关联卡片错误率阈值（≥此值视为"频繁出错"） */
        private const val RELATED_ERROR_THRESHOLD = 0.4f
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
