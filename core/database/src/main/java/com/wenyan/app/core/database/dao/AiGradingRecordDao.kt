package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.AiGradingRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI 批改记录表 DAO。
 */
@Dao
interface AiGradingRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AiGradingRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AiGradingRecordEntity>)

    @Update
    suspend fun update(entity: AiGradingRecordEntity)

    @Query("DELETE FROM ai_grading_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM ai_grading_records WHERE id = :id")
    suspend fun getById(id: String): AiGradingRecordEntity?

    @Query("SELECT * FROM ai_grading_records WHERE exam_question_id = :questionId ORDER BY created_at DESC")
    fun observeByExamQuestion(questionId: String): Flow<List<AiGradingRecordEntity>>

    @Query("SELECT * FROM ai_grading_records ORDER BY created_at DESC")
    fun observeAll(): Flow<List<AiGradingRecordEntity>>
}
