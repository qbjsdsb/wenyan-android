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

    /**
     * 删除 App 根据 seed_data.json 管理的知识点来源，保留未来用户手工添加的来源。
     *
     * 每次种子升级都先清理再重建，避免教材列表删除或改名后留下幽灵来源。
     */
    @Query(
        "DELETE FROM data_sources WHERE " +
            "id LIKE 'seed-kp-source:%' OR id LIKE 'seed-eq-source:%' OR id LIKE 'seed-wm-source:%'",
    )
    suspend fun deleteManagedSeedSources()

    @Query("SELECT * FROM data_sources WHERE id = :id")
    suspend fun getById(id: String): DataSourceEntity?

    @Query("SELECT * FROM data_sources WHERE knowledge_point_id = :pointId ORDER BY created_at ASC")
    fun observeByKnowledgePoint(pointId: String): Flow<List<DataSourceEntity>>

    @Query("SELECT * FROM data_sources WHERE exam_question_id = :questionId ORDER BY created_at ASC")
    fun observeByExamQuestion(questionId: String): Flow<List<DataSourceEntity>>

    @Query("SELECT * FROM data_sources WHERE writing_material_id = :materialId ORDER BY created_at ASC")
    fun observeByWritingMaterial(materialId: String): Flow<List<DataSourceEntity>>

    @Query("SELECT * FROM data_sources WHERE content_source = :source ORDER BY created_at ASC")
    fun observeByContentSource(source: String): Flow<List<DataSourceEntity>>

    /** 按 OCR 状态查询 */
    @Query("SELECT * FROM data_sources WHERE ocr_status = :status ORDER BY created_at ASC")
    fun observeByOcrStatus(status: String): Flow<List<DataSourceEntity>>

    @Query("SELECT * FROM data_sources ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DataSourceEntity>>
}
