package com.wenyan.app.core.fsrs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Task 30 - FsrsWrapper 单元测试。
 *
 * 验证 checklist C3.1-C3.5 项：FSRS-Kotlin 库集成、三档预设（名词解释/作品背诵/论述题/流派特征）、
 * 可提取性公式 R = (1 + t/(9*S))^(-1)、新卡初始稳定性/难度、间隔计算、
 * 保持率冲突解决（取 maxOf 不降级）、调度状态转换（NEW→LEARNING/REVIEW）、enableFuzz 控制。
 *
 * 运行环境：纯 JVM 单元测试（core:fsrs/src/test）。
 */
class FsrsWrapperTest {

    // ===================== C3.1: FSRS-Kotlin 库集成 =====================

    /** C3.1: FsrsWrapper 可实例化（FSRS-Kotlin 库正确集成） */
    @Test
    fun fsrsWrapper_isInstantiable() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        assertNotNull(wrapper)
    }

    // ===================== C3.2: 名词解释预设（TIER_FRAMEWORK） =====================

    /** C3.2: 名词解释预设 → TIER_FRAMEWORK，targetRetention=0.90/maxInterval=365/stabilityGrowthFactor=1.0/easyBonus=1.3/againPenalty=0.4 */
    @Test
    fun termExplanation_preset_matchesTierFramework() {
        val tier = ContentTierMapper.mapContentTypeToTier(ContentType.TERM_EXPLANATION)
        assertEquals(MemoryTier.TIER_FRAMEWORK, tier)
        val config = TIER_CONFIGS[tier]!!
        assertEquals(0.90f, config.targetRetention, 0.0001f)
        assertEquals(365, config.maxInterval)
        assertEquals(1.0f, config.stabilityGrowthFactor, 0.0001f)
        assertEquals(1.3f, config.easyBonus, 0.0001f)
        assertEquals(0.4f, config.againPenalty, 0.0001f)
        // 据此预设可构造 FsrsWrapper
        val wrapper = FsrsWrapper(
            requestRetention = config.targetRetention,
            maximumInterval = config.maxInterval,
            enableFuzz = ContentTierMapper.shouldEnableFuzz(ContentType.TERM_EXPLANATION),
            stabilityGrowthFactor = config.stabilityGrowthFactor,
            easyBonus = config.easyBonus,
            againPenalty = config.againPenalty,
        )
        assertNotNull(wrapper)
    }

    // ===================== C3.3: 作品背诵预设（TIER_EXACT, enableFuzz=false） =====================

    /** C3.3: 作品背诵预设 → TIER_EXACT，targetRetention=0.95/maxInterval=180/stabilityGrowthFactor=0.85/easyBonus=1.2/againPenalty=0.3/enableFuzz=false */
    @Test
    fun workRecitation_preset_matchesTierExact_enableFuzzFalse() {
        val tier = ContentTierMapper.mapContentTypeToTier(ContentType.WORK_RECITATION)
        assertEquals(MemoryTier.TIER_EXACT, tier)
        val config = TIER_CONFIGS[tier]!!
        assertEquals(0.95f, config.targetRetention, 0.0001f)
        assertEquals(180, config.maxInterval)
        assertEquals(0.85f, config.stabilityGrowthFactor, 0.0001f)
        assertEquals(1.2f, config.easyBonus, 0.0001f)
        assertEquals(0.3f, config.againPenalty, 0.0001f)
        // 精确记忆档要求 enableFuzz=false
        assertFalse(ContentTierMapper.shouldEnableFuzz(ContentType.WORK_RECITATION))
        val wrapper = FsrsWrapper(
            requestRetention = config.targetRetention,
            maximumInterval = config.maxInterval,
            enableFuzz = false,
            stabilityGrowthFactor = config.stabilityGrowthFactor,
            easyBonus = config.easyBonus,
            againPenalty = config.againPenalty,
        )
        assertNotNull(wrapper)
    }

    // ===================== C3.4: 论述题预设（TIER_UNDERSTAND） =====================

    /** C3.4: 论述题预设 → TIER_UNDERSTAND，targetRetention=0.85/maxInterval=720/stabilityGrowthFactor=1.15/easyBonus=1.5/againPenalty=0.5 */
    @Test
    fun essay_preset_matchesTierUnderstand() {
        val tier = ContentTierMapper.mapContentTypeToTier(ContentType.ESSAY)
        assertEquals(MemoryTier.TIER_UNDERSTAND, tier)
        val config = TIER_CONFIGS[tier]!!
        assertEquals(0.85f, config.targetRetention, 0.0001f)
        assertEquals(720, config.maxInterval)
        assertEquals(1.15f, config.stabilityGrowthFactor, 0.0001f)
        assertEquals(1.5f, config.easyBonus, 0.0001f)
        assertEquals(0.5f, config.againPenalty, 0.0001f)
        assertTrue(ContentTierMapper.shouldEnableFuzz(ContentType.ESSAY))
    }

    // ===================== C3.5: 流派特征预设（TIER_FRAMEWORK，同 C3.2） =====================

    /** C3.5: 流派特征预设 → TIER_FRAMEWORK（同 C3.2 参数） */
    @Test
    fun schoolFeature_preset_matchesTierFramework() {
        val tier = ContentTierMapper.mapContentTypeToTier(ContentType.SCHOOL_FEATURE)
        assertEquals(MemoryTier.TIER_FRAMEWORK, tier)
        val config = TIER_CONFIGS[tier]!!
        assertEquals(0.90f, config.targetRetention, 0.0001f)
        assertEquals(365, config.maxInterval)
        assertEquals(1.0f, config.stabilityGrowthFactor, 0.0001f)
        assertEquals(1.3f, config.easyBonus, 0.0001f)
        assertEquals(0.4f, config.againPenalty, 0.0001f)
        assertTrue(ContentTierMapper.shouldEnableFuzz(ContentType.SCHOOL_FEATURE))
    }

    // ===================== 可提取性公式 R = (1 + t/(9*S))^(-1) =====================

    /** 可提取性公式（stability=10, elapsedDays=0 → R=1；elapsedDays=9*S → R=0.5） */
    @Test
    fun retrievability_formula_correct() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val stability = 10f
        // t=0 → R = (1 + 0)^(-1) = 1
        assertEquals(1.0f, wrapper.retrievability(0f, stability), 0.0001f)
        // t = 9*S = 90 → R = (1 + 90/90)^(-1) = 0.5
        assertEquals(0.5f, wrapper.retrievability(9f * stability, stability), 0.0001f)
    }

    // ===================== 新卡初始稳定性 initStability =====================

    /** initStability（Rating.AGAIN 返回 w[0]=0.2172f） */
    @Test
    fun initStability_again_returnsW0() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        assertEquals(FsrsWrapper.DEFAULT_WEIGHTS[0], wrapper.initStability(Rating.AGAIN), 0.0001f)
        assertEquals(0.2172f, wrapper.initStability(Rating.AGAIN), 0.0001f)
    }

    // ===================== 初始难度 initDifficulty =====================

    /** initDifficulty（Rating.GOOD 返回 w[4]=4.7284f） */
    @Test
    fun initDifficulty_good_returnsW4() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        assertEquals(FsrsWrapper.DEFAULT_WEIGHTS[4], wrapper.initDifficulty(Rating.GOOD), 0.0001f)
        assertEquals(4.7284f, wrapper.initDifficulty(Rating.GOOD), 0.0001f)
    }

    // ===================== 间隔计算 nextInterval =====================

    /** nextInterval 公式 I = 9*S*(1/R_target - 1)，stability=10/requestRetention=0.9 → I≈10 */
    @Test
    fun nextInterval_formula_correct() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val stability = 10f
        // 公式 I = 9*S*(1/R_target - 1) = 9*10*(1/0.9 - 1) ≈ 10
        val expected = (9f * stability * (1f / 0.9f - 1f)).toInt()
        assertEquals(expected, wrapper.nextInterval(stability))
        assertTrue(
            "interval 应约为 10，实际 ${wrapper.nextInterval(stability)}",
            wrapper.nextInterval(stability) in 9..11
        )
    }

    // ===================== resolveRetention 取 maxOf（不降级） =====================

    /** resolveRetention 取 maxOf（卡片档位 0.95 + 全局 0.85 → 0.95，不降级） */
    @Test
    fun resolveRetention_takesMaxOf() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        // TIER_EXACT 的 targetRetention=0.95，全局 0.85 → 取较高值 0.95
        assertEquals(0.95f, wrapper.resolveRetention(MemoryTier.TIER_EXACT, 0.85f), 0.0001f)
        // 卡片档位低于全局时取全局（TIER_UNDERSTAND 0.85 + 全局 0.90 → 0.90）
        assertEquals(0.90f, wrapper.resolveRetention(MemoryTier.TIER_UNDERSTAND, 0.90f), 0.0001f)
    }

    // ===================== schedule 调度新卡状态转换 =====================

    /** schedule 调度新卡后 state 从 NEW 变为 LEARNING/REVIEW */
    @Test
    fun schedule_newCard_transitionsStateFromNew() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        val newCard = FlashCard(state = State.NEW)
        assertEquals(State.NEW, newCard.state)

        // AGAIN → LEARNING
        val afterAgain = wrapper.schedule(newCard, Rating.AGAIN, now)
        assertEquals(State.LEARNING, afterAgain.state)
        // GOOD → REVIEW
        val afterGood = wrapper.schedule(newCard, Rating.GOOD, now)
        assertEquals(State.REVIEW, afterGood.state)
    }

    // ===================== enableFuzz=false 不应用模糊因子 =====================

    /** enableFuzz=false 时调度结果确定性，无随机性（不应用模糊因子） */
    @Test
    fun enableFuzzFalse_isDeterministic() {
        val wrapper = FsrsWrapper(
            requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false
        )
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        // REVIEW 状态、stability 较大，使 interval 落入 fuzz 生效区间（>=2.5）
        val reviewCard = FlashCard(
            state = State.REVIEW,
            stability = 50f,
            difficulty = 5f,
            lastReview = now.minusDays(30),
            reps = 5,
        )
        val intervals = (1..30).map {
            wrapper.schedule(reviewCard, Rating.GOOD, now).scheduledDays
        }.toSet()
        assertEquals("enableFuzz=false 应产生确定性间隔", 1, intervals.size)
    }
}
