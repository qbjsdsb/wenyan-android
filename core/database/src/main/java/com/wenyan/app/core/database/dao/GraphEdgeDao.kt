package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import kotlinx.coroutines.flow.Flow

/**
 * 图谱边表 DAO。
 */
@Dao
interface GraphEdgeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GraphEdgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<GraphEdgeEntity>)

    @Update
    suspend fun update(entity: GraphEdgeEntity)

    @Query("DELETE FROM graph_edges WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM graph_edges WHERE id = :id")
    suspend fun getById(id: String): GraphEdgeEntity?

    @Query("SELECT * FROM graph_edges WHERE source_id = :nodeId ORDER BY id ASC")
    fun observeBySource(nodeId: String): Flow<List<GraphEdgeEntity>>

    @Query("SELECT * FROM graph_edges WHERE target_id = :nodeId ORDER BY id ASC")
    fun observeByTarget(nodeId: String): Flow<List<GraphEdgeEntity>>

    @Query("SELECT * FROM graph_edges WHERE type = :type ORDER BY id ASC")
    fun observeByType(type: String): Flow<List<GraphEdgeEntity>>

    /** 查询与某节点相关的所有边（作为起点或终点） */
    @Query("SELECT * FROM graph_edges WHERE source_id = :nodeId OR target_id = :nodeId ORDER BY id ASC")
    fun observeRelatedTo(nodeId: String): Flow<List<GraphEdgeEntity>>

    @Query("SELECT * FROM graph_edges ORDER BY id ASC")
    fun observeAll(): Flow<List<GraphEdgeEntity>>
}
