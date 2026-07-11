package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * 科目表 DAO。
 */
@Dao
interface SubjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SubjectEntity>)

    @Update
    suspend fun update(entity: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int
}
