package com.wenyan.app.core.fsrs

/**
 * 三档复习调度机制 —— 档位FSRS参数配置
 *
 * 对应设计文档第743-781行（3.3.4节 档位预设参数）。
 * 每个档位有完整的目标保留率、最大/最小间隔、稳定性增长系数、Easy加成、Again惩罚等参数。
 *
 * @property tier                   所属档位
 * @property targetRetention        目标保留率 R_target（FSRS-Kotlin库映射为 requestRetention）
 * @property maxInterval            最大间隔（天），超过此值不再增长
 * @property minInterval            最小间隔（天），低于此值强制提升
 * @property stabilityGrowthFactor  稳定性增长系数（影响 Good/Easy 的 S'）
 * @property easyBonus              Easy 额外加成
 * @property againPenalty           Again 惩罚系数
 */
data class TierFsrsConfig(
    val tier: MemoryTier,
    val targetRetention: Float,
    val maxInterval: Int,
    val minInterval: Int,
    val stabilityGrowthFactor: Float,
    val easyBonus: Float,
    val againPenalty: Float
)

/**
 * 三档预设参数表（完全镜像设计文档第753-781行）
 *
 * - TIER_EXACT:     targetRetention=0.95, maxInterval=180,  stabilityGrowthFactor=0.85, easyBonus=1.2, againPenalty=0.3
 * - TIER_FRAMEWORK: targetRetention=0.90, maxInterval=365,  stabilityGrowthFactor=1.0,  easyBonus=1.3, againPenalty=0.4
 * - TIER_UNDERSTAND:targetRetention=0.85, maxInterval=720,  stabilityGrowthFactor=1.15, easyBonus=1.5, againPenalty=0.5
 */
val TIER_CONFIGS: Map<MemoryTier, TierFsrsConfig> = mapOf(
    MemoryTier.TIER_EXACT to TierFsrsConfig(
        tier = MemoryTier.TIER_EXACT,
        targetRetention = 0.95f,
        maxInterval = 180,
        minInterval = 1,
        stabilityGrowthFactor = 0.85f,
        easyBonus = 1.2f,
        againPenalty = 0.3f
    ),
    MemoryTier.TIER_FRAMEWORK to TierFsrsConfig(
        tier = MemoryTier.TIER_FRAMEWORK,
        targetRetention = 0.90f,
        maxInterval = 365,
        minInterval = 1,
        stabilityGrowthFactor = 1.0f,
        easyBonus = 1.3f,
        againPenalty = 0.4f
    ),
    MemoryTier.TIER_UNDERSTAND to TierFsrsConfig(
        tier = MemoryTier.TIER_UNDERSTAND,
        targetRetention = 0.85f,
        maxInterval = 720,
        minInterval = 1,
        stabilityGrowthFactor = 1.15f,
        easyBonus = 1.5f,
        againPenalty = 0.5f
    )
)
