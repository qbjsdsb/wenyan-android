package com.wenyan.app.core.common.util

/**
 * 真题内容清洗工具函数（v0.9.10 新增）。
 *
 * 剥离题目内容中的题号前缀，包括阿拉伯数字前缀和中文数字前缀。
 * 不区分试卷标题，一律剥离。
 *
 * 剥离规则：
 * - 阿拉伯数字前缀 "1. " "2. " "10. " "6.20世纪…" 等
 * - 中文数字前缀 "一、" "二、" "一、谈鲁迅…" "三、论述题" 等
 *
 * 不修改种子数据（seed_data.json），仅运行时清洗。
 */
object ExamContentCleaner {

    /** 阿拉伯数字题号前缀： "1. " "10." "6.20世纪…" 等 */
    private val ARABIC_PREFIX_PATTERN = Regex("^\\d+\\.\\s*")

    /** 中文数字题号前缀： "一、" "二、" "三、论述题" 等 */
    private val CN_PREFIX_PATTERN = Regex("^[一二三四五六七八九十]+[、]")

    /**
     * 剥离题目内容中的题号前缀。
     *
     * 依次剥离阿拉伯数字前缀和中文数字前缀。
     *
     * @param content 原始题目内容
     * @return 清洗后的题目内容
     */
    fun stripQuestionNumber(content: String): String {
        // 依次剥离阿拉伯数字前缀 "N. " 和中文数字前缀 "N、"
        return content.trimStart()
            .replaceFirst(ARABIC_PREFIX_PATTERN, "")
            .replaceFirst(CN_PREFIX_PATTERN, "")
    }
}