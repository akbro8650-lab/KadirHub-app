package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.crypto.CryptoVaultManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.UUID

class CatalogRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun adminPublishEncryptedFileAndLink_regularUserDownloadsAndDecrypts() = runBlocking {
        val adminUid = signInTestUser(ADMIN_EMAIL)
        seedEmulatorAdminDoc(adminUid, ADMIN_EMAIL)
        val adminRepo = CatalogRepository(firestore, auth)

        val samplePayload = "KadirHub encrypted package bytes ${UUID.randomUUID()}".toByteArray(StandardCharsets.UTF_8)
        val secretUrl = "https://example.com/kadirhub-release-${UUID.randomUUID()}.apk"

        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.publishCatalogItem(
                title = "Kadir Cleaner Pro",
                description = "Verified utility tool uploaded by @kadirofficial007",
                category = "App",
                version = "3.0.1",
                targetUrl = secretUrl,
                fileSizeLabel = "1.2 KB",
                attachedFileName = "kadir_cleaner.bin",
                attachedMimeType = "application/octet-stream",
                attachedBytes = samplePayload
            ).getOrThrow()
        }

        // Switch to regular user Alice
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CatalogRepository(firestore, auth)

        val approvedItems = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeApprovedCatalog().first { list -> list.any { it.id == itemId } }
        }
        val item = approvedItems.first { it.id == itemId }

        // Decrypt URL and verify AES-256-GCM round-trip
        val decryptedUrl = CryptoVaultManager.decryptString(item.encryptedTargetUrl, item.urlIv)
        assertEquals(secretUrl, decryptedUrl)

        // Download & decrypt attached file with SHA-256 verification
        val downloaded = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.downloadAndDecryptFile(item.encryptedFileId).getOrThrow()
        }
        assertTrue(downloaded.integrityVerified)
        assertArrayEquals(samplePayload, downloaded.bytes)

        // Record download increment
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.recordDownload(itemId).getOrThrow()
        }

        // Submit user review & aggregate rating
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.submitOrUpdateReview(
                itemId = itemId,
                itemTitle = item.title,
                rating = 5,
                comment = "Verified encrypted download!"
            ).getOrThrow()
        }

        val reviews = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeReviewsForItem(itemId).first { it.isNotEmpty() }
        }
        assertEquals(5L, reviews.first().rating)
    }

    @Test
    fun regularUser_publishCatalogItem_failsWithPermissionDenied() = runBlocking {
        signInTestUser(BOB_EMAIL)
        val bobRepo = CatalogRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.publishCatalogItem(
                title = "Unauthorized App",
                description = "Should be blocked by security rules",
                category = "App",
                version = "1.0.0",
                targetUrl = "https://example.com/bad",
                fileSizeLabel = "1 MB"
            )
        }
        assertTrue(result.isFailure)
        val firestoreEx = checkNotNull(result.exceptionOrNull()?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got ${result.exceptionOrNull()}"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun unauthenticatedUser_observeApprovedCatalog_failsBeforeQueryOrPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CatalogRepository(firestore, auth)
        var failed = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeApprovedCatalog().first()
            }
        } catch (_: Exception) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun unauthenticatedDirectFirestoreQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        var ex: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("catalog_items")
                    .whereEqualTo("status", "approved")
                    .get()
                    .let { task ->
                        val latch = kotlinx.coroutines.CompletableDeferred<Unit>()
                        task.addOnSuccessListener { latch.complete(Unit) }
                        task.addOnFailureListener { e -> latch.completeExceptionally(e) }
                        latch.await()
                    }
            }
        } catch (e: Exception) {
            ex = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    private companion object {
        const val ADMIN_EMAIL = "admin-test@kadirhub.test"
        const val ALICE_EMAIL = "alice@kadirhub.test"
        const val BOB_EMAIL = "bob@kadirhub.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
