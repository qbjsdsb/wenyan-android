package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * API 配置表 DAO。
 */
@Dao
interface ApiConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ApiConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ApiConfigEntity>)

    @Update
    suspend fun update(entity: ApiConfigEntity)

    @Query("DELETE FROM api_configs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM api_configs WHERE id = :id")
    suspend fun getById(id: String): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE is_current = 1 LIMIT 1")
    suspend fun getCurrent(): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE is_current = 1 LIMIT 1")
    fun observeCurrent(): Flow<ApiConfigEntity?>

    /** 将指定配置设为当前，其余全部取消当前 */
    @Query("UPDATE api_configs SET is_current = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setCurrent(id: String)

    @Query("SELECT * FROM api_configs WHERE is_enabled = 1 ORDER BY created_at ASC")
    fun observeEnabled(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_configs ORDER BY created_at ASC")
    fun observeAll(): Flow<List<ApiConfigEntity>>
}
