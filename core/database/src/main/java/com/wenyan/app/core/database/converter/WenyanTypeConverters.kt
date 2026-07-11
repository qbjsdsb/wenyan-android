package com.wenyan.app.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 文研App Room 数据库 TypeConverter。
 *
 * 提供 JSON 字段与 Kotlin 类型之间的双向转换：
 * - List<String> ↔ String（JSON 数组，如 related_ids / tags / prerequisites）
 * - Map<String, String> ↔ String（JSON 对象，如 multi_perspectives / metadata）
 *
 * 对于结构更复杂的 JSON 字段（如 exam_records / grading_result / structure），
 * 在 Entity 中直接使用 String 存储，由业务层自行解析。
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
        return json.decodeFromString(value)
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
        return json.decodeFromString(value)
    }
}
