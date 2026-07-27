package com.wenyan.app.core.data.mapper

import com.wenyan.app.core.ai.RagReference
import kotlinx.serialization.json.Json
import timber.log.Timber

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
     * v0.8.16 P1-6 修复：原实现反序列化失败时静默返回 emptyList()，
     * 用户重启 App 后看到"AI 回复丢失了引用"，但日志中无任何线索。
     * 现添加 Log.w 输出原始异常 + JSON 前 200 字符，便于排查 schema 不兼容或数据损坏。
     *
     * 仍保持 fallback 为 emptyList()（不阻塞消息展示），与之前行为一致。
     *
     * @param jsonStr JSON 字符串(可为 null 或空)
     * @return 引用列表,null 或空字符串返回空列表;反序列化失败也返回空列表(并记录日志)
     */
    fun deserializeReferences(jsonStr: String?): List<RagReference> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(RagReference.serializer()),
                jsonStr,
            )
        }.getOrElse { e ->
            // v0.8.16 P1-6：记录反序列化失败，便于排查
            // - schema 变更后旧数据无法反序列化
            // - DB 损坏 / 写入中断
            // - 测试 Fake 数据格式错误
            // 输出 JSON 前 200 字符（避免超长日志），异常 message 含具体原因
            val preview = jsonStr.take(200)
            Timber.w(e, "deserializeReferences failed, returning empty list")
            Timber.w("invalid JSON (first 200 chars): $preview")
            emptyList()
        }
    }
}
