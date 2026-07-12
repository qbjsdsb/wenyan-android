package com.wenyan.app.core.data.crypto

import java.util.Base64

/**
 * [ApiKeyCrypto] 的 Fake 实现，供单元测试使用。
 *
 * 不使用 AndroidKeyStore（JVM 环境不可用），用简单 Base64 编码模拟加密。
 * 满足测试需求：encrypt → decrypt 往返一致性。
 */
class FakeApiKeyCrypto : ApiKeyCrypto {

    override fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(
            plaintext.toByteArray(Charsets.UTF_8),
        )
    }

    override fun decrypt(ciphertext: String): String {
        if (ciphertext.isEmpty()) return ""
        if (!ciphertext.startsWith(ENCRYPTED_PREFIX)) return ciphertext
        val encoded = ciphertext.removePrefix(ENCRYPTED_PREFIX)
        return String(
            Base64.getDecoder().decode(encoded),
            Charsets.UTF_8,
        )
    }

    private companion object {
        private const val ENCRYPTED_PREFIX = "ENC:"
    }
}
