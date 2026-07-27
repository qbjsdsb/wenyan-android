package com.wenyan.app.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * 文研App Room 数据库 TypeConverter。
 *
 * 提供 JSON 字段与 Kotlin 类型之间的双向转换：
 * - List<String> ↔ String（JSON 数组，如 related_ids / tags / prerequisites）
 * - Map<String, String> ↔ String（JSON 对象，如 multi_perspectives / metadata）
 *
 * 对于结构更复杂的 JSON 字段（如 exam_records / grading_result / structure），
 * 在 Entity 中直接使用 String 存储，由业务层自行解析。
 *
 * P0-1 修复：解析失败时降级返回空集合而非抛异常。
 * 原因：任何一行 JSON 字段损坏（外部 DB 编辑 / 历史 bug 写入脏数据 / 版本回退
 * 导致 schema 不一致）都会让 [json.decodeFromString] 抛 [SerializationException]，
 * Room 会把异常向上抛给查询，导致整张表所有行读取失败，用户看到"知识点全部消失"。
 * 现改为 runCatching 包裹，损坏行降级为空集合，至少不阻塞其他行的读取。
 */
class WenyanTypeConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---------------- List<String> ----------------

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return null
        // P0-1 修复：解析失败降级为空列表，避免单行损坏导致整表读取失败
        return runCatching { json.decodeFromString<List<String>>(value) }
            .onFailure { e ->
                Timber.w(e, "Failed to decode List<String> from JSON, returning empty list")
            }
            .getOrNull()
            ?: emptyList()
    }

    // ---------------- Map<String, String> ----------------

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        if (value == null) return null
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        if (value.isNullOrEmpty()) return null
        // P0-1 修复：解析失败降级为空 Map，避免单行损坏导致整表读取失败
        return runCatching { json.decodeFromString<Map<String, String>>(value) }
            .onFailure { e ->
                Timber.w(e, "Failed to decode Map<String, String> from JSON, returning empty map")
            }
            .getOrNull()
            ?: emptyMap()
    }
}
