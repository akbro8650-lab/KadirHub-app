package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CatalogConstants
import com.example.data.model.CatalogItem
import com.example.data.model.ModerationReport
import com.example.data.model.Review
import com.example.data.repository.UploadFileSource
import com.example.ui.components.CatalogItemIcon
import com.example.ui.components.StatusBadge
import com.example.ui.components.UploadOrEditItemForm
import com.example.ui.viewmodel.UiState

@Composable
fun AdminScreen(
    isUnlocked: Boolean,
    isServerAdmin: Boolean,
    isBusy: Boolean,
    transferProgress: Float?,
    adminCatalogState: UiState<List<CatalogItem>>,
    adminReviewsState: UiState<List<Review>>,
    adminReportsState: UiState<List<ModerationReport>>,
    pendingApprovalCount: Int,
    onUnlock: (String) -> Boolean,
    onLock: () -> Unit,
    onPublishItem: (
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        iconBase64: String,
        fileSource: UploadFileSource?,
        onSuccess: () -> Unit
    ) -> Unit,
    onOpenItemDetail: (CatalogItem) -> Unit,
    onEditItem: (CatalogItem) -> Unit,
    onSetItemStatus: (CatalogItem, String) -> Unit,
    onDeleteItem: (CatalogItem) -> Unit,
    onModerateReview: (Review, String) -> Unit,
    onDeleteReview: (Review) -> Unit,
    onResolveReport: (ModerationReport, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isUnlocked) {
        AdminPasswordGate(
            onUnlock = onUnlock,
            modifier = modifier
        )
    } else {
        AdminDashboardContent(
            isServerAdmin = isServerAdmin,
            isBusy = isBusy,
            transferProgress = transferProgress,
            adminCatalogState = adminCatalogState,
            adminReviewsState = adminReviewsState,
            adminReportsState = adminReportsState,
            pendingApprovalCount = pendingApprovalCount,
            onLock = onLock,
            onPublishItem = onPublishItem,
            onOpenItemDetail = onOpenItemDetail,
            onEditItem = onEditItem,
            onSetItemStatus = onSetItemStatus,
            onDeleteItem = onDeleteItem,
            onModerateReview = onModerateReview,
            onDeleteReview = onDeleteReview,
            onResolveReport = onResolveReport,
            modifier = modifier
        )
    }
}

@Composable
private fun AdminPasswordGate(
    onUnlock: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val invalidMsg = stringResource(R.string.admin_invalid_password)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "Admin Lock",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.admin_lock_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.admin_lock_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.admin_password_label)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.VisibilityOff
                                else
                                    Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = errorText != null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Button(
                    onClick = {
                        if (!onUnlock(password)) {
                            errorText = invalidMsg
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.admin_unlock_cta),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardContent(
    isServerAdmin: Boolean,
    isBusy: Boolean,
    transferProgress: Float?,
    adminCatalogState: UiState<List<CatalogItem>>,
    adminReviewsState: UiState<List<Review>>,
    adminReportsState: UiState<List<ModerationReport>>,
    pendingApprovalCount: Int,
    onLock: () -> Unit,
    onPublishItem: (
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        iconBase64: String,
        fileSource: UploadFileSource?,
        onSuccess: () -> Unit
    ) -> Unit,
    onOpenItemDetail: (CatalogItem) -> Unit,
    onEditItem: (CatalogItem) -> Unit,
    onSetItemStatus: (CatalogItem, String) -> Unit,
    onDeleteItem: (CatalogItem) -> Unit,
    onModerateReview: (Review, String) -> Unit,
    onDeleteReview: (Review) -> Unit,
    onResolveReport: (ModerationReport, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(if (pendingApprovalCount > 0) 0 else 1) }
    val approvalsTitle = if (pendingApprovalCount > 0) "Approvals ($pendingApprovalCount)" else "Approvals"
    val subTabs = listOf(approvalsTitle, "Publish", "Moderation")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Admin Control Center",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Creator: ${CatalogConstants.CREATOR_TIKTOK_HANDLE}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedButton(
                    onClick = onLock,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("admin_lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock")
                }
            }

            if (!isServerAdmin) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign in with admin Google account (${CatalogConstants.ADMIN_EMAIL}) for server-side Firestore approval & moderation access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = selectedSubTab) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = { selectedSubTab = index },
                        text = { Text(title) },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            when (selectedSubTab) {
                0 -> AdminCatalogTab(
                    catalogState = adminCatalogState,
                    onOpenItemDetail = onOpenItemDetail,
                    onEditItem = onEditItem,
                    onSetItemStatus = onSetItemStatus,
                    onDeleteItem = onDeleteItem
                )
                1 -> UploadOrEditItemForm(
                    editingItem = null,
                    isAdminMode = true,
                    isBusy = isBusy,
                    transferProgress = transferProgress,
                    onSubmitNew = onPublishItem
                )
                2 -> AdminModerationTab(
                    reviewsState = adminReviewsState,
                    reportsState = adminReportsState,
                    onModerateReview = onModerateReview,
                    onDeleteReview = onDeleteReview,
                    onResolveReport = onResolveReport
                )
            }
        }
    }
}

@Composable
private fun AdminCatalogTab(
    catalogState: UiState<List<CatalogItem>>,
    onOpenItemDetail: (CatalogItem) -> Unit,
    onEditItem: (CatalogItem) -> Unit,
    onSetItemStatus: (CatalogItem, String) -> Unit,
    onDeleteItem: (CatalogItem) -> Unit
) {
    var statusFilter by remember { mutableStateOf("all") }

    when (catalogState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(text = catalogState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val allItems = catalogState.data
            val pendingCount = allItems.count { it.status == "pending" }
            val filtered = if (statusFilter == "all") {
                allItems.sortedByDescending { it.status == "pending" }
            } else {
                allItems.filter { it.status == statusFilter }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf(
                            "all" to "All (${allItems.size})",
                            "pending" to "Pending ($pendingCount)",
                            "approved" to "Approved",
                            "flagged" to "Flagged",
                            "archived" to "Archived"
                        )
                        items(filters) { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { statusFilter = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("admin_filter_$key")
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No releases in this status filter.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        Card(
                            onClick = { onOpenItemDetail(item) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (item.status == "pending")
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CatalogItemIcon(
                                        iconBase64 = item.iconBase64,
                                        category = item.category,
                                        size = 46.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                                        val uploader = item.uploaderName.ifBlank { item.creatorHandle }
                                        Text(
                                            text = "${item.category} • v${item.version} • By $uploader",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusBadge(status = item.status)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (item.status != "approved") {
                                            Button(
                                                onClick = { onSetItemStatus(item, "approved") },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("admin_approve_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve")
                                            }
                                        }
                                        if (item.status != "archived") {
                                            OutlinedButton(
                                                onClick = { onSetItemStatus(item, "archived") },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Archive")
                                            }
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { onEditItem(item) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit release",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { onDeleteItem(item) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete item",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminModerationTab(
    reviewsState: UiState<List<Review>>,
    reportsState: UiState<List<ModerationReport>>,
    onModerateReview: (Review, String) -> Unit,
    onDeleteReview: (Review) -> Unit,
    onResolveReport: (ModerationReport, String, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Content Reports Queue",
                style = MaterialTheme.typography.titleMedium
            )
        }

        when (reportsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item {
                Text(reportsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                if (reportsState.data.isEmpty()) {
                    item {
                        Text(
                            text = "No moderation reports submitted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(reportsState.data, key = { it.id }) { report ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${report.targetTitle} (${report.status})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Reported by ${report.reporterName}: ${report.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (report.status == "open") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onResolveReport(report, "resolved", "Handled by admin") },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Resolve")
                                        }
                                        OutlinedButton(
                                            onClick = { onResolveReport(report, "dismissed", "Dismissed") },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "User Reviews Moderation",
                style = MaterialTheme.typography.titleMedium
            )
        }

        when (reviewsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item {
                Text(reviewsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                if (reviewsState.data.isEmpty()) {
                    item {
                        Text(
                            text = "No user reviews to moderate.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(reviewsState.data, key = { it.id }) { review ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${review.authorName} on ${review.itemTitle}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = review.moderationStatus.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (review.moderationStatus) {
                                            "flagged" -> MaterialTheme.colorScheme.error
                                            "hidden" -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${review.comment}\" (${review.rating}/5)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (review.flagReason.isNotBlank()) {
                                    Text(
                                        text = "Flag note: ${review.flagReason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onModerateReview(review, "visible") }) {
                                        Text("Approve")
                                    }
                                    TextButton(onClick = { onModerateReview(review, "hidden") }) {
                                        Text("Hide")
                                    }
                                    TextButton(onClick = { onDeleteReview(review) }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
