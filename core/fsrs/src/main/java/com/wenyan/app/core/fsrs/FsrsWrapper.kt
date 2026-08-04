package com.wenyan.app.core.fsrs

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * FSRS-Kotlin库封装层
 *
 * 对应设计文档第5.4.3节 FSRS-6完整实现（第3460-3744行）和spec.md第265-268行。
 *
 * FSRS-Kotlin库（https://github.com/open-spaced-repetition/FSRS-Kotlin）内部硬编码 enableFuzz=true，
 * 无法通过构造函数外部控制。本封装层fork/包装库的FSRS算法，实现：
 * 1. enableFuzz参数可控（精确记忆档TIER_EXACT要求enableFuzz=false，精确到天）
 * 2. 三档参数预设（stabilityGrowthFactor/easyBonus/againPenalty）可注入
 * 3. requestRetention（对应spec中的desired_retention）可动态调整
 *
 * FSRS-6核心公式（设计文档第4628-4640行）：
 * - 可提取性：R = (1 + t/(9*S))^(-1)
 * - 难度更新含均值回归：D' = D - w[6]*(rating-3); D_next = w[7]*D' + (1-w[7])*w[4]
 * - 遗忘稳定性更新：S' = w[11] * D^(-w[12]) * ((S+1)^w[13] - 1) * exp(-w[14]*(1-R))
 * - 间隔计算：I = 9*S*(1/R_target - 1)
 *
 * @param requestRetention    目标保留率 R_target（对应spec的desired_retention，库的requestRetention）
 * @param maximumInterval     最大间隔（天）
 * @param enableFuzz          是否启用模糊因子（false=精确间隔，TIER_EXACT要求false）
 * @param stabilityGrowthFactor 稳定性增长系数（三档参数，默认1.0=标准FSRS增长）
 * @param easyBonus           Easy额外加成（三档参数，默认1.0）
 * @param againPenalty        Again惩罚系数（三档参数，默认1.0=不额外惩罚）
 * @param random              [applyFuzz] 使用的随机源。
 *                            NF-T8 修正：原用全局 `Random.nextFloat()` 不可注入，
 *                            单元测试只能验证 fuzz 输出范围而非精确值。
 *                            现作为构造参数默认 [Random.Default]（生产环境行为不变），
 *                            测试可注入固定种子 Random 验证精确 fuzz 输出。
 */
class FsrsWrapper(
    private val requestRetention: Float,
    private val maximumInterval: Int,
    private val enableFuzz: Boolean = true,
    private val stabilityGrowthFactor: Float = 1.0f,
    private val easyBonus: Float = 1.0f,
    private val againPenalty: Float = 1.0f,
    private val random: Random = Random.Default,
) {
    companion object {
        private const val DAY_MS = 86_400_000L

        /** FSRS-6学习阶段间隔（Again=1分钟，Hard=5分钟，其他=1天） */
        private const val LEARNING_STEP_AGAIN_DAYS = 1f / 1440f   // 1分钟换算为天
        private const val LEARNING_STEP_HARD_DAYS = 5f / 1440f     // 5分钟换算为天
        // v0.9.31 优化：新卡首次 GOOD 进入 10 分钟学习步（Anki 学习循环最佳实践，
        // 此前新卡 GOOD 直接毕业到长间隔，用户只见过一次容易遗忘）
        private const val LEARNING_STEP_GOOD_DAYS = 10f / 1440f   // 10分钟换算为天

        /**
         * FSRS-6默认参数（21个，w[0]-w[20]）
         * 对应设计文档第3501-3509行 DEFAULT_WEIGHTS_FSRS_6
         */
        val DEFAULT_WEIGHTS = floatArrayOf(
            0.2172f, 0.3174f, 1.7265f, 5.1816f,  // w[0-3] 新卡初始稳定性S0
            4.7284f, 1.0526f, 0.5699f, 0.2197f,  // w[4-7] 初始难度D0 + initD评分影响 + nextD难度变化 + nextD均值回归
            1.5336f, 0.1752f, 0.9441f, 2.4926f,  // w[8-11] 回忆稳定性更新 + 遗忘稳定性更新
            0.0606f, 0.4656f, 1.1842f, 0.5316f,  // w[12-15] 遗忘参数 + Hard惩罚因子
            0.2316f,                               // w[16] Easy奖励因子
            0.0f, 0.0f,                            // w[17-18] FSRS-5短期记忆稳定性
            0.0f, 0.0f                             // w[19-20] FSRS-6学习/重学阶段短期参数
        )
    }

    /** FSRS-6参数权重（21个） */
    private val w: FloatArray = DEFAULT_WEIGHTS

    // ===================== 公开API =====================

    /**
     * 按指定评分调度卡片，返回更新后的卡片
     *
     * @param card   待调度的卡片
     * @param rating 评分（AGAIN/HARD/GOOD/EASY）
     * @param now    当前时间（默认LocalDateTime.now()）
     * @return 调度后的新卡片（dueDate/stability/difficulty等已更新）
     */
    fun schedule(card: FlashCard, rating: Rating, now: LocalDateTime = LocalDateTime.now()): FlashCard {
        val schedulingCard = scheduleInternal(card, rating, now)
        return schedulingCard.card
    }

    /**
     * 计算卡片当前的可提取性R（0-1）
     *
     * 对应设计文档第3631行 retrievability公式：R = (1 + t/(9*S))^(-1)
     * 新卡（stability=0或lastReview=null）返回0。
     *
     * @param card 待计算的卡片
     * @param now  当前时间
     * @return 可提取性R（0-1之间）
     */
    fun getRetrievability(card: FlashCard, now: LocalDateTime = LocalDateTime.now()): Float {
        val lastReview = card.lastReview ?: return 0f
        if (card.stability <= 0f) return 0f
        val elapsedDays = ChronoUnit.DAYS.between(lastReview, now).toFloat().coerceAtLeast(0f)
        return retrievability(elapsedDays, card.stability)
    }

    /**
     * 一次性返回4种评分的调度结果（用于UI预览按钮显示间隔）
     *
     * @param card 待调度的卡片
     * @param now  当前时间
     * @return Map<Rating, FlashCard>，每种评分对应的调度结果
     */
    fun repeat(card: FlashCard, now: LocalDateTime = LocalDateTime.now()): Map<Rating, FlashCard> {
        return Rating.entries.associateWith { schedule(card, it, now) }
    }

    /**
     * 解决卡片级档位保持率与全局保持率的冲突
     *
     * 对应spec.md第301-305行（内容类型与全局保持率冲突）：
     * - 卡片级预设优先于全局保持率
     * - 作品背诵卡片(0.95)在基础阶段(全局0.85) → 取较高值0.95，不降级
     * - 全局保持率仅作为未指定类型卡片的默认值
     *
     * @param cardTier        卡片所属档位
     * @param globalRetention 全局保持率（由考研倒计时阶段决定）
     * @return 最终使用的保持率（取较高值）
     */
    fun resolveRetention(cardTier: MemoryTier, globalRetention: Float): Float {
        val cardRetention = TIER_CONFIGS[cardTier]?.targetRetention ?: 0.90f
        // 取较高值，卡片级档位优先，不降级
        return maxOf(cardRetention, globalRetention)
    }

    // ===================== 内部调度逻辑 =====================

    /**
     * 内部调度方法，返回完整的SchedulingCard（卡片+日志）
     * 对应设计文档第3559-3600行 schedule方法
     */
    private fun scheduleInternal(card: FlashCard, rating: Rating, now: LocalDateTime): SchedulingCard {
        val elapsedDays = if (card.lastReview != null) {
            ChronoUnit.DAYS.between(card.lastReview, now).toInt().coerceAtLeast(0)
        } else 0

        val s = card.stability
        val d = card.difficulty
        // 新卡不计算R（stability=0会除零）
        val r = if (card.lastReview != null && card.stability > 0f) {
            retrievability(elapsedDays.toFloat(), card.stability)
        } else 0f

        val (newS, newD, newState, interval, lapses) = when (card.state) {
            State.NEW -> scheduleNew(card, rating, r)
            State.LEARNING -> scheduleLearning(card, rating, r)
            State.REVIEW -> scheduleReview(card, rating, r)
            State.RELEARNING -> scheduleRelearning(card, rating, r)
        }

        // 应用模糊因子（仅对≥2.5天的间隔应用，学习阶段短间隔不模糊）
        val fuzzedInterval = if (enableFuzz && interval >= 2.5f) {
            applyFuzz(interval)
        } else {
            interval
        }

        // 计算到期时间：
        // - 学习阶段（interval < 1天）：分钟级精度（Again=1分钟, Hard=5分钟）
        // - 复习阶段（interval ≥ 1天）：天级精度
        // scheduledDays=0 表示学习阶段同一天内的分钟级调度，dueDate 仍精确到分钟
        val isLearningStep = fuzzedInterval < 1f
        val dueDate: LocalDateTime
        val scheduledDaysValue: Int

        if (isLearningStep) {
            val minutes = (fuzzedInterval * 1440f).toLong().coerceAtLeast(1L)
            dueDate = now.plusMinutes(minutes)
            scheduledDaysValue = 0
        } else {
            // P1-11 修复：fuzz 后用 roundToInt 而非 toInt，保证扰动对称。
            // 原实现 toInt() 向零截断：interval=5.0, fuzz∈[-1,+1) → fuzzedInterval∈[4,6)
            // toInt 得 P(4)=0.5, P(5)=0.5, P(6)=0，期望=4.5（偏少 0.5 天）。
            // roundToInt: P(4)=0.25, P(5)=0.5, P(6)=0.25，期望=5.0（对称）。
            // 与同文件 nextInterval（第 387-391 行，F-05 已修复）取整策略对齐。
            //
            // v0.8.7 修复：fuzz 后 clamp 到 maximumInterval。
            // 原实现仅 coerceAtLeast(1)，长期复习卡 fuzz 后可能超过 maximumInterval
            // （如 maxInterval=365, interval=364, fuzz=+2 → 366 > 365）。
            // FSRS 规范要求所有调度间隔不超过 maximumInterval，加 coerceAtMost 保证。
            scheduledDaysValue = fuzzedInterval.roundToInt()
                .coerceIn(1, maximumInterval)
            dueDate = now.plusDays(scheduledDaysValue.toLong())
        }

        val updatedCard = FlashCard(
            dueDate = dueDate,
            stability = newS,
            difficulty = newD.coerceIn(1f, 10f),
            interval = scheduledDaysValue,
            reviewCount = card.reviewCount + 1,
            lastReview = now,
            state = newState,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDaysValue,
            reps = card.reps + 1,
            lapses = lapses
        )

        val reviewLog = ReviewLog(
            rating = rating,
            state = card.state,
            dueDate = card.dueDate,
            stability = s,
            difficulty = d,
            elapsedDays = elapsedDays,
            lastElapsedDays = card.scheduledDays,
            scheduledDays = scheduledDaysValue,
            reviewTime = now
        )

        return SchedulingCard(updatedCard, reviewLog)
    }

    // ---- 状态调度：状态转换规则见设计文档5.4.4 ----

    private fun scheduleNew(c: FlashCard, r: Rating, rR: Float): ScheduleResult {
        val newS = initStability(r)
        val newD = initDifficulty(r)
        return when (r) {
            Rating.AGAIN -> ScheduleResult(newS, newD, State.LEARNING, learningInterval(Rating.AGAIN), c.lapses)
            Rating.HARD -> ScheduleResult(newS, newD, State.LEARNING, learningInterval(Rating.HARD), c.lapses)
            // v0.9.31 优化：新卡首次 GOOD 先进 10 分钟学习步（再 GOOD 才毕业到 REVIEW）
            Rating.GOOD -> ScheduleResult(newS, newD, State.LEARNING, LEARNING_STEP_GOOD_DAYS, c.lapses)
            // EASY 直接毕业（Anki easy interval 语义：一次轻松答对视为已掌握）
            Rating.EASY -> ScheduleResult(newS, newD, State.REVIEW, nextInterval(newS).toFloat(), c.lapses)
        }
    }

    private fun scheduleLearning(c: FlashCard, r: Rating, rR: Float): ScheduleResult {
        val newD = nextDifficulty(c.difficulty, r)
        return when (r) {
            // 学习阶段答Again：保持LEARNING状态（尚未"记住"，不构成"遗忘"，
            // 不增加lapses，不进入RELEARNING）
            Rating.AGAIN -> ScheduleResult(
                nextForgetStability(c.difficulty, c.stability, rR) * againPenalty,
                newD, State.LEARNING, learningInterval(Rating.AGAIN), c.lapses
            )
            Rating.HARD -> ScheduleResult(
                nextRecallStability(c.difficulty, c.stability, rR, r),
                newD, State.LEARNING, learningInterval(Rating.HARD), c.lapses
            )
            Rating.GOOD -> {
                val recallS = nextRecallStability(c.difficulty, c.stability, rR, r)
                ScheduleResult(recallS, newD, State.REVIEW, nextInterval(recallS).toFloat(), c.lapses)
            }
            Rating.EASY -> {
                val recallS = nextRecallStability(c.difficulty, c.stability, rR, r) * easyBonus
                ScheduleResult(recallS, newD, State.REVIEW, nextInterval(recallS).toFloat(), c.lapses)
            }
        }
    }

    private fun scheduleReview(c: FlashCard, r: Rating, rR: Float): ScheduleResult {
        val newD = nextDifficulty(c.difficulty, r)
        return when (r) {
            Rating.AGAIN -> ScheduleResult(
                nextForgetStability(c.difficulty, c.stability, rR) * againPenalty,
                newD, State.RELEARNING, learningInterval(Rating.AGAIN), c.lapses + 1
            )
            Rating.HARD -> {
                val recallS = nextRecallStability(c.difficulty, c.stability, rR, r)
                ScheduleResult(recallS, newD, State.REVIEW, nextInterval(recallS).toFloat(), c.lapses)
            }
            Rating.GOOD -> {
                val recallS = nextRecallStability(c.difficulty, c.stability, rR, r)
                ScheduleResult(recallS, newD, State.REVIEW, nextInterval(recallS).toFloat(), c.lapses)
            }
            Rating.EASY -> {
                val recallS = nextRecallStability(c.difficulty, c.stability, rR, r) * easyBonus
                ScheduleResult(recallS, newD, State.REVIEW, nextInterval(recallS).toFloat(), c.lapses)
            }
        }
    }

    private fun scheduleRelearning(c: FlashCard, r: Rating, rR: Float): ScheduleResult {
        val newD = nextDifficulty(c.difficulty, r)
        return when (r) {
            Rating.AGAIN -> ScheduleResult(
                nextForgetStability(c.difficulty, c.stability, rR) * againPenalty,
                newD, State.RELEARNING, learningInterval(Rating.AGAIN), c.lapses
            )
            Rating.HARD -> ScheduleResult(
                nextForgetStability(c.difficulty, c.stability, rR),
                newD, State.RELEARNING, learningInterval(Rating.HARD), c.lapses
            )
            Rating.GOOD -> {
                val recallS = nextRecallStability(c.difficulty, c.stability, rR, r)
                ScheduleResult(recallS, newD, State.REVIEW, nextInterval(recallS).toFloat(), c.lapses)
            }
            Rating.EASY -> {
                val recallS = nextRecallStability(c.difficulty, c.stability, rR, r) * easyBonus
                ScheduleResult(recallS, newD, State.REVIEW, nextInterval(recallS).toFloat(), c.lapses)
            }
        }
    }

    // ===================== 核心数学公式（对应设计文档5.4.2） =====================

    /**
     * 可提取性公式：R = (1 + t/(9*S))^(-1)
     * 对应设计文档第3631行
     */
    fun retrievability(elapsedDays: Float, stability: Float): Float {
        if (stability <= 0f) return 0f
        return (1f + elapsedDays / (9f * stability)).pow(-1f)
    }

    /**
     * 新卡初始稳定性 S0 = w[rating.index]
     * 对应设计文档第3637行
     *
     * NF-T7 修正：原 `w[rating.value - 1]` 把枚举业务值与数组下标耦合，
     * 改用 `rating.index` 显式表达下标语义，避免未来枚举顺序调整时引发越界或权重错位。
     */
    fun initStability(rating: Rating): Float {
        return w[rating.index]
    }

    /**
     * 新卡初始难度 D0 = w[4] - (rating-3)*w[5]
     * 对应设计文档第3640行
     */
    fun initDifficulty(rating: Rating): Float {
        return (w[4] - (rating.value - 3) * w[5]).coerceIn(1f, 10f)
    }

    /**
     * 难度更新（含均值回归）
     * D' = D - w[6]*(rating-3)
     * D_next = w[7]*D' + (1-w[7])*w[4]
     * 对应设计文档第3645行
     */
    fun nextDifficulty(d: Float, rating: Rating): Float {
        val dNext = d - w[6] * (rating.value - 3)
        val meanReverted = w[7] * dNext + (1f - w[7]) * w[4]
        return meanReverted.coerceIn(1f, 10f)
    }

    /**
     * 稳定性更新——回忆成功
     * 对应设计文档第3652行
     *
     * 公式：growth = w[8] * (11-D) * S^(-w[9]) * (exp((1-R)*w[10]) - 1) * hardPenalty * easyBonus
     * 最终 S' = S * (1 + growth * stabilityGrowthFactor)
     *
     * NF-F1 修正（P0-F1 / 1.E 决策 A 简化版）：原实现用 `exp(w[8])` 放大增长系数,
     * 与 FSRS-6 标准公式不符。FSRS-6 默认权重 w[8]=1.5336 已是直接乘子,
     * 套用 exp() 后变为 exp(1.5336)≈4.635,放大 3.02 倍 → 间隔膨胀 3 倍 →
     * 用户复习频率比 FSRS-6 标准低 3 倍 → 实际保留率低于 R_target。
     *
     * 修正后 growth 降低 3 倍,stability 增长放缓,间隔缩短,复习频率提升,
     * 保留率向 R_target 收敛。对已发版用户:存量 stability 不变,后续评分增长放缓,
     * 间隔逐步从 ~3× 收敛到 ~1× 标准值。
     *
     * 不改 retrievability / nextInterval / nextForgetStability:
     * - retrievability decay=-1 vs FSRS-6 decay=-0.5:在 R_target∈{0.85,0.90,0.95}
     *   下 nextInterval 差异 <3%,可忽略
     * - nextForgetStability 槽位 w[11-14] vs FSRS-6 w[15-18]:当前 weights w[17-18]=0,
     *   改槽位会让公式近乎失效,需同步更新 weights 数组,风险高,不在本次修复范围
     */
    fun nextRecallStability(d: Float, s: Float, r: Float, rating: Rating): Float {
        // P2-1 (v0.9.22): stability <= 0 防御。
        // 背景：v1 时代老数据可能 state=LEARNING/REVIEW 但 stability=0（2.json 中
        // stability NOT NULL DEFAULT 0.0）。此时 s.pow(-w[9]) = 0.pow(-0.1752) = +Infinity，
        // 最终 s*(1+growth) = 0*Infinity = NaN；NaN 写回 stability 污染后续全部调度，
        // 且 nextInterval(NaN) 中 NaN.roundToInt() 会抛异常。
        // 与 nextForgetStability 的 maxOf(0.1f, ...) 防御对齐：退化到初始稳定性。
        if (s <= 0f) return initStability(rating)
        val hardPenalty = if (rating == Rating.HARD) w[15] else 1f
        // F-02 修正：w[16]=0.2316 < 1，直接用作乘子会让 EASY stability < GOOD stability（语义反转）。
        // 官方 FSRS-6 公式：easyBonus = 1 + w[16]，确保 EASY 增长 > GOOD 增长。
        val easyBonusVal = if (rating == Rating.EASY) 1f + w[16] else 1f
        // NF-F1 修正：exp(w[8]) → w[8]（FSRS-6 标准直接用 w[8] 作乘子,无需 exp 放大）
        val growth = (w[8] * (11.0 - d) * s.pow(-w[9]) *
            (exp((1f - r) * w[10].toDouble()) - 1.0) * hardPenalty * easyBonusVal).toFloat()
        return s * (1f + growth * stabilityGrowthFactor)
    }

    /**
     * 稳定性更新——遗忘
     * S' = w[11] * D^(-w[12]) * ((S+1)^w[13] - 1) * exp(-w[14]*(1-R))
     * 对应设计文档第3661行
     */
    fun nextForgetStability(d: Float, s: Float, r: Float): Float {
        val newS = (w[11] * d.pow(-w[12]) *
            ((s + 1f).pow(w[13]) - 1f) * exp(-w[14] * (1f - r).toDouble())).toFloat()
        return maxOf(0.1f, newS)
    }

    /**
     * 间隔计算：I = 9*S*(1/R_target - 1)
     * 对应设计文档第3668行
     * 结果限制在 [1, maximumInterval] 范围内。
     *
     * F-05 修正：用 roundToInt() 替代 toInt()。官方 FSRS-6 使用 round()，
     * 原实现 toInt() 截断会让 stability=5.7 得 5 天而官方得 6 天。
     */
    fun nextInterval(stability: Float): Int {
        if (stability <= 0f) return 1
        val interval = 9f * stability * (1f / requestRetention - 1f)
        return minOf(maxOf(interval.roundToInt(), 1), maximumInterval)
    }

    /**
     * 模糊因子应用（enableFuzz=true时调用）
     * 对应设计文档第3674行
     * interval < 2.5: 不模糊
     * interval < 15: ±1天
     * else: ±5%
     *
     * NF-T8 修正：原用全局 `Random.nextFloat()` 不可注入，测试不可重复。
     * 改用构造参数 [random]（默认 [Random.Default]，生产环境行为不变），
     * 测试可注入固定种子 Random 验证精确 fuzz 输出。
     */
    private fun applyFuzz(interval: Float): Float {
        val fuzzRange = when {
            interval < 2.5f -> 0f
            interval < 15f -> 1f
            else -> interval * 0.05f
        }
        val fuzz = random.nextFloat() * 2f * fuzzRange - fuzzRange
        return maxOf(1f, interval + fuzz)
    }

    /**
     * 学习阶段间隔（Again=1分钟, Hard=5分钟, Good/Easy=1天）
     * 对应设计文档第3684行
     */
    private fun learningInterval(rating: Rating): Float = when (rating) {
        Rating.AGAIN -> LEARNING_STEP_AGAIN_DAYS
        Rating.HARD -> LEARNING_STEP_HARD_DAYS
        else -> 1f
    }

    /** 内部调度结果数据类 */
    private data class ScheduleResult(
        val newStability: Float,
        val newDifficulty: Float,
        val newState: State,
        val interval: Float,
        val lapses: Int
    )
}
