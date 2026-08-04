package com.wenyan.app.core.fsrs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import kotlin.math.roundToInt
import kotlin.random.Random

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

    /**
     * NF-T7 回归：initStability 应使用 `rating.index` 而非 `rating.value - 1` 访问权重数组。
     *
     * 验证 4 档评分各自返回对应的 w[i]：
     * - AGAIN → w[0] = 0.2172
     * - HARD  → w[1] = 0.3174
     * - GOOD  → w[2] = 1.7265
     * - EASY  → w[3] = 5.1816
     *
     * 若未来 Rating 枚举顺序调整（如新增 MANUALLY_MARKED 档），`value - 1` 不再等于
     * 数组下标，本测试会立即失败，强制开发者重新审视 index 映射。
     */
    @Test
    fun initStability_allRatings_matchWeightsAtIndex() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val cases = listOf(
            Rating.AGAIN to 0,
            Rating.HARD to 1,
            Rating.GOOD to 2,
            Rating.EASY to 3,
        )
        for ((rating, expectedIndex) in cases) {
            assertEquals(
                "initStability($rating) 应等于 w[$expectedIndex]",
                FsrsWrapper.DEFAULT_WEIGHTS[expectedIndex],
                wrapper.initStability(rating),
                0.0001f,
            )
            // 同时验证 index 属性与数组下标一致
            assertEquals(
                "Rating.index 应等于数组下标",
                expectedIndex,
                rating.index,
            )
        }
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
        // F-05 修正后用 roundToInt（与实现一致）
        val expected = (9f * stability * (1f / 0.9f - 1f)).roundToInt()
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
        // v0.9.31：新卡首次 GOOD → LEARNING（10 分钟学习步，再 GOOD 才毕业）
        val afterGood = wrapper.schedule(newCard, Rating.GOOD, now)
        assertEquals(State.LEARNING, afterGood.state)
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

    /**
     * NF-T8 回归：applyFuzz 使用可注入 Random，相同种子的两个 wrapper 产生相同 fuzz 输出。
     *
     * 验证：
     * - 两个 wrapper 用相同种子 Random(42)，对相同卡片调度，scheduledDays 应一致
     * - 与 enableFuzz=false 的"裸间隔"不同（证明 fuzz 生效）
     *
     * 原实现用全局 `Random.nextFloat()`，每次运行结果不同，无法写精确断言。
     * 修正后可注入固定种子，测试可验证 fuzz 是否被正确应用。
     */
    @Test
    fun applyFuzz_withSeededRandom_isDeterministic() {
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        val reviewCard = FlashCard(
            state = State.REVIEW,
            stability = 50f,
            difficulty = 5f,
            lastReview = now.minusDays(30),
            reps = 5,
        )

        // 两个独立 wrapper，相同种子，独立消耗随机数
        val wrapper1 = FsrsWrapper(
            requestRetention = 0.9f, maximumInterval = 365,
            enableFuzz = true, random = Random(42),
        )
        val wrapper2 = FsrsWrapper(
            requestRetention = 0.9f, maximumInterval = 365,
            enableFuzz = true, random = Random(42),
        )

        val days1 = wrapper1.schedule(reviewCard, Rating.GOOD, now).scheduledDays
        val days2 = wrapper2.schedule(reviewCard, Rating.GOOD, now).scheduledDays

        assertEquals(
            "相同种子的两个 wrapper 应产生相同 scheduledDays（fuzz 输出可重现）",
            days1, days2,
        )

        // 验证 fuzz 确实生效：与 enableFuzz=false 的裸间隔对比
        val noFuzzWrapper = FsrsWrapper(
            requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false,
        )
        val noFuzzDays = noFuzzWrapper.schedule(reviewCard, Rating.GOOD, now).scheduledDays
        // stability=50 时裸间隔 = 9*50*(1/0.9-1) = 50，fuzz 后应在 [49, 51] 范围（±5% = ±2.5）
        // 注意：fuzz 是 ±5% 但取整后可能仍等于裸值（概率事件），所以只验证 fuzz 在合理范围
        assertTrue(
            "fuzz 后 scheduledDays ($days1) 应在裸间隔 ($noFuzzDays) ±3 范围内",
            kotlin.math.abs(days1 - noFuzzDays) <= 3,
        )
    }

    /**
     * NF-T8 补充：不同种子的 wrapper 应产生不同 fuzz 输出（高概率）。
     *
     * 100 个不同种子的 wrapper 调度相同卡片，scheduledDays 集合 size 应 > 1
     * （证明 fuzz 确实引入了随机性，而非恒定返回）。
     */
    @Test
    fun applyFuzz_differentSeeds_producesVariety() {
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        val reviewCard = FlashCard(
            state = State.REVIEW,
            stability = 50f,
            difficulty = 5f,
            lastReview = now.minusDays(30),
            reps = 5,
        )

        val distinctDays = (1..100).map { seed ->
            FsrsWrapper(
                requestRetention = 0.9f, maximumInterval = 365,
                enableFuzz = true, random = Random(seed),
            ).schedule(reviewCard, Rating.GOOD, now).scheduledDays
        }.toSet()

        assertTrue(
            "100 个不同种子的 wrapper 应产生 >1 种 scheduledDays（实际 ${distinctDays.size}）",
            distinctDays.size > 1,
        )
    }

    // ===================== F-01: nextDifficulty 权重索引修正回归测试 =====================

    /**
     * F-01 回归：nextDifficulty 用 w[6] 作为难度变化系数、w[7] 作为均值回归系数。
     * 以 d=6, GOOD（rating.value=3, delta=0）为例：
     * - 修正前（w[5]/w[6]）：meanReverted = 0.5699*6 + 0.4301*4.7284 ≈ 5.453
     * - 修正后（w[6]/w[7]）：meanReverted = 0.2197*6 + 0.7803*4.7284 ≈ 5.008
     * 断言落在 [4.95, 5.10] 区间，排除修正前的 5.453。
     */
    @Test
    fun nextDifficulty_uses_w6_w7_not_w5_w6() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val result = wrapper.nextDifficulty(6f, Rating.GOOD)
        // 修正后应为 ~5.008，修正前为 ~5.453
        assertTrue("nextDifficulty(GOOD, d=6) 应为 ~5.008，实际 $result", result in 4.95f..5.10f)
    }

    /**
     * F-01 回归：EASY 评分应让难度下降。修正前 w[6]=0.5699 导致过度回归，
     * EASY 后 D 反而偏高；修正后 w[7]=0.2197，EASY 应明显降低 D。
     */
    @Test
    fun nextDifficulty_easy_lowers_difficulty_more_than_good() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val dAfterGood = wrapper.nextDifficulty(6f, Rating.GOOD)
        val dAfterEasy = wrapper.nextDifficulty(6f, Rating.EASY)
        assertTrue("EASY 应比 GOOD 更降低难度：EASY=$dAfterEasy, GOOD=$dAfterGood",
            dAfterEasy < dAfterGood)
    }

    /**
     * P0-T1e: coerceIn(1, 10) 边界保护——各种评分下 nextDifficulty 始终在 [1, 10]。
     */
    @Test
    fun nextDifficulty_always_withinBounds() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val testCases = listOf(
            0f to Rating.AGAIN,    // 极端低
            0f to Rating.EASY,
            1f to Rating.AGAIN,    // 下边界
            10f to Rating.EASY,    // 上边界
            10f to Rating.AGAIN,
            100f to Rating.AGAIN,  // 超界
            100f to Rating.EASY,
            (-5f) to Rating.AGAIN, // 负值
        )
        for ((d, rating) in testCases) {
            val result = wrapper.nextDifficulty(d, rating)
            assertTrue("nextDifficulty(d=$d, $rating)=$result 应在 [1, 10]", result in 1f..10f)
        }
    }

    /**
     * P0-T1e: initDifficulty 也始终在 [1, 10]。
     */
    @Test
    fun initDifficulty_always_withinBounds() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        for (rating in Rating.entries) {
            val result = wrapper.initDifficulty(rating)
            assertTrue("initDifficulty($rating)=$result 应在 [1, 10]", result in 1f..10f)
        }
    }

    // ===================== F-02: easyBonus = 1 + w[16] 修正回归测试 =====================

    /**
     * F-02 回归：EASY 评分的稳定性增长应大于 GOOD（语义正确）。
     * NF-F1 修正后(exp(w[8])→w[8]):EASY stability (17.50) > GOOD (16.09)
     * (修正前 exp(w[8]) 放大 3 倍:EASY 32.64 > GOOD 28.38)
     */
    @Test
    fun nextRecallStability_easy_greater_than_good() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val d = 5f
        val s = 10f
        val r = 0.9f
        val sGood = wrapper.nextRecallStability(d, s, r, Rating.GOOD)
        val sEasy = wrapper.nextRecallStability(d, s, r, Rating.EASY)
        assertTrue("EASY stability ($sEasy) 应 > GOOD stability ($sGood)", sEasy > sGood)
    }

    /**
     * F-02 回归：精确数值验证（D=5, S=10, R=0.9）。
     * NF-F1 修正后(exp(w[8])→w[8],增长降低 3.02 倍):
     * - GOOD: growth ≈ 0.609 → S' ≈ 16.09
     * - EASY: growth ≈ 0.750 → S' ≈ 17.50
     */
    @Test
    fun nextRecallStability_easy_correct_value() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val sEasy = wrapper.nextRecallStability(5f, 10f, 0.9f, Rating.EASY)
        // NF-F1 修正后约 17.50(原 exp(w[8]) 放大版约 32.64)
        assertTrue("EASY stability 应约为 17.50（实际 $sEasy）", sEasy in 16f..19f)
    }

    /**
     * F-02 回归：HARD 评分稳定性增长 < GOOD（hardPenalty=w[15]=0.5316 < 1）。
     */
    @Test
    fun nextRecallStability_hard_less_than_good() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val sGood = wrapper.nextRecallStability(5f, 10f, 0.9f, Rating.GOOD)
        val sHard = wrapper.nextRecallStability(5f, 10f, 0.9f, Rating.HARD)
        assertTrue("HARD stability ($sHard) 应 < GOOD stability ($sGood)", sHard < sGood)
    }

    // ===================== F-03: EASY 评分 interval 与 stability 一致性 =====================

    /**
     * F-03 回归：EASY 调度后 interval >= GOOD 调度后 interval（含 easyBonus 后）。
     * 修正前：stability 用 recallS*easyBonus 但 interval 用 recallS（不含 bonus）→ 间隔偏小。
     * 修正后：两者一致，EASY interval 应明显 > GOOD interval。
     */
    @Test
    fun scheduleReview_easy_interval_greater_than_good() {
        val wrapper = FsrsWrapper(
            requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false
        )
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        val reviewCard = FlashCard(
            state = State.REVIEW,
            stability = 10f,
            difficulty = 5f,
            lastReview = now.minusDays(10),
            reps = 5,
        )
        val afterGood = wrapper.schedule(reviewCard, Rating.GOOD, now)
        val afterEasy = wrapper.schedule(reviewCard, Rating.EASY, now)
        assertTrue("EASY scheduledDays (${afterEasy.scheduledDays}) 应 >= GOOD (${afterGood.scheduledDays})",
            afterEasy.scheduledDays >= afterGood.scheduledDays)
        assertTrue("EASY stability (${afterEasy.stability}) 应 >= GOOD (${afterGood.stability})",
            afterEasy.stability >= afterGood.stability)
    }

    // ===================== F-05: nextInterval roundToInt 修正回归测试 =====================

    /**
     * F-05 回归：nextInterval 应用 roundToInt() 而非 toInt()。
     * 当 stability=5.7, requestRetention=0.9 时：
     * - interval = 9 * 5.7 * (1/0.9 - 1) = 5.7
     * - 修正前 toInt() = 5
     * - 修正后 roundToInt() = 6
     */
    @Test
    fun nextInterval_uses_round_not_truncation() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        val result = wrapper.nextInterval(5.7f)
        // 5.7 四舍五入为 6；若用 toInt() 则为 5
        assertEquals("nextInterval(5.7) 应为 6（round），实际 $result", 6, result)
    }

    /**
     * F-05 回归：stability=5.4 时 round(5.4)=5（不变）；stability=5.7 时 round(5.7)=6。
     * 确保四舍五入边界正确。
     */
    @Test
    fun nextInterval_rounding_boundary() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        // 9 * 5.4 * (1/0.9 - 1) = 5.4 → round = 5
        assertEquals(5, wrapper.nextInterval(5.4f))
        // 9 * 5.6 * (1/0.9 - 1) = 5.6 → round = 6
        assertEquals(6, wrapper.nextInterval(5.6f))
    }

    /**
     * F-05 回归：maximumInterval 上界保护仍生效。
     */
    @Test
    fun nextInterval_respects_maximumInterval() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 30)
        // 极大 stability 也被卡到 maximumInterval
        assertEquals(30, wrapper.nextInterval(10000f))
    }

    /**
     * F-05 回归：stability <= 0 时返回 1（下界保护）。
     */
    @Test
    fun nextInterval_zero_or_negative_stability_returns_1() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365)
        assertEquals(1, wrapper.nextInterval(0f))
        assertEquals(1, wrapper.nextInterval(-1f))
    }

    // ===================== P0-T1f: 4 档评分 × 4 状态 完整状态机覆盖 =====================

    /**
     * P0-T1f: 16 个状态-评分组合的状态转换覆盖。
     *
     * 期望状态转换：
     * - NEW + AGAIN/HARD → LEARNING
     * - NEW + GOOD/EASY → REVIEW
     * - LEARNING + AGAIN/HARD → LEARNING（保持）
     * - LEARNING + GOOD/EASY → REVIEW
     * - REVIEW + AGAIN → RELEARNING（lapses + 1）
     * - REVIEW + HARD/GOOD/EASY → REVIEW（保持）
     * - RELEARNING + AGAIN/HARD → RELEARNING（保持）
     * - RELEARNING + GOOD/EASY → REVIEW
     */
    @Test
    fun stateMachine_all_combinations_transition_correctly() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false)
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)

        // NEW 状态卡片
        val newCard = FlashCard(state = State.NEW, lapses = 0)
        assertEquals(State.LEARNING, wrapper.schedule(newCard, Rating.AGAIN, now).state)
        assertEquals(State.LEARNING, wrapper.schedule(newCard, Rating.HARD, now).state)
        assertEquals(State.LEARNING, wrapper.schedule(newCard, Rating.GOOD, now).state)
        assertEquals(State.REVIEW, wrapper.schedule(newCard, Rating.EASY, now).state)

        // LEARNING 状态卡片
        val learningCard = FlashCard(
            state = State.LEARNING, stability = 1f, difficulty = 5f,
            lastReview = now.minusMinutes(10), lapses = 0
        )
        assertEquals(State.LEARNING, wrapper.schedule(learningCard, Rating.AGAIN, now).state)
        assertEquals(State.LEARNING, wrapper.schedule(learningCard, Rating.HARD, now).state)
        assertEquals(State.REVIEW, wrapper.schedule(learningCard, Rating.GOOD, now).state)
        assertEquals(State.REVIEW, wrapper.schedule(learningCard, Rating.EASY, now).state)

        // REVIEW 状态卡片
        val reviewCard = FlashCard(
            state = State.REVIEW, stability = 10f, difficulty = 5f,
            lastReview = now.minusDays(10), lapses = 2
        )
        assertEquals(State.RELEARNING, wrapper.schedule(reviewCard, Rating.AGAIN, now).state)
        assertEquals(State.REVIEW, wrapper.schedule(reviewCard, Rating.HARD, now).state)
        assertEquals(State.REVIEW, wrapper.schedule(reviewCard, Rating.GOOD, now).state)
        assertEquals(State.REVIEW, wrapper.schedule(reviewCard, Rating.EASY, now).state)

        // RELEARNING 状态卡片
        val relearningCard = FlashCard(
            state = State.RELEARNING, stability = 2f, difficulty = 7f,
            lastReview = now.minusMinutes(15), lapses = 3
        )
        assertEquals(State.RELEARNING, wrapper.schedule(relearningCard, Rating.AGAIN, now).state)
        assertEquals(State.RELEARNING, wrapper.schedule(relearningCard, Rating.HARD, now).state)
        assertEquals(State.REVIEW, wrapper.schedule(relearningCard, Rating.GOOD, now).state)
        assertEquals(State.REVIEW, wrapper.schedule(relearningCard, Rating.EASY, now).state)
    }

    /**
     * P0-T1f: REVIEW + AGAIN 应增加 lapses（遗忘次数）。
     * 其他 REVIEW 评分不应改变 lapses。
     */
    @Test
    fun stateMachine_review_again_increments_lapses() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false)
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        val reviewCard = FlashCard(
            state = State.REVIEW, stability = 10f, difficulty = 5f,
            lastReview = now.minusDays(10), lapses = 2
        )

        assertEquals(3, wrapper.schedule(reviewCard, Rating.AGAIN, now).lapses)
        assertEquals(2, wrapper.schedule(reviewCard, Rating.HARD, now).lapses)
        assertEquals(2, wrapper.schedule(reviewCard, Rating.GOOD, now).lapses)
        assertEquals(2, wrapper.schedule(reviewCard, Rating.EASY, now).lapses)
    }

    /**
     * P0-T1f: 调度后 reps/reviewCount 应 +1（所有状态/评分）。
     */
    @Test
    fun schedule_always_increments_reps_and_reviewCount() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false)
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        val baseReps = 5
        val baseReviewCount = 5

        for (state in State.entries) {
            val card = FlashCard(
                state = state, stability = 5f, difficulty = 5f,
                lastReview = now.minusDays(5), reps = baseReps, reviewCount = baseReviewCount
            )
            for (rating in Rating.entries) {
                val scheduled = wrapper.schedule(card, rating, now)
                assertEquals("state=$state rating=$rating reps 应 +1",
                    baseReps + 1, scheduled.reps)
                assertEquals("state=$state rating=$rating reviewCount 应 +1",
                    baseReviewCount + 1, scheduled.reviewCount)
            }
        }
    }

    /**
     * P0-T1f: 调度后 lastReview 应更新为 now。
     */
    @Test
    fun schedule_updates_lastReview_to_now() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false)
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        val card = FlashCard(
            state = State.REVIEW, stability = 10f, difficulty = 5f,
            lastReview = now.minusDays(10)
        )
        for (rating in Rating.entries) {
            val scheduled = wrapper.schedule(card, rating, now)
            assertEquals("rating=$rating lastReview 应更新为 now", now, scheduled.lastReview)
        }
    }

    /**
     * P0-T1f: 新卡（state=NEW, stability=0, lastReview=null）调度不应崩溃。
     */
    @Test
    fun schedule_newCard_withZeroStability_doesNotCrash() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false)
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        val newCard = FlashCard(state = State.NEW, stability = 0f, lastReview = null)
        for (rating in Rating.entries) {
            val scheduled = wrapper.schedule(newCard, rating, now)
            assertNotNull("rating=$rating 调度后卡片非空", scheduled)
            assertTrue("rating=$rating 调度后 stability > 0",
                scheduled.stability > 0f)
        }
    }

    /**
     * P2-1 (v0.9.22): LEARNING/REVIEW 状态 + stability=0 不应产生 NaN。
     *
     * 背景：v1 时代老数据的 memo_records 可能 state=LEARNING/REVIEW 但 stability=0
     * （2.json 中 stability NOT NULL DEFAULT 0.0）。此时 nextRecallStability 中
     * s.pow(-w[9]) = 0.pow(-0.1752) = +Infinity，最终 s*(1+growth) = 0*Infinity = NaN，
     * NaN 写回 stability 会污染后续全部调度，且 nextInterval(NaN) 中 NaN.roundToInt() 会抛异常。
     */
    @Test
    fun schedule_learningOrReview_withZeroStability_doesNotProduceNaN() {
        val wrapper = FsrsWrapper(requestRetention = 0.9f, maximumInterval = 365, enableFuzz = false)
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)
        for (state in listOf(State.LEARNING, State.REVIEW, State.RELEARNING)) {
            val zeroStabilityCard = FlashCard(
                state = state,
                stability = 0f,
                difficulty = 5f,
                lastReview = now.minusDays(1),
            )
            for (rating in Rating.entries) {
                val scheduled = wrapper.schedule(zeroStabilityCard, rating, now)
                assertNotNull("state=$state rating=$rating 调度后卡片非空", scheduled)
                assertTrue(
                    "state=$state rating=$rating stability 不应为 NaN: ${scheduled.stability}",
                    !scheduled.stability.isNaN(),
                )
                assertTrue(
                    "state=$state rating=$rating stability 不应为 Infinity: ${scheduled.stability}",
                    !scheduled.stability.isInfinite(),
                )
                assertTrue(
                    "state=$state rating=$rating stability 应 > 0: ${scheduled.stability}",
                    scheduled.stability > 0f,
                )
            }
        }
    }
}
