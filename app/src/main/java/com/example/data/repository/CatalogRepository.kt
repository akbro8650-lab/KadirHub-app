package com.example.data.repository

import com.example.data.crypto.CryptoVaultManager
import com.example.data.model.CatalogConstants
import com.example.data.model.CatalogItem
import com.example.data.model.EncryptedFile
import com.example.data.model.EncryptedFileChunk
import com.example.data.model.ModerationReport
import com.example.data.model.Review
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

sealed interface UploadFileSource {
    val fileName: String
    val mimeType: String
    val byteSize: Long

    data class InMemory(
        override val fileName: String,
        override val mimeType: String,
        val bytes: ByteArray
    ) : UploadFileSource {
        override val byteSize: Long get() = bytes.size.toLong()
    }

    data class Streamed(
        override val fileName: String,
        override val mimeType: String,
        override val byteSize: Long,
        val openStream: () -> InputStream
    ) : UploadFileSource
}

data class DecryptedVaultFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray?,
    val tempFile: File?,
    val byteSize: Long,
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

    fun observeMyUploads(): Flow<List<CatalogItem>> = flow {
        val uid = requireUserId()
        val path = "catalog_items"
        emitAll(
            db.collection(path)
                .whereEqualTo("uploaderUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CatalogItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
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
        iconBase64: String = "",
        fileSource: UploadFileSource? = null,
        publishAsApproved: Boolean = false,
        chunkSizeBytes: Int = CatalogConstants.CHUNK_RAW_BYTES,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val uploaderName = requireDisplayName().take(80)
        val itemDocRef = db.collection("catalog_items").document()
        val itemId = itemDocRef.id
        val status = if (publishAsApproved && isServerAdmin()) "approved" else "pending"

        val urlPayload = CryptoVaultManager.encryptString(targetUrl.trim())
        var encryptedFileId = ""
        var itemChecksum = urlPayload.sha256Checksum

        if (fileSource != null) {
            val uploaded = uploadEncryptedFileSource(
                fileSource = fileSource,
                catalogItemId = itemId,
                uid = uid,
                status = status,
                chunkSizeBytes = chunkSizeBytes,
                onProgress = onProgress
            )
            encryptedFileId = uploaded.first
            itemChecksum = uploaded.second
        }

        val itemMap = mapOf(
            "title" to title.trim().take(120),
            "description" to description.trim().take(2000),
            "category" to category,
            "itemType" to if (fileSource != null) "encrypted_file" else "link",
            "version" to version.trim().ifBlank { "1.0.0" }.take(40),
            "fileSizeLabel" to fileSizeLabel.trim().take(40),
            "creatorHandle" to CatalogConstants.CREATOR_TIKTOK_HANDLE,
            "encryptedTargetUrl" to urlPayload.ciphertextBase64,
            "urlIv" to urlPayload.ivBase64,
            "encryptedFileId" to encryptedFileId,
            "sha256Checksum" to itemChecksum,
            "encryptionAlgorithm" to CatalogConstants.ENCRYPTION_ALGO,
            "status" to status,
            "downloadCount" to 0L,
            "averageRating" to 0.0,
            "reviewCount" to 0L,
            "uploaderUid" to uid,
            "uploaderName" to uploaderName,
            "iconBase64" to iconBase64.take(CatalogConstants.MAX_ICON_BASE64_CHARS),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            itemDocRef.set(itemMap).await()
            onProgress(1f)
            itemId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (encryptedFileId.isNotBlank()) {
                runCatching { deleteEncryptedFileAndChunks(encryptedFileId) }
            }
            handleFirestoreError(e, OperationType.CREATE, itemDocRef.path, auth)
            throw e
        }
    }

    suspend fun editCatalogItem(
        existingItem: CatalogItem,
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        iconBase64: String,
        newFileSource: UploadFileSource? = null,
        removeAttachedFile: Boolean = false,
        preserveStatusIfAdmin: Boolean = false,
        chunkSizeBytes: Int = CatalogConstants.CHUNK_RAW_BYTES,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val isAdmin = isServerAdmin()
        val newStatus = if (preserveStatusIfAdmin && isAdmin) {
            existingItem.status.ifBlank { "approved" }
        } else {
            "pending"
        }

        val urlPayload = CryptoVaultManager.encryptString(targetUrl.trim())
        var finalFileId = existingItem.encryptedFileId
        var finalItemType = existingItem.itemType
        var finalChecksum = urlPayload.sha256Checksum
        var fileIdToDeleteLater = ""

        if (newFileSource != null) {
            val uploaded = uploadEncryptedFileSource(
                fileSource = newFileSource,
                catalogItemId = existingItem.id,
                uid = uid,
                status = newStatus,
                chunkSizeBytes = chunkSizeBytes,
                onProgress = onProgress
            )
            fileIdToDeleteLater = existingItem.encryptedFileId
            finalFileId = uploaded.first
            finalChecksum = uploaded.second
            finalItemType = "encrypted_file"
        } else if (removeAttachedFile) {
            fileIdToDeleteLater = existingItem.encryptedFileId
            finalFileId = ""
            finalItemType = "link"
            finalChecksum = urlPayload.sha256Checksum
        } else if (existingItem.encryptedFileId.isNotBlank()) {
            finalChecksum = existingItem.sha256Checksum.ifBlank { urlPayload.sha256Checksum }
            if (existingItem.status != newStatus) {
                updateEncryptedFileAndChunksStatus(existingItem.encryptedFileId, newStatus)
            }
        }

        val itemDocRef = db.collection("catalog_items").document(existingItem.id)
        val updates = mapOf(
            "title" to title.trim().take(120),
            "description" to description.trim().take(2000),
            "category" to category,
            "itemType" to finalItemType,
            "version" to version.trim().ifBlank { "1.0.0" }.take(40),
            "fileSizeLabel" to fileSizeLabel.trim().take(40),
            "encryptedTargetUrl" to urlPayload.ciphertextBase64,
            "urlIv" to urlPayload.ivBase64,
            "encryptedFileId" to finalFileId,
            "sha256Checksum" to finalChecksum,
            "encryptionAlgorithm" to CatalogConstants.ENCRYPTION_ALGO,
            "uploaderName" to requireDisplayName().take(80),
            "iconBase64" to iconBase64.take(CatalogConstants.MAX_ICON_BASE64_CHARS),
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            itemDocRef.update(updates).await()
            if (fileIdToDeleteLater.isNotBlank() && fileIdToDeleteLater != finalFileId) {
                runCatching { deleteEncryptedFileAndChunks(fileIdToDeleteLater) }
            }
            onProgress(1f)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (newFileSource != null && finalFileId.isNotBlank() && finalFileId != existingItem.encryptedFileId) {
                runCatching { deleteEncryptedFileAndChunks(finalFileId) }
            }
            handleFirestoreError(e, OperationType.UPDATE, itemDocRef.path, auth)
            throw e
        }
    }

    private suspend fun uploadEncryptedFileSource(
        fileSource: UploadFileSource,
        catalogItemId: String,
        uid: String,
        status: String,
        chunkSizeBytes: Int,
        onProgress: (Float) -> Unit
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val totalSize = fileSource.byteSize
        require(totalSize > 0L) { "Selected file is empty." }
        require(totalSize <= CatalogConstants.MAX_DIRECT_FILE_BYTES) {
            "File exceeds 50 MB direct vault limit (${totalSize / (1024 * 1024)} MB selected)."
        }

        val normalizedMime = if (fileSource.fileName.endsWith(".apk", ignoreCase = true) &&
            (fileSource.mimeType.isBlank() || fileSource.mimeType == "application/octet-stream")
        ) {
            "application/vnd.android.package-archive"
        } else {
            fileSource.mimeType.ifBlank { "application/octet-stream" }
        }.take(128)

        val fileDocRef = db.collection("encrypted_files").document()
        val fileId = fileDocRef.id

        if (totalSize <= chunkSizeBytes) {
            val bytes = when (fileSource) {
                is UploadFileSource.InMemory -> fileSource.bytes
                is UploadFileSource.Streamed -> fileSource.openStream().use { it.readBytes() }
            }
            val payload = CryptoVaultManager.encryptBytes(bytes)
            val fileMap = mapOf(
                "catalogItemId" to catalogItemId,
                "fileName" to fileSource.fileName.take(255),
                "mimeType" to normalizedMime,
                "byteSize" to bytes.size.toLong(),
                "chunkCount" to 0L,
                "ciphertextBase64" to payload.ciphertextBase64,
                "ivBase64" to payload.ivBase64,
                "sha256Checksum" to payload.sha256Checksum,
                "encryptionAlgorithm" to CatalogConstants.ENCRYPTION_ALGO,
                "status" to status,
                "uploaderUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                fileDocRef.set(fileMap).await()
                onProgress(0.95f)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, fileDocRef.path, auth)
                throw e
            }
            return@withContext fileId to payload.sha256Checksum
        }

        // Streamed chunked upload for APKs / files > chunkSizeBytes (up to 50 MB)
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream: InputStream = when (fileSource) {
            is UploadFileSource.InMemory -> ByteArrayInputStream(fileSource.bytes)
            is UploadFileSource.Streamed -> fileSource.openStream()
        }

        var chunkIndex = 0L
        var uploadedBytes = 0L
        val buffer = ByteArray(chunkSizeBytes)
        var pendingBatch = db.batch()
        var pendingBatchOps = 0

        try {
            inputStream.use { stream ->
                while (true) {
                    var filled = 0
                    while (filled < chunkSizeBytes) {
                        val read = stream.read(buffer, filled, chunkSizeBytes - filled)
                        if (read == -1) break
                        filled += read
                    }
                    if (filled <= 0) break

                    val chunkBytes = if (filled == buffer.size) buffer else buffer.copyOf(filled)
                    digest.update(chunkBytes)
                    val encryptedChunk = CryptoVaultManager.encryptBytes(chunkBytes)

                    val chunkDocRef = fileDocRef.collection("chunks").document("chunk_$chunkIndex")
                    val chunkMap = mapOf(
                        "fileId" to fileId,
                        "catalogItemId" to catalogItemId,
                        "chunkIndex" to chunkIndex,
                        "byteSize" to filled.toLong(),
                        "ciphertextBase64" to encryptedChunk.ciphertextBase64,
                        "ivBase64" to encryptedChunk.ivBase64,
                        "status" to status,
                        "uploaderUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    pendingBatch.set(chunkDocRef, chunkMap)
                    pendingBatchOps++
                    chunkIndex++
                    uploadedBytes += filled

                    if (pendingBatchOps >= CHUNKS_PER_BATCH) {
                        pendingBatch.commit().await()
                        pendingBatch = db.batch()
                        pendingBatchOps = 0
                        onProgress((uploadedBytes.toFloat() / totalSize.toFloat()).coerceIn(0.05f, 0.92f))
                    }
                }
            }

            if (pendingBatchOps > 0) {
                pendingBatch.commit().await()
            }

            val fullSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            val manifestMap = mapOf(
                "catalogItemId" to catalogItemId,
                "fileName" to fileSource.fileName.take(255),
                "mimeType" to normalizedMime,
                "byteSize" to uploadedBytes,
                "chunkCount" to chunkIndex,
                "ciphertextBase64" to "",
                "ivBase64" to "",
                "sha256Checksum" to fullSha256,
                "encryptionAlgorithm" to CatalogConstants.ENCRYPTION_ALGO,
                "status" to status,
                "uploaderUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            fileDocRef.set(manifestMap).await()
            onProgress(0.96f)
            return@withContext fileId to fullSha256
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { deleteChunksRange(fileDocRef, chunkIndex) }
            handleFirestoreError(e, OperationType.CREATE, fileDocRef.path, auth)
            throw e
        }
    }

    suspend fun downloadAndDecryptFile(
        encryptedFileId: String,
        targetTempFile: File? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<DecryptedVaultFile> = runCatching {
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

        if (file.chunkCount <= 0L) {
            val plainBytes = CryptoVaultManager.decryptBytes(file.ciphertextBase64, file.ivBase64)
            val computedHash = CryptoVaultManager.sha256Hex(plainBytes)
            require(computedHash == file.sha256Checksum) {
                "File integrity verification failed (SHA-256 mismatch)."
            }
            if (targetTempFile != null) {
                withContext(Dispatchers.IO) {
                    targetTempFile.parentFile?.mkdirs()
                    targetTempFile.writeBytes(plainBytes)
                }
            }
            onProgress(1f)
            return@runCatching DecryptedVaultFile(
                fileName = file.fileName,
                mimeType = file.mimeType,
                bytes = if (targetTempFile == null) plainBytes else null,
                tempFile = targetTempFile,
                byteSize = plainBytes.size.toLong(),
                sha256Checksum = computedHash,
                integrityVerified = true
            )
        }

        // Chunked download & streaming AES-256-GCM decryption + SHA-256 verification
        withContext(Dispatchers.IO) {
            val totalChunks = file.chunkCount
            val digest = MessageDigest.getInstance("SHA-256")
            val byteOut = if (targetTempFile == null) ByteArrayOutputStream() else null
            val fileOut = if (targetTempFile != null) {
                targetTempFile.parentFile?.mkdirs()
                FileOutputStream(targetTempFile)
            } else {
                null
            }

            var totalWritten = 0L
            try {
                var idx = 0L
                while (idx < totalChunks) {
                    val windowEnd = minOf(idx + DOWNLOAD_CONCURRENCY, totalChunks)
                    val snapshots = coroutineScope {
                        (idx until windowEnd).map { cIndex ->
                            async {
                                docRef.collection("chunks").document("chunk_$cIndex").get().await()
                            }
                        }.awaitAll()
                    }

                    for (chunkSnap in snapshots) {
                        val chunk = chunkSnap.toObject(
                            EncryptedFileChunk::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: throw IllegalStateException("Encrypted chunk missing: ${chunkSnap.id}")
                        val plainChunk = CryptoVaultManager.decryptBytes(
                            chunk.ciphertextBase64,
                            chunk.ivBase64
                        )
                        digest.update(plainChunk)
                        fileOut?.write(plainChunk)
                        byteOut?.write(plainChunk)
                        totalWritten += plainChunk.size
                    }

                    idx = windowEnd
                    onProgress((idx.toFloat() / totalChunks.toFloat()).coerceIn(0.05f, 0.98f))
                }
                fileOut?.flush()
            } catch (e: Exception) {
                runCatching { fileOut?.close() }
                runCatching { targetTempFile?.delete() }
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.GET, "${docRef.path}/chunks", auth)
                throw e
            } finally {
                runCatching { fileOut?.close() }
            }

            val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (computedHash != file.sha256Checksum) {
                runCatching { targetTempFile?.delete() }
                throw IllegalArgumentException("File integrity verification failed (SHA-256 mismatch).")
            }
            onProgress(1f)

            DecryptedVaultFile(
                fileName = file.fileName,
                mimeType = file.mimeType,
                bytes = byteOut?.toByteArray(),
                tempFile = targetTempFile,
                byteSize = totalWritten,
                sha256Checksum = computedHash,
                integrityVerified = true
            )
        }
    }

    private suspend fun updateEncryptedFileAndChunksStatus(
        encryptedFileId: String,
        newStatus: String
    ) {
        val fileRef = db.collection("encrypted_files").document(encryptedFileId)
        val fileSnap = fileRef.get().await()
        if (!fileSnap.exists()) return
        val chunkCount = (fileSnap.getLong("chunkCount") ?: 0L)

        var batch = db.batch()
        var ops = 0
        batch.update(
            fileRef,
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        ops++

        for (i in 0 until chunkCount) {
            val chunkRef = fileRef.collection("chunks").document("chunk_$i")
            batch.update(
                chunkRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            ops++
            if (ops >= MAX_OPS_PER_METADATA_BATCH) {
                batch.commit().await()
                batch = db.batch()
                ops = 0
            }
        }
        if (ops > 0) {
            batch.commit().await()
        }
    }

    private suspend fun deleteEncryptedFileAndChunks(encryptedFileId: String) {
        val fileRef = db.collection("encrypted_files").document(encryptedFileId)
        val fileSnap = fileRef.get().await()
        if (!fileSnap.exists()) return
        val chunkCount = (fileSnap.getLong("chunkCount") ?: 0L)
        deleteChunksRange(fileRef, chunkCount)
        fileRef.delete().await()
    }

    private suspend fun deleteChunksRange(fileRef: DocumentReference, chunkCount: Long) {
        if (chunkCount <= 0L) return
        var batch = db.batch()
        var ops = 0
        for (i in 0 until chunkCount) {
            batch.delete(fileRef.collection("chunks").document("chunk_$i"))
            ops++
            if (ops >= MAX_OPS_PER_METADATA_BATCH) {
                batch.commit().await()
                batch = db.batch()
                ops = 0
            }
        }
        if (ops > 0) {
            batch.commit().await()
        }
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
        val itemRef = db.collection("catalog_items").document(itemId)
        try {
            if (encryptedFileId.isNotBlank()) {
                updateEncryptedFileAndChunksStatus(encryptedFileId, newStatus)
            }
            itemRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, itemRef.path, auth)
            throw e
        }
    }

    suspend fun deleteCatalogItem(
        itemId: String,
        encryptedFileId: String = ""
    ): Result<Unit> = runCatching {
        requireUserId()
        val itemRef = db.collection("catalog_items").document(itemId)
        try {
            if (encryptedFileId.isNotBlank()) {
                deleteEncryptedFileAndChunks(encryptedFileId)
            }
            itemRef.delete().await()
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

    private companion object {
        const val CHUNKS_PER_BATCH = 4
        const val DOWNLOAD_CONCURRENCY = 3
        const val MAX_OPS_PER_METADATA_BATCH = 350
    }
}
