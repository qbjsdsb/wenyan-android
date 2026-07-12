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
 */
@Singleton
class AntiRoteMemorization @Inject constructor(
    private val reviewLogDao: ReviewLogDao,
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
     * - 卡片自身连续正确 ≥ [STREAK_THRESHOLD]
     * - 关联卡片错误率 ≥ [RELATED_ERROR_THRESHOLD]
     *
     * 两个条件必须同时满足才判定为疑似死记硬背。
     */
    private suspend fun detectRotePattern(cardId: String, relatedCardIds: List<String>): Boolean {
        val consecutiveCorrect = getConsecutiveCorrectCount(cardId)
        val relatedErrorRate = getRelatedErrorRate(relatedCardIds)
        return consecutiveCorrect >= STREAK_THRESHOLD && relatedErrorRate >= RELATED_ERROR_THRESHOLD
    }

    /**
     * 获取卡片自身连续正确次数。
     *
     * 从最新复习记录开始往前遍历：
     * - 遇到 GOOD 或 EASY → 计数 +1
     * - 遇到 AGAIN 或 HARD → 停止计数
     *
     * @param cardId 卡片 ID
     * @return 连续正确次数
     */
    private suspend fun getConsecutiveCorrectCount(cardId: String): Int {
        val logs = reviewLogDao.getByPointOrderByCreatedDesc(cardId)
        var count = 0
        for (log in logs) {
            when (log.rating.uppercase()) {
                RATING_GOOD, RATING_EASY -> count++
                else -> break  // AGAIN 或 HARD，停止计数
            }
        }
        return count
    }

    /**
     * 获取关联卡片错误率。
     *
     * 统计关联卡片所有复习记录中 AGAIN 评级的占比。
     *
     * @param relatedCardIds 关联卡片 ID 列表
     * @return 错误率（0-1），无记录时返回 0
     */
    private suspend fun getRelatedErrorRate(relatedCardIds: List<String>): Float {
        if (relatedCardIds.isEmpty()) return 0f
        val logs = reviewLogDao.getByPointIds(relatedCardIds)
        if (logs.isEmpty()) return 0f
        val againCount = logs.count { it.rating.uppercase() == RATING_AGAIN }
        return againCount.toFloat() / logs.size
    }

    companion object {
        /** 自身连续正确次数阈值（≥此值视为"始终正确"） */
        private val STREAK_THRESHOLD = 5

        /** 关联卡片错误率阈值（≥此值视为"频繁出错"） */
        private val RELATED_ERROR_THRESHOLD = 0.4f

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
