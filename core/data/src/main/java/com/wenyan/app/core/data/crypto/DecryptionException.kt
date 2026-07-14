package com.wenyan.app.core.data.crypto

/**
 * API Key 解密异常。
 *
 * NF-E8 修正：[ApiKeyCrypto.decrypt] 在数据不完整（IV + 密文长度不足）、Base64 解码失败、
 * KeyStore 不可用、GCM 认证失败（密文被篡改或 master key 变更）等场景抛出本异常。
 *
 * 与"合法空 apiKey"区分：
 * - 合法空 apiKey：`encrypt("")` 返回 `""`，`decrypt("")` 返回 `""`（不抛异常）
 * - 解密失败：抛本异常，调用方（如 [com.wenyan.app.core.data.repository.ApiConfigRepository.decryptedOrNull]）
 *   用 `runCatching` 捕获并降级为 null，避免污染正常 apiKey 列表
 *
 * 原实现 `decrypt` 在数据不完整时静默返回 `""`，导致：
 * - 合法空 apiKey 与损坏密文无法区分（两者都返回 `""`）
 * - 用户看到一个"空 apiKey"的配置，误以为是数据问题而非密钥损坏
 *
 * @param message 错误描述
 * @param cause 底层异常（如 [javax.crypto.AEADBadTagException] / [java.security.KeyStoreException]）
 */
class DecryptionException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
