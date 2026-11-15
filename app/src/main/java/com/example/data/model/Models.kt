package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CatalogItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val whatsNew: String = "",
    val category: String = "App",
    val itemType: String = "link",
    val version: String = "1.0.0",
    val fileSizeLabel: String = "",
    val creatorHandle: String = "@kadirofficial007",
    val encryptedTargetUrl: String = "",
    val urlIv: String = "",
    val encryptedFileId: String = "",
    val sha256Checksum: String = "",
    val encryptionAlgorithm: String = "AES-256-GCM",
    val status: String = "pending",
    @field:PropertyName("isFeatured")
    @get:PropertyName("isFeatured")
    val isFeatured: Boolean = false,
    val moderationNote: String = "",
    val downloadCount: Long = 0L,
    val averageRating: Double = 0.0,
    val reviewCount: Long = 0L,
    val uploaderUid: String = "",
    val uploaderName: String = "",
    val iconBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EncryptedFile(
    @DocumentId val id: String = "",
    val catalogItemId: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val byteSize: Long = 0L,
    val chunkCount: Long = 0L,
    val ciphertextBase64: String = "",
    val ivBase64: String = "",
    val sha256Checksum: String = "",
    val encryptionAlgorithm: String = "AES-256-GCM",
    val status: String = "pending",
    val uploaderUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EncryptedFileChunk(
    @DocumentId val id: String = "",
    val fileId: String = "",
    val catalogItemId: String = "",
    val chunkIndex: Long = 0L,
    val byteSize: Long = 0L,
    val ciphertextBase64: String = "",
    val ivBase64: String = "",
    val status: String = "pending",
    val uploaderUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val itemId: String = "",
    val itemTitle: String = "",
    val userId: String = "",
    val authorName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val moderationStatus: String = "visible",
    val flagReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val targetType: String = "catalog_item",
    val targetId: String = "",
    val targetTitle: String = "",
    val reporterUid: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val status: String = "open",
    val adminNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val actionUrl: String = "",
    val active: Boolean = true,
    val authorEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookmarkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

object CatalogConstants {
    const val CREATOR_TIKTOK_HANDLE = "@kadirofficial007"
    const val CREATOR_TIKTOK_URL = "https://www.tiktok.com/@kadirofficial007"
    const val ADMIN_EMAIL = "kadir25.bd@gmail.com"
    const val ENCRYPTION_ALGO = "AES-256-GCM"
    const val CHUNK_RAW_BYTES = 450_000
    const val MAX_DIRECT_FILE_BYTES = 52_428_800L // 50 MB chunked AES-256-GCM limit
    const val MAX_ICON_BASE64_CHARS = 120_000
    val CATEGORIES = listOf("All", "App", "Link", "Tool", "Game", "Resource")
    val UPLOAD_CATEGORIES = listOf("App", "Link", "Tool", "Game", "Resource")
}
