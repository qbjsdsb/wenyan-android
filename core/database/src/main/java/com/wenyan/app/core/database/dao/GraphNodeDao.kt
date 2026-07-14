package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.GraphNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * 图谱节点表 DAO。
 */
@Dao
interface GraphNodeDao {

    // P1 修正:原用 @Insert(REPLACE),DELETE+INSERT 会触发子表 CASCADE
    // (graph_edges 双向级联删除,图谱连接丢失)。改用 @Upsert。
    @Upsert
    suspend fun insert(entity: GraphNodeEntity)

    @Upsert
    suspend fun insertAll(entities: List<GraphNodeEntity>)

    @Update
    suspend fun update(entity: GraphNodeEntity)

    @Query("DELETE FROM graph_nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM graph_nodes WHERE id = :id")
    suspend fun getById(id: String): GraphNodeEntity?

    /**
     * 批量查询节点（P1-D1/D2 修正 N+1 问题）。
     *
     * 原实现 [GraphRepositoryImpl.getPrerequisites] / [getRelatedNodes] / [getAdjacentNodes]
     * 对每个相邻节点 ID 串行调 getById，导致 N 次数据库往返。改用 `WHERE id IN (:ids)` 一次查询。
     *
     * @param ids 节点 ID 列表（空列表时返回空列表，Room 不会发出 SQL）
     * @return 匹配的节点列表（顺序不保证，调用方需自行按需排序）
     */
    @Query("SELECT * FROM graph_nodes WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<GraphNodeEntity>

    @Query("SELECT * FROM graph_nodes WHERE type = :type ORDER BY id ASC")
    fun observeByType(type: String): Flow<List<GraphNodeEntity>>

    @Query("SELECT * FROM graph_nodes WHERE subject_id = :subjectId ORDER BY id ASC")
    fun observeBySubject(subjectId: String): Flow<List<GraphNodeEntity>>

    @Query("SELECT * FROM graph_nodes WHERE related_point_id = :pointId ORDER BY id ASC")
    fun observeByKnowledgePoint(pointId: String): Flow<List<GraphNodeEntity>>

    @Query("SELECT * FROM graph_nodes ORDER BY id ASC")
    fun observeAll(): Flow<List<GraphNodeEntity>>
}
