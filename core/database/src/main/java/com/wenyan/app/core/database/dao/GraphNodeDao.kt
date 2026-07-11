package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.GraphNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * 图谱节点表 DAO。
 */
@Dao
interface GraphNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GraphNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<GraphNodeEntity>)

    @Update
    suspend fun update(entity: GraphNodeEntity)

    @Query("DELETE FROM graph_nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM graph_nodes WHERE id = :id")
    suspend fun getById(id: String): GraphNodeEntity?

    @Query("SELECT * FROM graph_nodes WHERE type = :type")
    fun observeByType(type: String): Flow<List<GraphNodeEntity>>

    @Query("SELECT * FROM graph_nodes WHERE subject_id = :subjectId")
    fun observeBySubject(subjectId: String): Flow<List<GraphNodeEntity>>

    @Query("SELECT * FROM graph_nodes WHERE related_point_id = :pointId")
    fun observeByKnowledgePoint(pointId: String): Flow<List<GraphNodeEntity>>

    @Query("SELECT * FROM graph_nodes")
    fun observeAll(): Flow<List<GraphNodeEntity>>
}
