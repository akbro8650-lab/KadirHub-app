package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.crypto.CryptoVaultManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID

class CatalogRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun adminPublishEncryptedFileAndLink_regularUserDownloadsAndDecrypts(): Unit = runBlocking {
        val adminUid = signInTestUser(ADMIN_EMAIL)
        seedEmulatorAdminDoc(adminUid, ADMIN_EMAIL)
        val adminRepo = CatalogRepository(firestore, auth)

        val samplePayload = "KadirHub encrypted package bytes ${UUID.randomUUID()}".toByteArray(StandardCharsets.UTF_8)
        val secretUrl = "https://example.com/kadirhub-release-${UUID.randomUUID()}.apk"

        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.publishCatalogItem(
                title = "Kadir Cleaner Pro",
                description = "Verified utility tool uploaded by @kadirofficial007",
                whatsNew = "v3.0.1 performance improvements",
                category = "App",
                version = "3.0.1",
                targetUrl = secretUrl,
                fileSizeLabel = "1.2 KB",
                iconBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
                fileSource = UploadFileSource.InMemory(
                    fileName = "kadir_cleaner.apk",
                    mimeType = "application/vnd.android.package-archive",
                    bytes = samplePayload
                ),
                publishAsApproved = true,
                isFeatured = true
            ).getOrThrow()
        }

        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CatalogRepository(firestore, auth)

        val approvedItems = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeApprovedCatalog().first { list -> list.any { it.id == itemId } }
        }
        val item = approvedItems.first { it.id == itemId }
        assertEquals("iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB", item.iconBase64)
        assertEquals("v3.0.1 performance improvements", item.whatsNew)
        assertTrue(item.isFeatured)

        val decryptedUrl = CryptoVaultManager.decryptString(item.encryptedTargetUrl, item.urlIv)
        assertEquals(secretUrl, decryptedUrl)

        val downloaded = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.downloadAndDecryptFile(item.encryptedFileId).getOrThrow()
        }
        assertTrue(downloaded.integrityVerified)
        assertArrayEquals(samplePayload, downloaded.bytes)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.recordDownload(itemId).getOrThrow()
        }

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

        // Alice can toggle bookmark for her own user only
        val bookmarked = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.toggleBookmark(itemId, currentlyBookmarked = false).getOrThrow()
        }
        assertTrue(bookmarked)
        val bookmarkedSet = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeBookmarkedIds().first { it.contains(itemId) }
        }
        assertTrue(bookmarkedSet.contains(itemId))
    }

    @Test
    fun regularUserUploadsChunkedApkAsPending_adminApproves_thenPublicDownloadsAndVerifies(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CatalogRepository(firestore, auth)

        // Multi-chunk binary payload (4 chunks of 1024 bytes + partial = 3,500 bytes)
        val apkBytes = ByteArray(3500) { idx -> ((idx * 31) and 0xFF).toByte() }
        val tempDownloadFile = File.createTempFile("kadirhub_chunk_test", ".apk").apply { deleteOnExit() }

        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.publishCatalogItem(
                title = "Community Mod Apk ${UUID.randomUUID().toString().take(6)}",
                description = "Uploaded by regular user with custom icon, pending approval",
                whatsNew = "First public beta",
                category = "App",
                version = "1.2.0",
                targetUrl = "",
                fileSizeLabel = "3.4 KB",
                iconBase64 = "ZmFrZV9pY29uX2Jhc2U2NA==",
                fileSource = UploadFileSource.InMemory(
                    fileName = "community_app.apk",
                    mimeType = "application/vnd.android.package-archive",
                    bytes = apkBytes
                ),
                publishAsApproved = false,
                chunkSizeBytes = 1024
            ).getOrThrow()
        }

        // Alice sees her own pending item in My Uploads
        val aliceUploads = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMyUploads().first { list -> list.any { it.id == itemId } }
        }
        val pendingItem = aliceUploads.first { it.id == itemId }
        assertEquals("pending", pendingItem.status)
        assertEquals("ZmFrZV9pY29uX2Jhc2U2NA==", pendingItem.iconBase64)

        // Bob does NOT see unapproved upload in approved catalog
        signInTestUser(BOB_EMAIL)
        val bobRepo = CatalogRepository(firestore, auth)
        val bobApproved = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeApprovedCatalog().first()
        }
        assertFalse(bobApproved.any { it.id == itemId })

        // Admin bulk-approves pending item & all encrypted chunks
        val adminUid = signInTestUser(ADMIN_EMAIL)
        seedEmulatorAdminDoc(adminUid, ADMIN_EMAIL)
        val adminRepo = CatalogRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.adminApproveAllPending(listOf(pendingItem)).getOrThrow()
        }

        // Now Bob sees it and downloads & verifies the streamed chunked APK
        signInTestUser(BOB_EMAIL)
        val bobApprovedAfter = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeApprovedCatalog().first { list -> list.any { it.id == itemId } }
        }
        val approvedItem = bobApprovedAfter.first { it.id == itemId }
        assertEquals("approved", approvedItem.status)

        val downloadedFile = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.downloadAndDecryptFile(
                encryptedFileId = approvedItem.encryptedFileId,
                targetTempFile = tempDownloadFile
            ).getOrThrow()
        }
        assertTrue(downloadedFile.integrityVerified)
        assertEquals(apkBytes.size.toLong(), downloadedFile.byteSize)
        assertArrayEquals(apkBytes, tempDownloadFile.readBytes())
        tempDownloadFile.delete()
        Unit
    }

    @Test
    fun uploaderCanEditOwnUpload_resetsToPending_otherUserEditRejected(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CatalogRepository(firestore, auth)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.publishCatalogItem(
                title = "Alice Tool v1",
                description = "Initial release",
                category = "Tool",
                version = "1.0.0",
                targetUrl = "https://example.com/alice-v1",
                fileSizeLabel = "2.0 MB",
                iconBase64 = "aWNvbjE="
            ).getOrThrow()
        }

        // Admin approves it
        val adminUid = signInTestUser(ADMIN_EMAIL)
        seedEmulatorAdminDoc(adminUid, ADMIN_EMAIL)
        val adminRepo = CatalogRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.adminModerateCatalogItem(itemId, "approved").getOrThrow()
        }

        val approvedItem = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllCatalogForAdmin().first { list -> list.any { it.id == itemId } }
        }.first { it.id == itemId }

        // Bob tries to edit Alice's upload -> PERMISSION_DENIED
        signInTestUser(BOB_EMAIL)
        val bobRepo = CatalogRepository(firestore, auth)
        val bobEditResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.editCatalogItem(
                existingItem = approvedItem,
                title = "Bob Hijack",
                description = "Unauthorized edit",
                category = "Tool",
                version = "9.9.9",
                targetUrl = "https://example.com/evil",
                fileSizeLabel = "1 MB",
                iconBase64 = ""
            )
        }
        assertTrue(bobEditResult.isFailure)
        val bobEx = checkNotNull(bobEditResult.exceptionOrNull()?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got ${bobEditResult.exceptionOrNull()}"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, bobEx.code)

        // Alice edits her own upload (with new icon & replacement chunked file) -> succeeds & resets to pending
        signInTestUser(ALICE_EMAIL)
        val newPayload = ByteArray(2500) { 0x42 }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.editCatalogItem(
                existingItem = approvedItem,
                title = "Alice Tool v2",
                description = "Updated release with APK & new icon",
                whatsNew = "Fixed crash",
                category = "Tool",
                version = "2.0.0",
                targetUrl = "",
                fileSizeLabel = "2.4 KB",
                iconBase64 = "aWNvbjI=",
                newFileSource = UploadFileSource.InMemory(
                    fileName = "alice_v2.apk",
                    mimeType = "application/vnd.android.package-archive",
                    bytes = newPayload
                ),
                chunkSizeBytes = 1024
            ).getOrThrow()
        }

        val aliceEdited = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMyUploads().first { list ->
                list.any { it.id == itemId && it.title == "Alice Tool v2" }
            }
        }.first { it.id == itemId }

        assertEquals("pending", aliceEdited.status)
        assertEquals("aWNvbjI=", aliceEdited.iconBase64)
        assertEquals("2.0.0", aliceEdited.version)
        assertEquals("Fixed crash", aliceEdited.whatsNew)
    }

    @Test
    fun regularUser_cannotSelfApproveOrPublishAnnouncements(): Unit = runBlocking {
        signInTestUser(BOB_EMAIL)
        val itemRef = firestore.collection("catalog_items").document("self_approve_${UUID.randomUUID()}")
        val badMap = mapOf(
            "title" to "Self Approved Attempt",
            "description" to "Bypass attempt",
            "category" to "App",
            "itemType" to "link",
            "version" to "1.0.0",
            "fileSizeLabel" to "1 MB",
            "creatorHandle" to "@kadirofficial007",
            "encryptedTargetUrl" to "ZW5jcnlwdGVk",
            "urlIv" to "MTIzNDU2Nzg5MDEy",
            "encryptedFileId" to "",
            "sha256Checksum" to "a".repeat(64),
            "encryptionAlgorithm" to "AES-256-GCM",
            "status" to "approved",
            "downloadCount" to 0L,
            "averageRating" to 0.0,
            "reviewCount" to 0L,
            "uploaderUid" to auth.currentUser!!.uid,
            "uploaderName" to "Bob",
            "iconBase64" to "",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        var ex: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                itemRef.set(badMap).let { task ->
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

        // Regular user also cannot publish an announcement
        val bobRepo = CatalogRepository(firestore, auth)
        val annResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.publishAnnouncement("Unauthorized", "Should fail")
        }
        assertTrue(annResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            annResult.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun guestUser_canViewApprovedCatalog_butCannotDownloadDecryptOrWriteOrListUnfiltered(): Unit = runBlocking {
        // Seed one approved item and one announcement as admin
        val adminUid = signInTestUser(ADMIN_EMAIL)
        seedEmulatorAdminDoc(adminUid, ADMIN_EMAIL)
        val adminRepo = CatalogRepository(firestore, auth)
        val approvedId = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.publishCatalogItem(
                title = "Public Viewable App",
                description = "Visible to guests for preview",
                category = "App",
                version = "1.0.0",
                targetUrl = "https://example.com/public",
                fileSizeLabel = "1 MB",
                fileSource = UploadFileSource.InMemory(
                    fileName = "public.apk",
                    mimeType = "application/vnd.android.package-archive",
                    bytes = "apk_payload".toByteArray(StandardCharsets.UTF_8)
                ),
                publishAsApproved = true
            ).getOrThrow()
        }
        val approvedItem = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeApprovedCatalog().first { list -> list.any { it.id == approvedId } }
        }.first { it.id == approvedId }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.publishAnnouncement("Notice", "Welcome guests!").getOrThrow()
        }

        // Sign out -> Guest mode
        auth.signOut()
        val guestRepo = CatalogRepository(firestore, auth)

        // Guest CAN view approved catalog & active announcements
        val guestItems = withTimeout(FLOW_TIMEOUT_MS) {
            guestRepo.observeApprovedCatalog().first { list -> list.any { it.id == approvedId } }
        }
        assertTrue(guestItems.any { it.id == approvedId })
        val guestNotices = withTimeout(FLOW_TIMEOUT_MS) {
            guestRepo.observeActiveAnnouncements().first { it.isNotEmpty() }
        }
        assertTrue(guestNotices.isNotEmpty())

        // Guest CANNOT download encrypted files, record download, toggle bookmarks, or query unfiltered collection
        val downloadAttempt = guestRepo.downloadAndDecryptFile(approvedItem.encryptedFileId)
        assertTrue(downloadAttempt.isFailure)

        val bookmarkAttempt = guestRepo.toggleBookmark(approvedId, false)
        assertTrue(bookmarkAttempt.isFailure)

        var unfilteredEx: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("catalog_items").get().let { task ->
                    val latch = kotlinx.coroutines.CompletableDeferred<Unit>()
                    task.addOnSuccessListener { latch.complete(Unit) }
                    task.addOnFailureListener { e -> latch.completeExceptionally(e) }
                    latch.await()
                }
            }
        } catch (e: Exception) {
            unfilteredEx = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unfilteredEx?.code)

        var fileGetEx: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("encrypted_files").document(approvedItem.encryptedFileId).get().let { task ->
                    val latch = kotlinx.coroutines.CompletableDeferred<Unit>()
                    task.addOnSuccessListener { latch.complete(Unit) }
                    task.addOnFailureListener { e -> latch.completeExceptionally(e) }
                    latch.await()
                }
            }
        } catch (e: Exception) {
            fileGetEx = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fileGetEx?.code)
    }

    private companion object {
        const val ADMIN_EMAIL = "admin-test@kadirhub.test"
        const val ALICE_EMAIL = "alice@kadirhub.test"
        const val BOB_EMAIL = "bob@kadirhub.test"
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
