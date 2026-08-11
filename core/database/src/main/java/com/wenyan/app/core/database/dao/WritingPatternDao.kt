package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.WritingPatternEntity
import kotlinx.coroutines.flow.Flow

/**
 * 写作句式表 DAO。
 */
@Dao
interface WritingPatternDao {

    @Upsert
    suspend fun insert(entity: WritingPatternEntity)

    @Upsert
    suspend fun insertAll(entities: List<WritingPatternEntity>)

    @Update
    suspend fun update(entity: WritingPatternEntity)

    @Query("DELETE FROM writing_patterns WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM writing_patterns WHERE id = :id")
    suspend fun getById(id: String): WritingPatternEntity?

    @Query("SELECT * FROM writing_patterns WHERE category = :category ORDER BY created_at ASC, id ASC")
    fun observeByCategory(category: String): Flow<List<WritingPatternEntity>>

    @Query("SELECT * FROM writing_patterns WHERE is_builtin = 1 ORDER BY created_at ASC, id ASC")
    fun observeBuiltin(): Flow<List<WritingPatternEntity>>

    @Query("SELECT * FROM writing_patterns ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<WritingPatternEntity>>
}
