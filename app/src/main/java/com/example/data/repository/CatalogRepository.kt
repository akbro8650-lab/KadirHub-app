package com.example.data.repository

import com.example.data.crypto.CryptoVaultManager
import com.example.data.model.CatalogConstants
import com.example.data.model.CatalogItem
import com.example.data.model.EncryptedFile
import com.example.data.model.ModerationReport
import com.example.data.model.Review
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class DecryptedVaultFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val sha256Checksum: String,
    val integrityVerified: Boolean
)

class CatalogRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun requireDisplayName(): String {
        val user = auth.currentUser
        return user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Community Member"
    }

    suspend fun isServerAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.email == CatalogConstants.ADMIN_EMAIL && user.isEmailVerified) {
            return true
        }
        val path = "admins/${user.uid}"
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path, auth)
            false
        }
    }

    fun observeApprovedCatalog(): Flow<List<CatalogItem>> = flow {
        val uid = requireUserId()
        val path = "catalog_items"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", "approved")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CatalogItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeAllCatalogForAdmin(): Flow<List<CatalogItem>> = flow {
        val uid = requireUserId()
        val path = "catalog_items"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CatalogItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeReviewsForItem(itemId: String): Flow<List<Review>> = flow {
        val uid = requireUserId()
        val path = "reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("itemId", itemId)
                .whereEqualTo("moderationStatus", "visible")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Review::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeMyReviews(): Flow<List<Review>> = flow {
        val uid = requireUserId()
        val path = "reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Review::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeAllReviewsForAdmin(): Flow<List<Review>> = flow {
        val uid = requireUserId()
        val path = "reviews"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Review::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeReportsForAdmin(): Flow<List<ModerationReport>> = flow {
        val uid = requireUserId()
        val path = "moderation_reports"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ModerationReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun publishCatalogItem(
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        attachedFileName: String? = null,
        attachedMimeType: String? = null,
        attachedBytes: ByteArray? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val itemDocRef = db.collection("catalog_items").document()
        val itemId = itemDocRef.id
        val batch = db.batch()

        val hasFile = attachedBytes != null && attachedBytes.isNotEmpty() && !attachedFileName.isNullOrBlank()
        var encryptedFileId = ""
        val urlPayload = CryptoVaultManager.encryptString(targetUrl.trim())
        var itemChecksum = urlPayload.sha256Checksum

        if (hasFile) {
            val bytes = attachedBytes!!
            require(bytes.size <= CatalogConstants.MAX_DIRECT_FILE_BYTES) {
                "Encrypted vault file exceeds 500 KB direct storage limit; use an external link instead."
            }
            val fileDocRef = db.collection("encrypted_files").document()
            encryptedFileId = fileDocRef.id
            val filePayload = CryptoVaultManager.encryptBytes(bytes)
            itemChecksum = filePayload.sha256Checksum

            val fileMap = mapOf(
                "catalogItemId" to itemId,
                "fileName" to attachedFileName!!.take(255),
                "mimeType" to (attachedMimeType?.take(128) ?: "application/octet-stream"),
                "byteSize" to bytes.size.toLong(),
                "ciphertextBase64" to filePayload.ciphertextBase64,
                "ivBase64" to filePayload.ivBase64,
                "sha256Checksum" to filePayload.sha256Checksum,
                "encryptionAlgorithm" to CatalogConstants.ENCRYPTION_ALGO,
                "status" to "approved",
                "uploaderUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(fileDocRef, fileMap)
        }

        val itemMap = mapOf(
            "title" to title.trim().take(120),
            "description" to description.trim().take(2000),
            "category" to category,
            "itemType" to if (hasFile) "encrypted_file" else "link",
            "version" to version.trim().ifBlank { "1.0.0" }.take(40),
            "fileSizeLabel" to fileSizeLabel.trim().take(40),
            "creatorHandle" to CatalogConstants.CREATOR_TIKTOK_HANDLE,
            "encryptedTargetUrl" to urlPayload.ciphertextBase64,
            "urlIv" to urlPayload.ivBase64,
            "encryptedFileId" to encryptedFileId,
            "sha256Checksum" to itemChecksum,
            "encryptionAlgorithm" to CatalogConstants.ENCRYPTION_ALGO,
            "status" to "approved",
            "downloadCount" to 0L,
            "averageRating" to 0.0,
            "reviewCount" to 0L,
            "uploaderUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        batch.set(itemDocRef, itemMap)

        try {
            batch.commit().await()
            itemId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, itemDocRef.path, auth)
            throw e
        }
    }

    suspend fun downloadAndDecryptFile(encryptedFileId: String): Result<DecryptedVaultFile> = runCatching {
        requireUserId()
        val docRef = db.collection("encrypted_files").document(encryptedFileId)
        val snapshot = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }
        val file = snapshot.toObject(
            EncryptedFile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: throw IllegalStateException("Encrypted file record not found.")

        val plainBytes = CryptoVaultManager.decryptBytes(file.ciphertextBase64, file.ivBase64)
        val computedHash = CryptoVaultManager.sha256Hex(plainBytes)
        val verified = computedHash == file.sha256Checksum
        require(verified) { "File integrity verification failed (SHA-256 mismatch)." }

        DecryptedVaultFile(
            fileName = file.fileName,
            mimeType = file.mimeType,
            bytes = plainBytes,
            sha256Checksum = computedHash,
            integrityVerified = true
        )
    }

    suspend fun recordDownload(itemId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("catalog_items").document(itemId)
        try {
            docRef.update(
                mapOf(
                    "downloadCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun submitOrUpdateReview(
        itemId: String,
        itemTitle: String,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val clampedRating = rating.coerceIn(1, 5).toLong()
        val reviewId = "${itemId}_${uid}"
        val reviewRef = db.collection("reviews").document(reviewId)

        val existing = try {
            reviewRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, reviewRef.path, auth)
            throw e
        }

        try {
            if (!existing.exists()) {
                reviewRef.set(
                    mapOf(
                        "itemId" to itemId,
                        "itemTitle" to itemTitle.take(120),
                        "userId" to uid,
                        "authorName" to requireDisplayName().take(80),
                        "rating" to clampedRating,
                        "comment" to comment.trim().take(1000),
                        "moderationStatus" to "visible",
                        "flagReason" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                reviewRef.update(
                    mapOf(
                        "rating" to clampedRating,
                        "comment" to comment.trim().take(1000),
                        "authorName" to requireDisplayName().take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            recomputeItemRatingAggregate(itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reviewRef.path, auth)
            throw e
        }
    }

    private suspend fun recomputeItemRatingAggregate(itemId: String) {
        val visibleSnapshot = db.collection("reviews")
            .whereEqualTo("itemId", itemId)
            .whereEqualTo("moderationStatus", "visible")
            .get()
            .await()
        val reviews = visibleSnapshot.toObjects(Review::class.java)
        val count = reviews.size.toLong()
        val avg = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
        val itemRef = db.collection("catalog_items").document(itemId)
        itemRef.update(
            mapOf(
                "averageRating" to avg,
                "reviewCount" to count,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun flagReview(reviewId: String, reason: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("reviews").document(reviewId)
        try {
            docRef.update(
                mapOf(
                    "moderationStatus" to "flagged",
                    "flagReason" to reason.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun submitModerationReport(
        targetType: String,
        targetId: String,
        targetTitle: String,
        reason: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("moderation_reports").document()
        try {
            docRef.set(
                mapOf(
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "targetTitle" to targetTitle.take(120),
                    "reporterUid" to uid,
                    "reporterName" to requireDisplayName().take(80),
                    "reason" to reason.trim().take(400),
                    "status" to "open",
                    "adminNotes" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun adminModerateCatalogItem(
        itemId: String,
        newStatus: String,
        encryptedFileId: String = ""
    ): Result<Unit> = runCatching {
        requireUserId()
        val batch = db.batch()
        val itemRef = db.collection("catalog_items").document(itemId)
        batch.update(
            itemRef,
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (encryptedFileId.isNotBlank()) {
            val fileRef = db.collection("encrypted_files").document(encryptedFileId)
            batch.update(
                fileRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, itemRef.path, auth)
            throw e
        }
    }

    suspend fun adminDeleteCatalogItem(
        itemId: String,
        encryptedFileId: String = ""
    ): Result<Unit> = runCatching {
        requireUserId()
        val batch = db.batch()
        val itemRef = db.collection("catalog_items").document(itemId)
        batch.delete(itemRef)
        if (encryptedFileId.isNotBlank()) {
            val fileRef = db.collection("encrypted_files").document(encryptedFileId)
            batch.delete(fileRef)
        }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, itemRef.path, auth)
            throw e
        }
    }

    suspend fun adminModerateReview(
        review: Review,
        newModerationStatus: String,
        flagReason: String = review.flagReason
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("reviews").document(review.id)
        try {
            docRef.update(
                mapOf(
                    "moderationStatus" to newModerationStatus,
                    "flagReason" to flagReason.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recomputeItemRatingAggregate(review.itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun adminDeleteReview(review: Review): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("reviews").document(review.id)
        try {
            docRef.delete().await()
            recomputeItemRatingAggregate(review.itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    suspend fun adminResolveReport(
        reportId: String,
        newStatus: String,
        adminNotes: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("moderation_reports").document(reportId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "adminNotes" to adminNotes.trim().take(400),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }
}
