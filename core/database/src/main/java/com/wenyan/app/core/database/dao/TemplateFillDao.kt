package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.TemplateFillEntity
import kotlinx.coroutines.flow.Flow

/**
 * 模板填写记录表 DAO。
 */
@Dao
interface TemplateFillDao {

    @Upsert
    suspend fun insert(entity: TemplateFillEntity)

    @Upsert
    suspend fun insertAll(entities: List<TemplateFillEntity>)

    @Update
    suspend fun update(entity: TemplateFillEntity)

    @Query("DELETE FROM template_fills WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM template_fills WHERE id = :id")
    suspend fun getById(id: String): TemplateFillEntity?

    @Query("SELECT * FROM template_fills WHERE template_id = :templateId ORDER BY created_at DESC, id ASC")
    fun observeByTemplate(templateId: String): Flow<List<TemplateFillEntity>>

    @Query("SELECT * FROM template_fills WHERE exam_question_id = :questionId ORDER BY created_at DESC, id ASC")
    fun observeByExamQuestion(questionId: String): Flow<List<TemplateFillEntity>>

    @Query("SELECT * FROM template_fills ORDER BY created_at DESC, id ASC")
    fun observeAll(): Flow<List<TemplateFillEntity>>
}
