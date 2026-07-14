package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wenyan.app.core.database.entity.AppMetaEntity

/**
 * 应用元数据表 DAO（NF-B / P0-E4 修复新增）。
 *
 * 提供按 key 读写 [AppMetaEntity] 的能力，供 [com.wenyan.app.core.data.repository.ClockGuard]
 * 等组件存储应用级元数据（如最近已知时间戳）。
 *
 * 写入用 [OnConflictStrategy.REPLACE]：upsert 语义，已存在 key 则覆盖（更新时间戳用）。
 */
@Dao
interface AppMetaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppMetaEntity)

    @Query("SELECT * FROM app_meta WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): AppMetaEntity?
}
