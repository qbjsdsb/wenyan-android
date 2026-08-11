package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * 章节表 DAO。
 */
@Dao
interface ChapterDao {

    // P0 修正:原用 @Insert(REPLACE),DELETE+INSERT 会触发子表 CASCADE
    // (knowledge_points → memo_records/review_logs/data_sources),
    // 静默清空用户 FSRS 调度数据。改用 @Upsert。
    @Upsert
    suspend fun insert(entity: ChapterEntity)

    @Upsert
    suspend fun insertAll(entities: List<ChapterEntity>)

    @Update
    suspend fun update(entity: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE subject_id = :subjectId ORDER BY sort_order ASC, id ASC")
    fun observeBySubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE parent_id = :parentId ORDER BY sort_order ASC, id ASC")
    fun observeChildren(parentId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subject_id = :subjectId AND parent_id IS NULL ORDER BY sort_order ASC, id ASC")
    fun observeRoots(subjectId: String): Flow<List<ChapterEntity>>

    /**
     * 递归 CTE 查询：返回以 [rootId] 为根的整棵子树（含根），按 sort_order 排序。
     *
     * 用于章节树视图一次性拉取整棵子树，避免 N+1 查询。
     * SQLite 自 3.8.3 起支持 WITH RECURSIVE，Android API 21+（minSdk 26 满足）。
     */
    @Query(
        """
        WITH RECURSIVE tree AS (
            SELECT * FROM chapters WHERE id = :rootId
            UNION ALL
            SELECT c.* FROM chapters c JOIN tree t ON c.parent_id = t.id
        )
        SELECT * FROM tree ORDER BY sort_order ASC, id ASC
        """,
    )
    fun observeTree(rootId: String): Flow<List<ChapterEntity>>

    /**
     * 统计有父章节的子章节数量（用于 seed 导入后自检章节树已生成）。
     */
    @Query("SELECT COUNT(*) FROM chapters WHERE parent_id IS NOT NULL")
    suspend fun countNonRootChapters(): Int
}
