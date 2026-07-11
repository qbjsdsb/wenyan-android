package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.AnswerTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 答题模板表 DAO。
 */
@Dao
interface AnswerTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnswerTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AnswerTemplateEntity>)

    @Update
    suspend fun update(entity: AnswerTemplateEntity)

    @Query("DELETE FROM answer_templates WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM answer_templates WHERE id = :id")
    suspend fun getById(id: String): AnswerTemplateEntity?

    @Query("SELECT * FROM answer_templates WHERE question_type = :type ORDER BY created_at ASC")
    fun observeByQuestionType(type: String): Flow<List<AnswerTemplateEntity>>

    @Query("SELECT * FROM answer_templates WHERE is_builtin = 1 ORDER BY created_at ASC")
    fun observeBuiltin(): Flow<List<AnswerTemplateEntity>>

    @Query("SELECT * FROM answer_templates ORDER BY created_at ASC")
    fun observeAll(): Flow<List<AnswerTemplateEntity>>
}
