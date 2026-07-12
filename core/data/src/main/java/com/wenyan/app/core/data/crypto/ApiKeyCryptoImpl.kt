package com.wenyan.app.core.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ApiKeyCrypto] 的生产实现，使用 AndroidKeyStore + AES-256-GCM。
 *
 * 加密流程：
 * 1. 在 AndroidKeyStore 中生成/获取 AES-256 master key（别名 [MASTER_KEY_ALIAS]）
 * 2. 用 AES/GCM/NoPadding 加密明文，生成 12 字节 IV + 密文
 * 3. 组合 IV + 密文，Base64 编码后存入数据库
 *
 * 解密流程：
 * 1. Base64 解码，拆分 IV（前 12 字节）和密文
 * 2. 用 master key + GCMParameterSpec(128, iv) 解密
 *
 * 注意：AndroidKeyStore 仅在 Android 设备上可用，JVM 单元测试需用 Fake 实现。
 *
 * @param context 应用 Context（用于 AndroidKeyStore 初始化，实际未直接使用，
 *                保留参数以便未来扩展为 EncryptedSharedPreferences 方案）
 */
@Singleton
class ApiKeyCryptoImpl @Inject constructor(
    private val context: Context,
) : ApiKeyCrypto {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
    }

    private val masterKey: SecretKey by lazy {
        keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey ?: generateMasterKey()
    }

    override fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        // 组合 IV + 密文，Base64 编码
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decrypt(ciphertext: String): String {
        if (ciphertext.isEmpty()) return ""
        val data = Base64.decode(ciphertext, Base64.NO_WRAP)
        if (data.size < GCM_IV_SIZE + 1) return "" // 数据不完整
        val iv = data.copyOfRange(0, GCM_IV_SIZE)
        val encrypted = data.copyOfRange(GCM_IV_SIZE, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    /** 在 AndroidKeyStore 中生成 AES-256 master key（仅首次调用） */
    private fun generateMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "wenyan_api_key_master"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_SIZE = 12       // GCM 标准 IV 长度
        private const val GCM_TAG_BITS = 128     // GCM 认证标签位数
    }
}
