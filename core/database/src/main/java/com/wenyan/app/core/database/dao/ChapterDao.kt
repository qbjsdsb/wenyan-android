package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * 章节表 DAO。
 */
@Dao
interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ChapterEntity>)

    @Update
    suspend fun update(entity: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE subject_id = :subjectId ORDER BY sort_order ASC")
    fun observeBySubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE parent_id = :parentId ORDER BY sort_order ASC")
    fun observeChildren(parentId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subject_id = :subjectId AND parent_id IS NULL ORDER BY sort_order ASC")
    fun observeRoots(subjectId: String): Flow<List<ChapterEntity>>
}
