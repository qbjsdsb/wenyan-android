package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.TemplateFillEntity
import kotlinx.coroutines.flow.Flow

/**
 * 模板填写记录表 DAO。
 */
@Dao
interface TemplateFillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TemplateFillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TemplateFillEntity>)

    @Update
    suspend fun update(entity: TemplateFillEntity)

    @Query("DELETE FROM template_fills WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM template_fills WHERE id = :id")
    suspend fun getById(id: String): TemplateFillEntity?

    @Query("SELECT * FROM template_fills WHERE template_id = :templateId ORDER BY created_at DESC")
    fun observeByTemplate(templateId: String): Flow<List<TemplateFillEntity>>

    @Query("SELECT * FROM template_fills WHERE exam_question_id = :questionId ORDER BY created_at DESC")
    fun observeByExamQuestion(questionId: String): Flow<List<TemplateFillEntity>>

    @Query("SELECT * FROM template_fills ORDER BY created_at DESC")
    fun observeAll(): Flow<List<TemplateFillEntity>>
}
