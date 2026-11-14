package com.example.data.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CryptoVaultManagerTest {

    @Test
    fun adminPasswordVerification_acceptsExactPasswordAndRejectsInvalid() {
        assertTrue(CryptoVaultManager.verifyAdminPassword("kadir@25"))
        assertFalse(CryptoVaultManager.verifyAdminPassword("kadir@26"))
        assertFalse(CryptoVaultManager.verifyAdminPassword(""))
    }

    @Test
    fun encryptAndDecryptString_roundTripsWithSha256Integrity() {
        val url = "https://example.com/releases/kadirhub-v2.apk"
        val payload = CryptoVaultManager.encryptString(url)
        val decrypted = CryptoVaultManager.decryptString(payload.ciphertextBase64, payload.ivBase64)
        assertEquals(url, decrypted)
        assertEquals(64, payload.sha256Checksum.length)
        assertEquals("AES-256-GCM", payload.algorithm)
    }

    @Test
    fun encryptAndDecryptBytes_roundTripsCleanly() {
        val raw = ByteArray(1024) { (it % 251).toByte() }
        val payload = CryptoVaultManager.encryptBytes(raw)
        val decrypted = CryptoVaultManager.decryptBytes(payload.ciphertextBase64, payload.ivBase64)
        assertArrayEquals(raw, decrypted)
        assertEquals(CryptoVaultManager.sha256Hex(raw), payload.sha256Checksum)
    }
}
