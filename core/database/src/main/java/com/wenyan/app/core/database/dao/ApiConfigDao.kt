package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * API 配置表 DAO。
 *
 * v0.8.12 修复（P1-3 反向验证发现）：原 [insert] / [insertAll] 用
 * `@Insert(onConflict = OnConflictStrategy.REPLACE)`，REPLACE 在 SQLite 中等价于
 * DELETE + INSERT。删除 api_configs 行时，子表 ai_grading_records 的
 * `api_config_id` 外键（onDelete = ForeignKey.SET_NULL）被置 NULL，
 * 导致历史批改记录丢失"使用哪个 API 配置"的关联信息。
 * 现改用 [@Upsert]（INSERT ... ON CONFLICT DO UPDATE），不触发 DELETE，
 * 安全更新已存在的配置行。
 */
@Dao
interface ApiConfigDao {

    @Upsert
    suspend fun insert(entity: ApiConfigEntity)

    @Upsert
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
