package com.wenyan.app.core.data.crypto

/**
 * API Key 加解密接口。
 *
 * 用于 [com.wenyan.app.core.data.repository.ApiConfigRepository] 对 apiKey 字段做加密存储。
 * 数据库 `api_configs.api_key` 列存储密文（Base64 编码的 AES-GCM 加密结果），
 * 业务层读取时通过本接口解密为明文。
 *
 * 设计为接口以便单元测试注入 Fake 实现（AndroidKeyStore 在 JVM 测试环境不可用）。
 *
 * NF-E8 修正：[decrypt] 在密文损坏或解密失败时抛 [DecryptionException]，
 * 与"合法空 apiKey"（[encrypt]("") 返回 ""，[decrypt]("") 返回 ""）明确区分。
 * 调用方应使用 `runCatching { decrypt(...) }.getOrNull()` 包装以容错。
 */
interface ApiKeyCrypto {

    /**
     * 加密明文 API Key，返回 Base64 编码的密文（含 IV 前缀）。
     *
     * 空字符串输入返回空字符串（合法空 apiKey，不解密也不加密）。
     */
    fun encrypt(plaintext: String): String

    /**
     * 解密 Base64 编码的密文，返回明文 API Key。
     *
     * - 空字符串输入返回空字符串（合法空 apiKey，不抛异常）
     * - 数据不完整（IV + 密文长度不足）、Base64 解码失败、GCM 认证失败（密文被篡改或
     *   master key 变更）等场景抛 [DecryptionException]
     *
     * 调用方应使用 `runCatching { decrypt(...) }.getOrNull()` 包装以容错，
     * 单条解密失败时降级为 null（被上层 `mapNotNull` 过滤），其他配置仍可用。
     *
     * @throws DecryptionException 密文损坏或解密失败时抛出
     */
    @Throws(DecryptionException::class)
    fun decrypt(ciphertext: String): String
}
