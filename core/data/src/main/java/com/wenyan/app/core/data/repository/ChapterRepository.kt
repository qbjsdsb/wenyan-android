package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * 章节仓库接口（ADR-001 B1.2）。
 *
 * 提供章节树读取能力，复用既有 [ChapterEntity] schema（parentId 字段支持多级树）。
 * 用于知识点列表的层级视图（章节树）替代已移除的图谱全局视图。
 *
 * 设计说明（与 [WrongAnswerRepository] / [ExamRepository] 一致）：
 * - 读 API 直接返回 Entity，不引入 domain model 增加无谓映射
 * - 实现为 [ChapterRepositoryImpl]，通过 Hilt @Binds 绑定
 */
interface ChapterRepository {

    /** 观察所有科目（按 sortOrder ASC） */
    fun observeSubjects(): Flow<List<SubjectEntity>>

    /** 观察指定科目的根章节（parentId IS NULL，按 sortOrder ASC） */
    fun observeRootChapters(subjectId: String): Flow<List<ChapterEntity>>

    /** 观察指定父章节的直接子章节（按 sortOrder ASC） */
    fun observeChildren(parentId: String): Flow<List<ChapterEntity>>

    /**
     * 观察以 [rootId] 为根的整棵子树（递归 CTE，含根）。
     *
     * 一次性拉取整棵子树避免 N+1 查询，用于章节树视图渲染。
     */
    fun observeTree(rootId: String): Flow<List<ChapterEntity>>

    /** 观察指定章节下的知识点（按 created_at ASC） */
    fun observeKnowledgePointsByChapter(chapterId: String): Flow<List<KnowledgePointEntity>>

    /**
     * 统计有父章节的子章节数量。
     *
     * 用于 seed 导入后自检章节树已生成（FF4 健身函数）。
     */
    suspend fun countNonRootChapters(): Int
}
