package com.wenyan.app.core.data.mapper

import com.wenyan.app.core.ai.RagReference
import kotlinx.serialization.json.Json

/**
 * 聊天消息映射器(NF-PP6 Wave 2.3)。
 *
 * 负责 [RagReference] 列表与 JSON 字符串的双向序列化,供
 * [com.wenyan.app.core.data.repository.ChatRepositoryImpl] 在
 * [com.wenyan.app.core.database.entity.ChatMessageEntity.referencesJson]
 * 字段存储/读取 RAG 引用。
 *
 * 设计说明:
 * - [RagReference] 已加 @Serializable 注解(core:ai 模块,有 kotlinx.serialization 依赖)
 * - 用单例 [Json] 实例,ignoreUnknownKeys=true 容错向前兼容(未来加字段不破坏旧数据)
 * - 空列表序列化为 "[]"(非 null),null 输入返回 null(表示无引用字段)
 */
object ChatMessageMapper {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 序列化 [RagReference] 列表为 JSON 字符串。
     *
     * @param refs 引用列表(可为空)
     * @return JSON 字符串,空列表返回 "[]",null 输入返回 null
     */
    fun serializeReferences(refs: List<RagReference>?): String? {
        if (refs == null) return null
        return json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(RagReference.serializer()),
            refs,
        )
    }

    /**
     * 反序列化 JSON 字符串为 [RagReference] 列表。
     *
     * @param jsonStr JSON 字符串(可为 null 或空)
     * @return 引用列表,null 或空字符串返回空列表
     */
    fun deserializeReferences(jsonStr: String?): List<RagReference> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(RagReference.serializer()),
                jsonStr,
            )
        }.getOrElse { emptyList() }
    }
}
