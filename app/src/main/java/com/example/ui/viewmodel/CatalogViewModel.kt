package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.crypto.CryptoVaultManager
import com.example.data.model.CatalogItem
import com.example.data.model.ModerationReport
import com.example.data.model.Review
import com.example.data.repository.CatalogRepository
import com.example.data.repository.DecryptedVaultFile
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

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SortOption(val label: String) {
    NEWEST("Newest"),
    TOP_RATED("Top Rated"),
    MOST_DOWNLOADED("Most Downloaded")
}

sealed interface DownloadActionResult {
    data class OpenUrl(val url: String, val itemTitle: String) : DownloadActionResult
    data class FileDecrypted(val file: DecryptedVaultFile, val fallbackUrl: String) : DownloadActionResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val repository: CatalogRepository,
    val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId.asStateFlow()

    private val _isAdminPanelUnlocked = MutableStateFlow(false)
    val isAdminPanelUnlocked: StateFlow<Boolean> = _isAdminPanelUnlocked.asStateFlow()

    private val _isServerAdmin = MutableStateFlow(false)
    val isServerAdmin: StateFlow<Boolean> = _isServerAdmin.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

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

    val filteredCatalogState: StateFlow<UiState<List<CatalogItem>>> = combine(
        rawCatalogState,
        _searchQuery,
        _selectedCategory,
        _sortOption
    ) { state, query, category, sort ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val q = query.trim().lowercase()
                val filtered = state.data.filter { item ->
                    val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)
                    val matchesQuery = q.isEmpty() ||
                        item.title.lowercase().contains(q) ||
                        item.description.lowercase().contains(q) ||
                        item.category.lowercase().contains(q)
                    matchesCategory && matchesQuery
                }
                val sorted = when (sort) {
                    SortOption.NEWEST -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    SortOption.TOP_RATED -> filtered.sortedByDescending { it.averageRating }
                    SortOption.MOST_DOWNLOADED -> filtered.sortedByDescending { it.downloadCount }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val selectedItem: StateFlow<CatalogItem?> = combine(
        rawCatalogState,
        _selectedItemId
    ) { state, id ->
        if (id == null || state !is UiState.Success) null
        else state.data.firstOrNull { it.id == id }
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

    private val adminActiveFlow: StateFlow<Boolean> = combine(
        _isAdminPanelUnlocked,
        _isServerAdmin
    ) { unlocked, serverAdmin -> unlocked && serverAdmin }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val adminCatalogState: StateFlow<UiState<List<CatalogItem>>> = adminActiveFlow
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

    val adminReviewsState: StateFlow<UiState<List<Review>>> = adminActiveFlow
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

    val adminReportsState: StateFlow<UiState<List<ModerationReport>>> = adminActiveFlow
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

    init {
        refreshAdminStatus()
    }

    fun refreshAdminStatus() {
        viewModelScope.launch {
            _isServerAdmin.value = repository.isServerAdmin()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
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

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun clearPendingDownload() {
        _pendingDownloadResult.value = null
    }

    fun unlockAdminPanel(password: String): Boolean {
        val valid = CryptoVaultManager.verifyAdminPassword(password)
        if (valid) {
            _isAdminPanelUnlocked.value = true
            refreshAdminStatus()
            _actionMessage.value = "Admin Dashboard unlocked."
        }
        return valid
    }

    fun lockAdminPanel() {
        _isAdminPanelUnlocked.value = false
        _actionMessage.value = "Admin Dashboard locked."
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

    fun downloadOrOpenItem(item: CatalogItem) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val decryptedUrl = decryptItemUrl(item)
                if (item.itemType == "encrypted_file" && item.encryptedFileId.isNotBlank()) {
                    val fileResult = repository.downloadAndDecryptFile(item.encryptedFileId)
                    fileResult.onSuccess { decryptedFile ->
                        repository.recordDownload(item.id)
                        _pendingDownloadResult.value = DownloadActionResult.FileDecrypted(
                            file = decryptedFile,
                            fallbackUrl = decryptedUrl
                        )
                    }.onFailure { err ->
                        _actionMessage.value = err.message ?: "Failed to decrypt file."
                    }
                } else if (decryptedUrl.isNotBlank()) {
                    repository.recordDownload(item.id)
                    _pendingDownloadResult.value = DownloadActionResult.OpenUrl(
                        url = decryptedUrl,
                        itemTitle = item.title
                    )
                } else {
                    _actionMessage.value = "No downloadable link or file attached."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun submitReview(item: CatalogItem, rating: Int, comment: String) {
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
                .onSuccess { _actionMessage.value = "Report submitted to @kadirofficial007 moderation queue." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to submit report." }
            _isBusy.value = false
        }
    }

    fun publishCatalogItem(
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        attachedFileName: String?,
        attachedMimeType: String?,
        attachedBytes: ByteArray?,
        onPublished: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.publishCatalogItem(
                title = title,
                description = description,
                category = category,
                version = version,
                targetUrl = targetUrl,
                fileSizeLabel = fileSizeLabel,
                attachedFileName = attachedFileName,
                attachedMimeType = attachedMimeType,
                attachedBytes = attachedBytes
            )
                .onSuccess {
                    _actionMessage.value = "Encrypted & published to KadirHub!"
                    onPublished()
                }
                .onFailure {
                    _actionMessage.value = it.message ?: "Upload failed. Ensure your account has admin access."
                }
            _isBusy.value = false
        }
    }

    fun adminSetItemStatus(item: CatalogItem, newStatus: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminModerateCatalogItem(item.id, newStatus, item.encryptedFileId)
                .onSuccess { _actionMessage.value = "${item.title} marked as $newStatus." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to update item status." }
            _isBusy.value = false
        }
    }

    fun adminDeleteItem(item: CatalogItem) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.adminDeleteCatalogItem(item.id, item.encryptedFileId)
                .onSuccess { _actionMessage.value = "${item.title} removed." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to delete item." }
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
