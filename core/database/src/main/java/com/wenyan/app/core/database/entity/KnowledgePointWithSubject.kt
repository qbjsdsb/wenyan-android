package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * 知识点 + 科目名 关联数据类（P1 修复）。
 *
 * 由 [com.wenyan.app.core.database.dao.KnowledgePointDao.observeVerifiedWithSubject]
 * 的 JOIN 查询返回，避免 N+1 查询。
 *
 * 关联路径：knowledge_points.chapter_id → chapters.subject_id → subjects.name
 *
 * 注意：subjectName 需显式 @ColumnInfo(name = "subject_name") 映射，
 * Room 对 JOIN 查询的 POJO 不自动转换 snake_case → camelCase。
 *
 * P1-AUDIT-5 修正：subjectName 改为 String?（nullable）。
 * 原 INNER JOIN + non-null 设计会静默排除 chapter_id 或 subject_id 无效的知识点，
 * 导致用户在 VERIFIED 列表中无感知地"丢失"知识点。改用 LEFT JOIN 后，
 * 无效关联的科目名为 null，UI 层 fallback 显示"未知科目"，确保数据不丢失。
 *
 * @property point 知识点实体（@Embedded 展开所有字段）
 * @property subjectName 科目全名（如"中国古代文学"），来自 subjects.name；
 *   null 表示知识点无有效科目关联（数据异常，需 UI 层 fallback）
 */
data class KnowledgePointWithSubject(
    @Embedded val point: KnowledgePointEntity,
    @ColumnInfo(name = "subject_name") val subjectName: String?,
)

