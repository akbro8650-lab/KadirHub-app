package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.crypto.CryptoVaultManager
import com.example.data.model.Announcement
import com.example.data.model.CatalogConstants
import com.example.data.model.CatalogItem
import com.example.data.model.ModerationReport
import com.example.data.model.Review
import com.example.data.repository.CatalogRepository
import com.example.data.repository.DecryptedVaultFile
import com.example.data.repository.UploadFileSource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SortOption(val label: String) {
    NEWEST("Newest"),
    TOP_RATED("Top Rated"),
    MOST_DOWNLOADED("Downloads"),
    NAME_AZ("A–Z")
}

enum class TypeFilter(val label: String) {
    ALL("All"),
    APK("Vault APK"),
    LINK("Links"),
    SAVED("Saved")
}

sealed interface DownloadActionResult {
    data class OpenUrl(val url: String, val itemTitle: String) : DownloadActionResult
    data class FileDecrypted(val file: DecryptedVaultFile, val fallbackUrl: String) : DownloadActionResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val repository: CatalogRepository,
    val currentUserId: String?,
    val currentUserEmail: String? = null,
    val isEmailVerified: Boolean = false
) : ViewModel() {

    val isSignedIn: Boolean get() = !currentUserId.isNullOrBlank()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _typeFilter = MutableStateFlow(TypeFilter.ALL)
    val typeFilter: StateFlow<TypeFilter> = _typeFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId.asStateFlow()

    private val _editingItem = MutableStateFlow<CatalogItem?>(null)
    val editingItem: StateFlow<CatalogItem?> = _editingItem.asStateFlow()

    private val _isServerAdmin = MutableStateFlow(false)
    val isServerAdmin: StateFlow<Boolean> = _isServerAdmin.asStateFlow()

    // Admin access is strictly for verified kadir25.bd@gmail.com or trusted server /admins/{uid}
    val isAdminAccess: StateFlow<Boolean> = _isServerAdmin.map { serverAdmin ->
        isSignedIn && (
            (currentUserEmail.equals(CatalogConstants.ADMIN_EMAIL, ignoreCase = true) && isEmailVerified) ||
                serverAdmin
            )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = isSignedIn &&
            currentUserEmail.equals(CatalogConstants.ADMIN_EMAIL, ignoreCase = true) &&
            isEmailVerified
    )

    private val _signInPromptReason = MutableStateFlow<String?>(null)
    val signInPromptReason: StateFlow<String?> = _signInPromptReason.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _transferProgress = MutableStateFlow<Float?>(null)
    val transferProgress: StateFlow<Float?> = _transferProgress.asStateFlow()

    private val _pendingDownloadResult = MutableStateFlow<DownloadActionResult?>(null)
    val pendingDownloadResult: StateFlow<DownloadActionResult?> = _pendingDownloadResult.asStateFlow()

    val rawCatalogState: StateFlow<UiState<List<CatalogItem>>> = repository.observeApprovedCatalog()
        .map<List<CatalogItem>, UiState<List<CatalogItem>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            if (root == null && error is CancellationException) throw error
            val displayError = root ?: error
            Log.w(TAG, "Error observing catalog", displayError)
            emit(UiState.Error(displayError.message ?: "Unable to load catalog."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeAnnouncementsState: StateFlow<List<Announcement>> = repository.observeActiveAnnouncements()
        .catch { error ->
            if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val bookmarkedIds: StateFlow<Set<String>> = if (!isSignedIn) {
        MutableStateFlow(emptySet())
    } else {
        repository.observeBookmarkedIds()
            .catch { error ->
                if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )
    }

    val myUploadsState: StateFlow<UiState<List<CatalogItem>>> = if (!isSignedIn) {
        MutableStateFlow(UiState.Success(emptyList()))
    } else {
        repository.observeMyUploads()
            .map<List<CatalogItem>, UiState<List<CatalogItem>>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                val displayError = root ?: error
                Log.w(TAG, "Error observing my uploads", displayError)
                emit(UiState.Error(displayError.message ?: "Unable to load your uploads."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    val featuredItems: StateFlow<List<CatalogItem>> = rawCatalogState
        .map { state ->
            (state as? UiState.Success)?.data?.filter { it.isFeatured } ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val filteredCatalogState: StateFlow<UiState<List<CatalogItem>>> = combine(
        rawCatalogState,
        bookmarkedIds,
        _searchQuery,
        _selectedCategory,
        combine(_typeFilter, _sortOption) { t, s -> t to s }
    ) { state, bookmarks, query, category, typeAndSort ->
        val (typeFilter, sort) = typeAndSort
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val q = query.trim().lowercase()
                val filtered = state.data.filter { item ->
                    val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)
                    val matchesType = when (typeFilter) {
                        TypeFilter.ALL -> true
                        TypeFilter.APK -> item.itemType == "encrypted_file"
                        TypeFilter.LINK -> item.itemType == "link"
                        TypeFilter.SAVED -> bookmarks.contains(item.id)
                    }
                    val matchesQuery = q.isEmpty() ||
                        item.title.lowercase().contains(q) ||
                        item.description.lowercase().contains(q) ||
                        item.whatsNew.lowercase().contains(q) ||
                        item.category.lowercase().contains(q) ||
                        item.uploaderName.lowercase().contains(q)
                    matchesCategory && matchesType && matchesQuery
                }
                val sorted = when (sort) {
                    SortOption.NEWEST -> filtered.sortedWith(
                        compareByDescending<CatalogItem> { it.isFeatured }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                    SortOption.TOP_RATED -> filtered.sortedByDescending { it.averageRating }
                    SortOption.MOST_DOWNLOADED -> filtered.sortedByDescending { it.downloadCount }
                    SortOption.NAME_AZ -> filtered.sortedBy { it.title.lowercase() }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val adminCatalogState: StateFlow<UiState<List<CatalogItem>>> = isAdminAccess
        .flatMapLatest { active ->
            if (!active) flowOf(UiState.Success(emptyList()))
            else repository.observeAllCatalogForAdmin()
                .map<List<CatalogItem>, UiState<List<CatalogItem>>> { UiState.Success(it) }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null && error is CancellationException) throw error
                    emit(UiState.Error((root ?: error).message ?: "Failed to load admin catalog"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val pendingApprovalCount: StateFlow<Int> = adminCatalogState
        .map { state ->
            (state as? UiState.Success)?.data?.count { it.status == "pending" } ?: 0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    val selectedItem: StateFlow<CatalogItem?> = combine(
        rawCatalogState,
        myUploadsState,
        adminCatalogState,
        _selectedItemId
    ) { approvedState, myState, adminState, id ->
        if (id == null) return@combine null
        (approvedState as? UiState.Success)?.data?.firstOrNull { it.id == id }
            ?: (myState as? UiState.Success)?.data?.firstOrNull { it.id == id }
            ?: (adminState as? UiState.Success)?.data?.firstOrNull { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    val itemReviewsState: StateFlow<UiState<List<Review>>> = _selectedItemId
        .flatMapLatest { itemId ->
            if (itemId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeReviewsForItem(itemId)
                    .map<List<Review>, UiState<List<Review>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error)
                        if (root == null && error is CancellationException) throw error
                        emit(UiState.Error((root ?: error).message ?: "Failed to load reviews"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val adminReviewsState: StateFlow<UiState<List<Review>>> = isAdminAccess
        .flatMapLatest { active ->
            if (!active) flowOf(UiState.Success(emptyList()))
            else repository.observeAllReviewsForAdmin()
                .map<List<Review>, UiState<List<Review>>> { UiState.Success(it) }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null && error is CancellationException) throw error
                    emit(UiState.Error((root ?: error).message ?: "Failed to load moderation reviews"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val adminReportsState: StateFlow<UiState<List<ModerationReport>>> = isAdminAccess
        .flatMapLatest { active ->
            if (!active) flowOf(UiState.Success(emptyList()))
            else repository.observeReportsForAdmin()
                .map<List<ModerationReport>, UiState<List<ModerationReport>>> { UiState.Success(it) }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null && error is CancellationException) throw error
                    emit(UiState.Error((root ?: error).message ?: "Failed to load moderation reports"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val adminAnnouncementsState: StateFlow<UiState<List<Announcement>>> = isAdminAccess
        .flatMapLatest { active ->
            if (!active) flowOf(UiState.Success(emptyList()))
            else repository.observeAllAnnouncementsForAdmin()
                .map<List<Announcement>, UiState<List<Announcement>>> { UiState.Success(it) }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null && error is CancellationException) throw error
                    emit(UiState.Error((root ?: error).message ?: "Failed to load announcements"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        if (isSignedIn) {
            refreshAdminStatus()
        }
    }

    fun refreshAdminStatus() {
        if (!isSignedIn) {
            _isServerAdmin.value = false
            return
        }
        viewModelScope.launch {
            _isServerAdmin.value = repository.isServerAdmin()
        }
    }

    fun requireAuth(reason: String): Boolean {
        if (isSignedIn) return true
        _signInPromptReason.value = reason
        return false
    }

    fun requestSignIn(reason: String) {
        _signInPromptReason.value = reason
    }

    fun dismissSignInPrompt() {
        _signInPromptReason.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectTypeFilter(type: TypeFilter) {
        if (type == TypeFilter.SAVED && !requireAuth("Sign in with Google to view and manage your saved bookmarks.")) {
            return
        }
        _typeFilter.value = type
    }

    fun selectSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun openItemDetail(itemId: String) {
        _selectedItemId.value = itemId
    }

    fun closeItemDetail() {
        _selectedItemId.value = null
    }

    fun startEditing(item: CatalogItem) {
        if (!requireAuth("Sign in with Google to edit uploads.")) return
        _editingItem.value = item
    }

    fun stopEditing() {
        _editingItem.value = null
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun showMessage(message: String) {
        _actionMessage.value = message
    }

    fun clearPendingDownload() {
        _pendingDownloadResult.value = null
    }

    fun decryptItemUrl(item: CatalogItem): String {
        return try {
            if (item.encryptedTargetUrl.isNotBlank() && item.urlIv.isNotBlank()) {
                CryptoVaultManager.decryptString(item.encryptedTargetUrl, item.urlIv)
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun toggleBookmark(item: CatalogItem) {
        if (!requireAuth("Sign in with Google to save releases to your bookmarks.")) return
        val currentlyBookmarked = bookmarkedIds.value.contains(item.id)
        viewModelScope.launch {
            repository.toggleBookmark(item.id, currentlyBookmarked)
                .onSuccess { saved ->
                    _actionMessage.value = if (saved) {
                        "Saved \"${item.title}\" to bookmarks."
                    } else {
                        "Removed \"${item.title}\" from bookmarks."
                    }
                }
                .onFailure {
                    _actionMessage.value = it.message ?: "Unable to update bookmark."
                }
        }
    }

    fun downloadOrOpenItem(item: CatalogItem, cacheDir: File? = null) {
        if (!requireAuth("Sign in with Google to decrypt APKs and open verified download links.")) return
        viewModelScope.launch {
            _isBusy.value = true
            _transferProgress.value = if (item.itemType == "encrypted_file") 0.02f else null
            try {
                val decryptedUrl = decryptItemUrl(item)
                if (item.itemType == "encrypted_file" && item.encryptedFileId.isNotBlank()) {
                    val tempTarget = cacheDir?.let { dir ->
                        File(File(dir, "vault_downloads").apply { mkdirs() }, "tmp_${item.id}.bin")
                    }
                    val fileResult = repository.downloadAndDecryptFile(
                        encryptedFileId = item.encryptedFileId,
                        targetTempFile = tempTarget,
                        onProgress = { progress -> _transferProgress.value = progress }
                    )
                    fileResult.onSuccess { decryptedFile ->
                        if (item.status == "approved") {
                            runCatching { repository.recordDownload(item.id) }
                        }
                        _pendingDownloadResult.value = DownloadActionResult.FileDecrypted(
                            file = decryptedFile,
                            fallbackUrl = decryptedUrl
                        )
                    }.onFailure { err ->
                        _actionMessage.value = err.message ?: "Failed to decrypt file."
                    }
                } else if (decryptedUrl.isNotBlank()) {
                    if (item.status == "approved") {
                        runCatching { repository.recordDownload(item.id) }
                    }
                    _pendingDownloadResult.value = DownloadActionResult.OpenUrl(
                        url = decryptedUrl,
                        itemTitle = item.title
                    )
                } else {
                    _actionMessage.value = "No downloadable link or file attached."
                }
            } finally {
                _isBusy.value = false
                _transferProgress.value = null
            }
        }
    }

    fun submitReview(item: CatalogItem, rating: Int, comment: String) {
        if (!requireAuth("Sign in with Google to rate and review releases.")) return
        if (comment.isBlank()) {
            _actionMessage.value = "Please enter a brief review comment."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitOrUpdateReview(item.id, item.title, rating, comment)
                .onSuccess { _actionMessage.value = "Review published!" }
                .onFailure { _actionMessage.value = it.message ?: "Failed to publish review." }
            _isBusy.value = false
        }
    }

    fun flagReview(review: Review, reason: String) {
        if (!requireAuth("Sign in with Google to flag reviews for moderation.")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.flagReview(review.id, reason.ifBlank { "Flagged by community" })
                .onSuccess {
                    repository.submitModerationReport(
                        targetType = "review",
                        targetId = review.id,
                        targetTitle = "Review on ${review.itemTitle}",
                        reason = reason.ifBlank { "Flagged for moderator review" }
                    )
                    _actionMessage.value = "Review flagged for moderation."
                }
                .onFailure { _actionMessage.value = it.message ?: "Unable to flag review." }
            _isBusy.value = false
        }
    }

    fun reportCatalogItem(item: CatalogItem, reason: String) {
        if (!requireAuth("Sign in with Google to submit a content moderation report.")) return
        if (reason.trim().length < 3) {
            _actionMessage.value = "Please describe the reason (at least 3 characters)."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitModerationReport(
                targetType = "catalog_item",
                targetId = item.id,
                targetTitle = item.title,
                reason = reason
            )
                .onSuccess { _actionMessage.value = "Report submitted to moderation queue." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to submit report." }
            _isBusy.value = false
        }
    }

    fun publishCatalogItem(
        title: String,
        description: String,
        whatsNew: String = "",
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        iconBase64: String,
        fileSource: UploadFileSource?,
        publishAsApproved: Boolean,
        isFeatured: Boolean = false,
        onPublished: () -> Unit
    ) {
        if (!requireAuth("Sign in with Google to upload APKs or links to KadirHub.")) return
        viewModelScope.launch {
            _isBusy.value = true
            _transferProgress.value = if (fileSource != null) 0.02f else null
            repository.publishCatalogItem(
                title = title,
                description = description,
                whatsNew = whatsNew,
                category = category,
                version = version,
                targetUrl = targetUrl,
                fileSizeLabel = fileSizeLabel,
                iconBase64 = iconBase64,
                fileSource = fileSource,
                publishAsApproved = publishAsApproved,
                isFeatured = isFeatured,
                onProgress = { progress -> _transferProgress.value = progress }
            )
                .onSuccess {
                    _actionMessage.value = if (publishAsApproved && isAdminAccess.value) {
                        "Encrypted & published live to KadirHub!"
                    } else {
                        "Encrypted & uploaded! Waiting for admin approval."
                    }
                    onPublished()
                }
                .onFailure {
                    _actionMessage.value = it.message ?: "Upload failed."
                }
            _transferProgress.value = null
            _isBusy.value = false
        }
    }

    fun editCatalogItem(
        existingItem: CatalogItem,
        title: String,
        description: String,
        whatsNew: String = "",
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        iconBase64: String,
        newFileSource: UploadFileSource?,
        removeAttachedFile: Boolean,
        preserveStatusIfAdmin: Boolean,
        isFeatured: Boolean = existingItem.isFeatured,
        onEdited: () -> Unit
    ) {
        if (!requireAuth("Sign in with Google to edit this upload.")) return
        viewModelScope.launch {
            _isBusy.value = true
            _transferProgress.value = if (newFileSource != null) 0.02f else null
            repository.editCatalogItem(
                existingItem = existingItem,
                title = title,
                description = description,
                whatsNew = whatsNew,
                category = category,
                version = version,
                targetUrl = targetUrl,
                fileSizeLabel = fileSizeLabel,
                iconBase64 = iconBase64,
                newFileSource = newFileSource,
                removeAttachedFile = removeAttachedFile,
                preserveStatusIfAdmin = preserveStatusIfAdmin,
                isFeatured = isFeatured,
                onProgress = { progress -> _transferProgress.value = progress }
            )
                .onSuccess {
                    _editingItem.value = null
                    _actionMessage.value = if (preserveStatusIfAdmin && isAdminAccess.value) {
                        "Release updated."
                    } else {
                        "Changes saved & sent for admin approval."
                    }
                    onEdited()
                }
                .onFailure {
                    _actionMessage.value = it.message ?: "Failed to update upload."
                }
            _transferProgress.value = null
            _isBusy.value = false
        }
    }

    fun deleteItem(item: CatalogItem) {
        if (!requireAuth("Sign in with Google to delete an upload.")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.deleteCatalogItem(item.id, item.encryptedFileId)
                .onSuccess {
                    if (_selectedItemId.value == item.id) {
                        _selectedItemId.value = null
                    }
                    if (_editingItem.value?.id == item.id) {
                        _editingItem.value = null
                    }
                    _actionMessage.value = "${item.title} deleted."
                }
                .onFailure { _actionMessage.value = it.message ?: "Failed to delete item." }
            _isBusy.value = false
        }
    }

    fun adminSetItemStatus(item: CatalogItem, newStatus: String, moderationNote: String? = null) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminModerateCatalogItem(item.id, newStatus, item.encryptedFileId, moderationNote)
                .onSuccess { _actionMessage.value = "${item.title} marked as $newStatus." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to update item status." }
            _isBusy.value = false
        }
    }

    fun adminSetFeatured(item: CatalogItem, featured: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminSetFeatured(item.id, featured)
                .onSuccess {
                    _actionMessage.value = if (featured) {
                        "\"${item.title}\" pinned to Featured Spotlight."
                    } else {
                        "\"${item.title}\" removed from Featured Spotlight."
                    }
                }
                .onFailure { _actionMessage.value = it.message ?: "Failed to toggle featured state." }
            _isBusy.value = false
        }
    }

    fun adminBulkApprovePending() {
        val pending = (adminCatalogState.value as? UiState.Success)?.data
            ?.filter { it.status == "pending" } ?: emptyList()
        if (pending.isEmpty()) {
            _actionMessage.value = "No pending uploads to approve."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminApproveAllPending(pending)
                .onSuccess { count -> _actionMessage.value = "Approved $count pending release(s)." }
                .onFailure { _actionMessage.value = it.message ?: "Bulk approval failed." }
            _isBusy.value = false
        }
    }

    fun adminPublishAnnouncement(title: String, message: String, actionUrl: String, onDone: () -> Unit) {
        if (title.isBlank() || message.isBlank()) {
            _actionMessage.value = "Enter both announcement title and message."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.publishAnnouncement(title, message, actionUrl)
                .onSuccess {
                    _actionMessage.value = "Announcement published to Explore!"
                    onDone()
                }
                .onFailure { _actionMessage.value = it.message ?: "Failed to publish announcement." }
            _isBusy.value = false
        }
    }

    fun adminToggleAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.toggleAnnouncementActive(announcement.id, !announcement.active)
                .onSuccess {
                    _actionMessage.value = if (!announcement.active) "Announcement activated." else "Announcement paused."
                }
                .onFailure { _actionMessage.value = it.message ?: "Failed to update announcement." }
            _isBusy.value = false
        }
    }

    fun adminDeleteAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.deleteAnnouncement(announcement.id)
                .onSuccess { _actionMessage.value = "Announcement deleted." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to delete announcement." }
            _isBusy.value = false
        }
    }

    fun adminModerateReview(review: Review, newStatus: String, reason: String = review.flagReason) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminModerateReview(review, newStatus, reason)
                .onSuccess { _actionMessage.value = "Review status updated to $newStatus." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to moderate review." }
            _isBusy.value = false
        }
    }

    fun adminDeleteReview(review: Review) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminDeleteReview(review)
                .onSuccess { _actionMessage.value = "Review deleted." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to delete review." }
            _isBusy.value = false
        }
    }

    fun adminResolveReport(report: ModerationReport, newStatus: String, notes: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminResolveReport(report.id, newStatus, notes)
                .onSuccess { _actionMessage.value = "Report marked $newStatus." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to update report." }
            _isBusy.value = false
        }
    }

    fun adminHideReportedTargetAndResolve(report: ModerationReport, notes: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val finalNote = notes.ifBlank { "Hidden/archived via report moderation" }
            if (report.targetType == "catalog_item") {
                val catalogItem = (adminCatalogState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == report.targetId }
                repository.adminModerateCatalogItem(
                    itemId = report.targetId,
                    newStatus = "archived",
                    encryptedFileId = catalogItem?.encryptedFileId ?: "",
                    moderationNote = finalNote
                )
            } else if (report.targetType == "review") {
                val targetReview = (adminReviewsState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == report.targetId }
                if (targetReview != null) {
                    repository.adminModerateReview(targetReview, "hidden", finalNote)
                }
            }
            repository.adminResolveReport(report.id, "resolved", finalNote)
                .onSuccess { _actionMessage.value = "Target hidden & report resolved." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to resolve report." }
            _isBusy.value = false
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private companion object {
        const val TAG = "CatalogViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
