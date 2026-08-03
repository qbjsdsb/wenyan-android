package com.wenyan.app.core.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * 卡片备考设置（v0.9.29）。
 *
 * 数据模型：每日新卡限额 + 考频筛选 + 科目筛选 + 考试日期。
 * 用于"卡片备考系统"——把 6000+ 张卡拆解为每日可控的学习量。
 */
data class CardSettings(
    /** 每日新卡数（用户可设，默认 60） */
    val dailyNewLimit: Int = 60,
    /** 考频筛选（默认 高频+中频） */
    val frequencyFilter: CardFrequencyFilter = CardFrequencyFilter.HIGH_MEDIUM,
    /** 科目多选（默认全选四科） */
    val subjectFilters: Set<String> = DEFAULT_SUBJECTS,
    /** 考试日期（毫秒时间戳；null = 未设置，UI 层按当年 12/24 计算） */
    val examDateMillis: Long? = null,
) {
    companion object {
        val DEFAULT_SUBJECTS: Set<String> = setOf(
            "中国古代文学",
            "中国现当代文学",
            "外国文学",
            "文学理论",
        )
    }
}

/** 考频筛选档位（v0.9.29）。 */
enum class CardFrequencyFilter(val displayName: String) {
    /** 仅高频（真题常考） */
    HIGH("仅高频"),
    /** 高频 + 中频（推荐，聚焦考试重点） */
    HIGH_MEDIUM("高频 + 中频"),
    /** 全部 */
    ALL("全部"),
}

/**
 * 卡片备考设置仓库（v0.9.29）。
 *
 * 底层使用 DataStore Preferences（与 [com.wenyan.app.core.designsystem.theme.ThemeRepositoryImpl]
 * 同一模式），提供 [cardSettings] Flow 与各字段 setter。
 */
interface CardSettingsRepository {
    /** 当前卡片备考设置（含默认值；IO 异常降级默认） */
    val cardSettings: Flow<CardSettings>

    suspend fun setDailyNewLimit(limit: Int)

    suspend fun setFrequencyFilter(filter: CardFrequencyFilter)

    suspend fun setSubjectFilters(subjects: Set<String>)

    suspend fun setExamDate(millis: Long?)
}
