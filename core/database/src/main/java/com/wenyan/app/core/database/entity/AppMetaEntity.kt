package com.wenyan.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 应用元数据表 Entity（app_meta，NF-B / P0-E4 修复新增）。
 *
 * 通用 key-value 结构，存储应用级单行元数据。当前用途：
 * - `last_known_timestamp_ms`：[com.wenyan.app.core.data.repository.ClockGuard] 用，记录最近一次已知有效时间戳，
 *   用于检测系统时钟回拨（用户手动改时间 / 时区切换 / NTP 异常）。
 *
 * 设计：通用 key-value 而非单行单列，便于未来扩展（如 last_seed_version、last_boot_count 等）
 * 而无需再次迁移 schema。
 *
 * @property key 主键，元数据键名（如 `last_known_timestamp_ms`）
 * @property longValue Long 值（可空，时间戳类元数据用）
 * @property stringValue String 值（可空，文本类元数据用）
 */
@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "long_value")
    val longValue: Long? = null,

    @ColumnInfo(name = "string_value")
    val stringValue: String? = null,
)
