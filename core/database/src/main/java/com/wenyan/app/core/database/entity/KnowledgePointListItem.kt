package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo

/**
 * 知识点列表展示投影（v0.9.37 P1-2）。
 *
 * 列表流只查询展示列（id/title/summary/core_conclusion/exam_frequency/subject_name），
 * 避免 [KnowledgePointEntity] 全字段 `SELECT *` 反复传输 full_content / study_text /
 * multi_perspectives / related_ids 等大文本列——960 行大文本反序列化在内存与 CPU 上
 * 是纯浪费（列表卡片只用 title/summary/考频/科目）。
 *
 * 详情页仍走 [KnowledgePointEntity] 全字段（getById），不受影响。
 *
 * @param subjectName 科目名（LEFT JOIN subjects，可能为 null → UI fallback "未知科目"）
 */
data class KnowledgePointListItem(
    val id: String,
    val title: String,
    val summary: String?,
    /** 兜底展示用：summary 为 null 时截取 coreConclusion 前 100 字 */
    @ColumnInfo(name = "core_conclusion")
    val coreConclusion: String,
    /** 考频原始值（HIGH/MEDIUM/LOW/NEVER），UI 层映射中文标签 */
    @ColumnInfo(name = "exam_frequency")
    val examFrequency: String,
    @ColumnInfo(name = "subject_name")
    val subjectName: String?,
)
