package com.wenyan.app.core.database.entity

/**
 * 卡片模板类型枚举（Task 17.1）。
 *
 * 对应 Spec 第34-36行的6种文学专用卡片模板。
 * 与设计文档3.3.2节5种背诵模式正交：
 * - 卡片模板（内容组织维度）：定义卡片正反面放什么内容
 * - 背诵模式（复习方式维度）：定义用户如何复习这张卡
 *
 * 映射关系（spec第三轮确认）：
 * - [TERM_EXPLANATION] → 适配 Read / Cloze 模式
 * - [CLOZE_QUOTE] → 适配 Cloze 模式
 * - [ESSAY_POINTS] → 适配 Outline 模式
 * - [WORK_AUTHOR_BIDIRECTIONAL] → 适配 Recall 模式
 */
enum class CardTemplateType {
    /** 名词解释卡（社团类：时间/地点/人物/刊物/主张/贡献；作品类：作者/年代/内容/特色/影响） */
    TERM_EXPLANATION,

    /** Cloze名句填空卡（填空 + 语法情感提示） */
    CLOZE_QUOTE,

    /** 作品-作者双向卡（自动生成正反两张） */
    WORK_AUTHOR_BIDIRECTIONAL,

    /** 论述要点卡（背面放关键词提示而非完整答案） */
    ESSAY_POINTS,

    /** 流派对照卡（表格化：京派/海派/新月派/象征派） */
    SCHOOL_COMPARISON,

    /** 区分卡（易混淆作家/作品对比，正反面都出） */
    DISTINCTION,
}
