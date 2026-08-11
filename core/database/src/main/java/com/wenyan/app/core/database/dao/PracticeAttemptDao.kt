package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.PracticeAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeAttemptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attempt: PracticeAttemptEntity)

    @Update
    suspend fun update(attempt: PracticeAttemptEntity): Int

    @Query("SELECT * FROM practice_attempts WHERE id = :id")
    suspend fun getById(id: String): PracticeAttemptEntity?

    @Query(
        """
        SELECT * FROM practice_attempts
        WHERE session_id = :sessionId AND question_id = :questionId
        ORDER BY updated_at DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestBySessionAndQuestion(sessionId: String, questionId: String): PracticeAttemptEntity?

    @Query("SELECT * FROM practice_attempts WHERE question_id = :questionId ORDER BY created_at DESC, id DESC")
    fun observeByQuestion(questionId: String): Flow<List<PracticeAttemptEntity>>

    @Query("SELECT * FROM practice_attempts WHERE session_id = :sessionId ORDER BY created_at, id")
    fun observeBySession(sessionId: String): Flow<List<PracticeAttemptEntity>>

    @Query("SELECT * FROM practice_attempts WHERE repair_state IN ('CANDIDATE', 'SCHEDULED') ORDER BY updated_at, id")
    fun observePendingRepair(): Flow<List<PracticeAttemptEntity>>
}
