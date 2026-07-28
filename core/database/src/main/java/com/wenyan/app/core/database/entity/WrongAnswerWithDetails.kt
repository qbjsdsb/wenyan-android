package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * 错题本 JOIN 查询结果 POJO（v0.9.2 新增）。
 *
 * 在 [WrongAnswerEntity] 基础上补充**题目文本**，供 UI 渲染：
 * - 卡片来源（point_id 非空）：LEFT JOIN knowledge_points 取 `title`
 * - 真题来源（exam_question_id 非空）：LEFT JOIN exam_questions 取 `content`
 * - 两者都为空（理论不应发生）：questionTitle = null，UI 兜底显示"未知题目"
 *
 * 使用 COALESCE 优先取知识点 title（卡片来源），真题来源取 exam_questions.content。
 * 不冗余存储题目文本，题目变更自动同步（JOIN 实时查询）。
 *
 * @property wrongAnswer 错题本体（@Embedded 展开所有列）
 * @property questionTitle 关联的题目文本（知识点 title 或真题 content）
 */
data class WrongAnswerWithDetails(
    @Embedded val wrongAnswer: WrongAnswerEntity,
    @ColumnInfo(name = "question_title") val questionTitle: String?,
)
