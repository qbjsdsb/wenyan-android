package com.wenyan.app.core.data.crypto

/**
 * API Key 加解密接口。
 *
 * 用于 [com.wenyan.app.core.data.repository.ApiConfigRepository] 对 apiKey 字段做加密存储。
 * 数据库 `api_configs.api_key` 列存储密文（Base64 编码的 AES-GCM 加密结果），
 * 业务层读取时通过本接口解密为明文。
 *
 * 设计为接口以便单元测试注入 Fake 实现（AndroidKeyStore 在 JVM 测试环境不可用）。
 */
interface ApiKeyCrypto {

    /** 加密明文 API Key，返回 Base64 编码的密文（含 IV 前缀） */
    fun encrypt(plaintext: String): String

    /** 解密 Base64 编码的密文，返回明文 API Key */
    fun decrypt(ciphertext: String): String
}
