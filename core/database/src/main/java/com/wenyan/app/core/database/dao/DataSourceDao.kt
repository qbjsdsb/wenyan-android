package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.DataSourceEntity
import kotlinx.coroutines.flow.Flow

/**
 * 资料来源溯源表 DAO（Spec 新增表）。
 */
@Dao
interface DataSourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DataSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DataSourceEntity>)

    @Update
    suspend fun update(entity: DataSourceEntity)

    @Query("DELETE FROM data_sources WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM data_sources WHERE id = :id")
    suspend fun getById(id: String): DataSourceEntity?

    @Query("SELECT * FROM data_sources WHERE knowledge_point_id = :pointId")
    fun observeByKnowledgePoint(pointId: String): Flow<List<DataSourceEntity>>

    @Query("SELECT * FROM data_sources WHERE exam_question_id = :questionId")
    fun observeByExamQuestion(questionId: String): Flow<List<DataSourceEntity>>

    @Query("SELECT * FROM data_sources WHERE content_source = :source")
    fun observeByContentSource(source: String): Flow<List<DataSourceEntity>>

    /** 按 OCR 状态查询 */
    @Query("SELECT * FROM data_sources WHERE ocr_status = :status")
    fun observeByOcrStatus(status: String): Flow<List<DataSourceEntity>>

    @Query("SELECT * FROM data_sources ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DataSourceEntity>>
}
