package com.wenyan.app.core.fsrs

/**
 * 内容类型枚举 —— spec中定义的4种文学考研内容类型
 *
 * 对应spec.md第30行（FSRS参数预设：不同内容类型使用不同 desired_retention）。
 * 这4种内容类型映射到设计文档三档机制，非另立体系。
 *
 * @property TERM_EXPLANATION 名词解释/术语定义
 * @property WORK_RECITATION  作品背诵/原文默写
 * @property ESSAY             论述题/分析题
 * @property SCHOOL_FEATURE    流派特征
 * @property USER_CUSTOM       用户自定义内容（默认使用名词解释预设0.90）
 */
enum class ContentType {
    TERM_EXPLANATION,
    WORK_RECITATION,
    ESSAY,
    SCHOOL_FEATURE,
    USER_CUSTOM
}

/**
 * 内容类型 → 三档机制映射器
 *
 * 对应spec.md第261行（4种内容类型预设映射到三档机制，非另立体系）：
 * - 作品背诵/原文默写 → TIER_EXACT   (0.95)  考场需逐字复述
 * - 名词解释/流派特征 → TIER_FRAMEWORK (0.90)  考场需分条复述要点
 * - 论述题/分析题     → TIER_UNDERSTAND(0.85)  考场需能用自己的话阐述
 * - 用户自定义        → TIER_FRAMEWORK (0.90)  默认名词解释预设，用户可手动调整
 */
object ContentTierMapper {

    /**
     * 将内容类型映射到三档机制中的档位
     *
     * @param contentType 内容类型
     * @return 对应的MemoryTier档位
     */
    fun mapContentTypeToTier(contentType: ContentType): MemoryTier {
        return when (contentType) {
            ContentType.WORK_RECITATION -> MemoryTier.TIER_EXACT
            ContentType.TERM_EXPLANATION -> MemoryTier.TIER_FRAMEWORK
            ContentType.SCHOOL_FEATURE -> MemoryTier.TIER_FRAMEWORK
            ContentType.ESSAY -> MemoryTier.TIER_UNDERSTAND
            ContentType.USER_CUSTOM -> MemoryTier.TIER_FRAMEWORK
        }
    }

    /**
     * 根据内容类型获取对应的FSRS档位配置
     *
     * @param contentType 内容类型
     * @return 对应的TierFsrsConfig
     */
    fun getConfig(contentType: ContentType): TierFsrsConfig {
        return TIER_CONFIGS[mapContentTypeToTier(contentType)]!!
    }

    /**
     * 判断内容类型是否需要精确间隔（enableFuzz=false）
     * spec要求：作品背诵类（TIER_EXACT）使用enableFuzz=false（精确到天，不模糊间隔）
     *
     * @param contentType 内容类型
     * @return true表示需要模糊间隔，false表示需要精确间隔
     */
    fun shouldEnableFuzz(contentType: ContentType): Boolean {
        return mapContentTypeToTier(contentType) != MemoryTier.TIER_EXACT
    }
}
