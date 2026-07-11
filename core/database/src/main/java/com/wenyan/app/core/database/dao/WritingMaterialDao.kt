package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.WritingMaterialEntity
import kotlinx.coroutines.flow.Flow

/**
 * 写作素材表 DAO。
 */
@Dao
interface WritingMaterialDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WritingMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<WritingMaterialEntity>)

    @Update
    suspend fun update(entity: WritingMaterialEntity)

    @Query("DELETE FROM writing_materials WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM writing_materials WHERE id = :id")
    suspend fun getById(id: String): WritingMaterialEntity?

    @Query("SELECT * FROM writing_materials WHERE category = :category ORDER BY created_at DESC")
    fun observeByCategory(category: String): Flow<List<WritingMaterialEntity>>

    @Query("SELECT * FROM writing_materials WHERE tags LIKE '%' || :tag || '%' ORDER BY created_at DESC")
    fun observeByTag(tag: String): Flow<List<WritingMaterialEntity>>

    @Query("SELECT * FROM writing_materials ORDER BY created_at DESC")
    fun observeAll(): Flow<List<WritingMaterialEntity>>
}
