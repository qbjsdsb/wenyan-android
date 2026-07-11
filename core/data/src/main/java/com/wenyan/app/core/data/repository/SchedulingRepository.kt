package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.mapper.MemoRecordMapper
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.fsrs.FsrsWrapper
import com.wenyan.app.core.fsrs.MemoryTier
import com.wenyan.app.core.fsrs.Rating
import com.wenyan.app.core.fsrs.ReviewLog
import com.wenyan.app.core.fsrs.TIER_CONFIGS
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调度仓库（阶段3：FSRS调度接通）。
 *
 * 桥接 UI 卡片评分与 FSRS 调度算法，是评分调度的核心入口：
 *
 * 流程：UI 评分 → 读 MemoRecord → 转 FlashCard → 按 tier 构造 FsrsWrapper
 *      → schedule() → 转 MemoRecord → upsert 回数据库
 *
 * 设计决策（阶段3）：
 * - FsrsWrapper 不通过 Hilt DI 注入，而是按 tier 动态构造（无状态纯算法，构造成本极低）
 * - tier 由 CardTemplateType 推断（6种卡片类型 → 3档记忆强度）
 * - enableFuzz 在 TIER_EXACT 档关闭（精确到天），其他档开启（避免间隔过于规律）
 *
 * @property memoRecordDao 背诵记录 DAO（读写 memo_records 表）
 */
@Singleton
class SchedulingRepository @Inject constructor(
    private val memoRecordDao: MemoRecordDao,
) {
    /**
     * 评分调度：根据用户评分更新知识点的 FSRS 调度状态。
     *
     * @param pointId  知识点 ID（对应 memo_records.point_id 主键）
     * @param rating   用户评分（AGAIN/HARD/GOOD/EASY）
     * @param cardType 卡片模板类型（用于推断 tier，决定 FSRS 参数）
     *
     * @return 更新后的 MemoRecordEntity（已写入数据库），调用方可读取 nextReviewAt 等
     */
    suspend fun rateCard(
        pointId: String,
        rating: Rating,
        cardType: CardTemplateType,
    ): MemoRecordEntity? {
        if (pointId.isBlank()) return null

        // 1. 读取现有 MemoRecord（不存在则创建默认记录）
        val existingMemo = memoRecordDao.getById(pointId) ?: createDefaultMemoRecord(pointId)

        // 2. 按 tier 构造 FsrsWrapper（无状态，每次按需构造）
        val tier = mapCardTypeToTier(cardType)
        val config = TIER_CONFIGS[tier] ?: TIER_CONFIGS[MemoryTier.TIER_FRAMEWORK]!!
        val wrapper = FsrsWrapper(
            requestRetention = config.targetRetention,
            maximumInterval = config.maxInterval,
            enableFuzz = tier != MemoryTier.TIER_EXACT,
            stabilityGrowthFactor = config.stabilityGrowthFactor,
            easyBonus = config.easyBonus,
            againPenalty = config.againPenalty,
        )

        // 3. MemoRecord → FlashCard（调度前状态）
        val now = LocalDateTime.now()
        val flashCardBefore = MemoRecordMapper.toFlashCard(existingMemo)

        // 4. FSRS 调度
        val flashCardAfter = wrapper.schedule(flashCardBefore, rating, now)

        // 5. 构造 ReviewLog（调度前状态 + 调度后 scheduledDays）
        val reviewLog = ReviewLog(
            rating = rating,
            state = flashCardBefore.state,
            dueDate = flashCardBefore.dueDate,
            stability = flashCardBefore.stability,
            difficulty = flashCardBefore.difficulty,
            elapsedDays = flashCardBefore.elapsedDays,
            lastElapsedDays = flashCardBefore.scheduledDays,
            scheduledDays = flashCardAfter.scheduledDays,
            reviewTime = now,
        )

        // 6. FlashCard → MemoRecord（调度后状态）+ 持久化
        val updatedMemo = MemoRecordMapper.toMemoRecord(
            flashCard = flashCardAfter,
            pointId = pointId,
            inPriorityQueue = existingMemo.inPriorityQueue != 0,
            reviewLog = reviewLog,
            existingHistoryJson = existingMemo.history ?: "[]",
        )
        memoRecordDao.upsert(updatedMemo)
        return updatedMemo
    }

    /**
     * 卡片模板类型 → 记忆档位映射
     *
     * 设计依据（spec 第301-305行 + 设计文档3.3.4节）：
     * - TIER_EXACT（精确档，R_target=0.95）：名句填空，需逐字精确
     * - TIER_FRAMEWORK（框架档，R_target=0.90）：名词解释、作品作者、流派对照、区分卡
     * - TIER_UNDERSTAND（理解档，R_target=0.85）：论述要点，重在思路而非原文
     */
    private fun mapCardTypeToTier(cardType: CardTemplateType): MemoryTier = when (cardType) {
        CardTemplateType.CLOZE_QUOTE -> MemoryTier.TIER_EXACT
        CardTemplateType.TERM_EXPLANATION -> MemoryTier.TIER_FRAMEWORK
        CardTemplateType.WORK_AUTHOR_BIDIRECTIONAL -> MemoryTier.TIER_FRAMEWORK
        CardTemplateType.SCHOOL_COMPARISON -> MemoryTier.TIER_FRAMEWORK
        CardTemplateType.DISTINCTION -> MemoryTier.TIER_FRAMEWORK
        CardTemplateType.ESSAY_POINTS -> MemoryTier.TIER_UNDERSTAND
    }

    /**
     * 创建默认 MemoRecord（兜底：知识点无对应记录时自动创建）
     *
     * 状态 NEW，stability=0，difficulty=5.0，nextReviewAt=now（立即到期）
     */
    private fun createDefaultMemoRecord(pointId: String): MemoRecordEntity {
        val now = System.currentTimeMillis()
        return MemoRecordEntity(
            pointId = pointId,
            state = "NEW",
            stability = 0.0,
            difficulty = 5.0,
            lastReviewAt = now,
            nextReviewAt = now,
            reviewCount = 0,
            failCount = 0,
            elapsedDays = 0,
            scheduledDays = 0,
            reps = 0,
            history = "[]",
            inPriorityQueue = 0,
        )
    }
}
