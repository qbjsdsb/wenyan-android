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
 * @property point 知识点实体（@Embedded 展开所有字段）
 * @property subjectName 科目全名（如"中国古代文学"），来自 subjects.name
 */
data class KnowledgePointWithSubject(
    @Embedded val point: KnowledgePointEntity,
    @ColumnInfo(name = "subject_name") val subjectName: String,
)

