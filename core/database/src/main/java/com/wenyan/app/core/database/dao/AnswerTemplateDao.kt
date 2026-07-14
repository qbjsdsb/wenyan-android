package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.AnswerTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 答题模板表 DAO。
 */
@Dao
interface AnswerTemplateDao {

    // P1 修正:原用 @Insert(REPLACE),DELETE+INSERT 会触发子表 CASCADE
    // (template_fills 级联删除,用户答题记录丢失)。改用 @Upsert。
    @Upsert
    suspend fun insert(entity: AnswerTemplateEntity)

    @Upsert
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
