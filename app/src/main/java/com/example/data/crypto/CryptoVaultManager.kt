package com.example.data.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String,
    val sha256Checksum: String,
    val algorithm: String = "AES-256-GCM"
)

object CryptoVaultManager {
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val VAULT_KEY_SEED = "KadirHub::@kadirofficial007::AES256GCM::VaultKey::v1"

    private val secureRandom = SecureRandom()

    private fun deriveVaultKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(VAULT_KEY_SEED.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun encryptString(plaintext: String): EncryptedPayload {
        return encryptBytes(plaintext.toByteArray(StandardCharsets.UTF_8))
    }

    fun decryptString(ciphertextBase64: String, ivBase64: String): String {
        val bytes = decryptBytes(ciphertextBase64, ivBase64)
        return String(bytes, StandardCharsets.UTF_8)
    }

    fun encryptBytes(plainBytes: ByteArray): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveVaultKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val encryptedBytes = cipher.doFinal(plainBytes)
        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            sha256Checksum = sha256Hex(plainBytes),
            algorithm = "AES-256-GCM"
        )
    }

    fun decryptBytes(ciphertextBase64: String, ivBase64: String): ByteArray {
        val encryptedBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, deriveVaultKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encryptedBytes)
    }
}
