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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.data.model.CatalogConstants
import com.example.data.model.CatalogItem
import com.example.data.model.ModerationReport
import com.example.data.model.Review
import com.example.data.repository.UploadFileSource
import com.example.ui.components.CatalogItemIcon
import com.example.ui.components.StatusBadge
import com.example.ui.components.UploadOrEditItemForm
import com.example.ui.viewmodel.UiState
import java.util.Locale

@Composable
fun AdminScreen(
    adminEmail: String,
    isBusy: Boolean,
    transferProgress: Float?,
    adminCatalogState: UiState<List<CatalogItem>>,
    adminReviewsState: UiState<List<Review>>,
    adminReportsState: UiState<List<ModerationReport>>,
    adminAnnouncementsState: UiState<List<Announcement>>,
    pendingApprovalCount: Int,
    onPublishItem: (
        title: String,
        description: String,
        whatsNew: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        iconBase64: String,
        fileSource: UploadFileSource?,
        isFeatured: Boolean,
        onSuccess: () -> Unit
    ) -> Unit,
    onOpenItemDetail: (CatalogItem) -> Unit,
    onEditItem: (CatalogItem) -> Unit,
    onSetItemStatus: (CatalogItem, String, String?) -> Unit,
    onToggleFeatured: (CatalogItem, Boolean) -> Unit,
    onBulkApprovePending: () -> Unit,
    onDeleteItem: (CatalogItem) -> Unit,
    onPublishAnnouncement: (String, String, String, () -> Unit) -> Unit,
    onToggleAnnouncement: (Announcement) -> Unit,
    onDeleteAnnouncement: (Announcement) -> Unit,
    onModerateReview: (Review, String) -> Unit,
    onDeleteReview: (Review) -> Unit,
    onResolveReport: (ModerationReport, String, String) -> Unit,
    onHideReportedTargetAndResolve: (ModerationReport, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(if (pendingApprovalCount > 0) 1 else 0) }
    val approvalsTitle = if (pendingApprovalCount > 0) "Approvals ($pendingApprovalCount)" else "Approvals"
    val openReportsCount = (adminReportsState as? UiState.Success)?.data?.count { it.status == "open" } ?: 0
    val modTitle = if (openReportsCount > 0) "Moderation ($openReportsCount)" else "Moderation"
    val subTabs = listOf("Overview", approvalsTitle, "Publish", "Notices", modTitle)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Command Center",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verified Account: $adminEmail",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedSubTab,
                edgePadding = 12.dp
            ) {
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
                0 -> AdminOverviewTab(
                    adminCatalogState = adminCatalogState,
                    adminReviewsState = adminReviewsState,
                    adminReportsState = adminReportsState,
                    adminAnnouncementsState = adminAnnouncementsState,
                    onJumpToTab = { selectedSubTab = it },
                    onOpenItemDetail = onOpenItemDetail
                )
                1 -> AdminCatalogTab(
                    catalogState = adminCatalogState,
                    isBusy = isBusy,
                    onOpenItemDetail = onOpenItemDetail,
                    onEditItem = onEditItem,
                    onSetItemStatus = onSetItemStatus,
                    onToggleFeatured = onToggleFeatured,
                    onBulkApprovePending = onBulkApprovePending,
                    onDeleteItem = onDeleteItem
                )
                2 -> UploadOrEditItemForm(
                    editingItem = null,
                    isAdminMode = true,
                    isBusy = isBusy,
                    transferProgress = transferProgress,
                    onSubmitNew = onPublishItem
                )
                3 -> AdminNoticesTab(
                    announcementsState = adminAnnouncementsState,
                    isBusy = isBusy,
                    onPublishAnnouncement = onPublishAnnouncement,
                    onToggleAnnouncement = onToggleAnnouncement,
                    onDeleteAnnouncement = onDeleteAnnouncement
                )
                4 -> AdminModerationTab(
                    reviewsState = adminReviewsState,
                    reportsState = adminReportsState,
                    onModerateReview = onModerateReview,
                    onDeleteReview = onDeleteReview,
                    onResolveReport = onResolveReport,
                    onHideReportedTargetAndResolve = onHideReportedTargetAndResolve
                )
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    adminCatalogState: UiState<List<CatalogItem>>,
    adminReviewsState: UiState<List<Review>>,
    adminReportsState: UiState<List<ModerationReport>>,
    adminAnnouncementsState: UiState<List<Announcement>>,
    onJumpToTab: (Int) -> Unit,
    onOpenItemDetail: (CatalogItem) -> Unit
) {
    val items = (adminCatalogState as? UiState.Success)?.data ?: emptyList()
    val reviews = (adminReviewsState as? UiState.Success)?.data ?: emptyList()
    val reports = (adminReportsState as? UiState.Success)?.data ?: emptyList()
    val notices = (adminAnnouncementsState as? UiState.Success)?.data ?: emptyList()

    val pendingCount = items.count { it.status == "pending" }
    val approvedCount = items.count { it.status == "approved" }
    val featuredCount = items.count { it.isFeatured && it.status == "approved" }
    val totalDownloads = items.sumOf { it.downloadCount }
    val openReports = reports.count { it.status == "open" }
    val flaggedReviews = reviews.count { it.moderationStatus == "flagged" }
    val activeNotices = notices.count { it.active }
    val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile("Total Releases", "${items.size}", "$approvedCount approved", Modifier.weight(1f))
                MetricTile("Pending Queue", "$pendingCount", "Needs approval", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile("Downloads", "$totalDownloads", "$featuredCount featured", Modifier.weight(1f))
                MetricTile(
                    "Moderation",
                    "${openReports + flaggedReviews}",
                    "$openReports reports • $flaggedReviews reviews",
                    Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    "Avg Rating",
                    if (reviews.isEmpty()) "—" else String.format(Locale.US, "%.1f★", avgRating),
                    "${reviews.size} reviews",
                    Modifier.weight(1f)
                )
                MetricTile("Notices", "$activeNotices", "${notices.size} total banners", Modifier.weight(1f))
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onJumpToTab(1) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Review ($pendingCount)")
                        }
                        OutlinedButton(
                            onClick = { onJumpToTab(3) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Notices")
                        }
                        OutlinedButton(
                            onClick = { onJumpToTab(4) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reports ($openReports)")
                        }
                    }
                }
            }
        }

        if (items.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                        val maxCount = CatalogConstants.UPLOAD_CATEGORIES.maxOf { cat ->
                            items.count { it.category.equals(cat, ignoreCase = true) }
                        }.coerceAtLeast(1)
                        CatalogConstants.UPLOAD_CATEGORIES.forEach { cat ->
                            val count = items.count { it.category.equals(cat, ignoreCase = true) }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "$count",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / maxCount.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            item {
                val topDownloads = items.sortedByDescending { it.downloadCount }.take(3)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Top Downloaded", style = MaterialTheme.typography.titleMedium)
                        topDownloads.forEach { topItem ->
                            OutlinedButton(
                                onClick = { onOpenItemDetail(topItem) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${topItem.title} • ${topItem.downloadCount} downloads",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            RoundedCornerShape(18.dp)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminCatalogTab(
    catalogState: UiState<List<CatalogItem>>,
    isBusy: Boolean,
    onOpenItemDetail: (CatalogItem) -> Unit,
    onEditItem: (CatalogItem) -> Unit,
    onSetItemStatus: (CatalogItem, String, String?) -> Unit,
    onToggleFeatured: (CatalogItem, Boolean) -> Unit,
    onBulkApprovePending: () -> Unit,
    onDeleteItem: (CatalogItem) -> Unit
) {
    var statusFilter by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var flagTarget by remember { mutableStateOf<CatalogItem?>(null) }
    var flagNote by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CatalogItem?>(null) }

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
            val q = search.trim().lowercase()
            val filtered = allItems.filter { item ->
                val matchesStatus = when (statusFilter) {
                    "all" -> true
                    "featured" -> item.isFeatured
                    else -> item.status == statusFilter
                }
                val matchesSearch = q.isEmpty() ||
                    item.title.lowercase().contains(q) ||
                    item.uploaderName.lowercase().contains(q) ||
                    item.category.lowercase().contains(q)
                matchesStatus && matchesSearch
            }.sortedByDescending { it.status == "pending" }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search admin catalog...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf(
                            "all" to "All (${allItems.size})",
                            "pending" to "Pending ($pendingCount)",
                            "featured" to "Featured",
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

                if (pendingCount > 0) {
                    item {
                        Button(
                            onClick = onBulkApprovePending,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_bulk_approve_button")
                        ) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Approve All Pending ($pendingCount)", fontWeight = FontWeight.Bold)
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
                                text = "No releases match this filter.",
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
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val uploader = item.uploaderName.ifBlank { item.creatorHandle }
                                        Text(
                                            text = "${item.category} • v${item.version} • $uploader",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusBadge(status = item.status)
                                }

                                if (item.moderationNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Note: ${item.moderationNote}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (item.status != "approved") {
                                            item {
                                                Button(
                                                    onClick = { onSetItemStatus(item, "approved", "") },
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.testTag("admin_approve_${item.id}")
                                                ) {
                                                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(15.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Approve")
                                                }
                                            }
                                        }
                                        if (item.status != "flagged") {
                                            item {
                                                OutlinedButton(
                                                    onClick = {
                                                        flagTarget = item
                                                        flagNote = item.moderationNote
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Filled.Flag, null, Modifier.size(15.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Flag")
                                                }
                                            }
                                        }
                                        if (item.status != "archived") {
                                            item {
                                                OutlinedButton(
                                                    onClick = { onSetItemStatus(item, "archived", null) },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Archive")
                                                }
                                            }
                                        } else {
                                            item {
                                                OutlinedButton(
                                                    onClick = { onSetItemStatus(item, "approved", "") },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Filled.Restore, null, Modifier.size(15.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Restore")
                                                }
                                            }
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = { onToggleFeatured(item, !item.isFeatured) }) {
                                            Icon(
                                                imageVector = if (item.isFeatured)
                                                    Icons.Filled.AutoAwesome
                                                else
                                                    Icons.Outlined.AutoAwesome,
                                                contentDescription = if (item.isFeatured) "Unfeature release" else "Feature release",
                                                tint = if (item.isFeatured)
                                                    MaterialTheme.colorScheme.tertiary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onEditItem(item) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit release",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { deleteTarget = item }) {
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

    if (flagTarget != null) {
        val target = flagTarget!!
        AlertDialog(
            onDismissRequest = { flagTarget = null },
            title = { Text("Flag \"${target.title}\"") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Add a moderation note visible to the uploader so they know what to fix:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = flagNote,
                        onValueChange = { flagNote = it },
                        label = { Text("Feedback note (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSetItemStatus(target, "flagged", flagNote)
                    flagTarget = null
                    flagNote = ""
                }) {
                    Text("Flag Release")
                }
            },
            dismissButton = {
                TextButton(onClick = { flagTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (deleteTarget != null) {
        val target = deleteTarget!!
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Release?") },
            text = { Text("Permanently remove \"${target.title}\" and any attached vault chunks?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteItem(target)
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminNoticesTab(
    announcementsState: UiState<List<Announcement>>,
    isBusy: Boolean,
    onPublishAnnouncement: (String, String, String, () -> Unit) -> Unit,
    onToggleAnnouncement: (Announcement) -> Unit,
    onDeleteAnnouncement: (Announcement) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var actionUrl by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Banner Notice", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Announcement Title *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notice_title_input")
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message *") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notice_message_input")
                    )
                    OutlinedTextField(
                        value = actionUrl,
                        onValueChange = { actionUrl = it },
                        label = { Text("Optional Link URL (https://...)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onPublishAnnouncement(title, message, actionUrl) {
                                title = ""
                                message = ""
                                actionUrl = ""
                            }
                        },
                        enabled = !isBusy && title.isNotBlank() && message.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_publish_notice_button")
                    ) {
                        Text("Publish Notice to Explore", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        when (announcementsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item {
                Text(announcementsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                if (announcementsState.data.isEmpty()) {
                    item {
                        Text(
                            text = "No notices broadcast yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(announcementsState.data, key = { it.id }) { ann ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ann.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = ann.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = ann.active,
                                            onCheckedChange = { onToggleAnnouncement(ann) }
                                        )
                                        IconButton(onClick = { onDeleteAnnouncement(ann) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete notice",
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
    onResolveReport: (ModerationReport, String, String) -> Unit,
    onHideReportedTargetAndResolve: (ModerationReport, String) -> Unit
) {
    var reportFilter by remember { mutableStateOf("open") }
    var noteDialogReport by remember { mutableStateOf<ModerationReport?>(null) }
    var noteDialogAction by remember { mutableStateOf("resolved") }
    var noteInput by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Content Reports Queue",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = reportFilter == "open",
                        onClick = { reportFilter = "open" },
                        label = { Text("Open") }
                    )
                    FilterChip(
                        selected = reportFilter == "all",
                        onClick = { reportFilter = "all" },
                        label = { Text("All") }
                    )
                }
            }
        }

        when (reportsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item {
                Text(reportsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                val filteredReports = if (reportFilter == "open") {
                    reportsState.data.filter { it.status == "open" }
                } else {
                    reportsState.data
                }
                if (filteredReports.isEmpty()) {
                    item {
                        Text(
                            text = if (reportFilter == "open") "No open reports right now." else "No moderation reports submitted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(filteredReports, key = { it.id }) { report ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${report.targetTitle} (${report.targetType} • ${report.status})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Reported by ${report.reporterName}: ${report.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (report.adminNotes.isNotBlank()) {
                                    Text(
                                        text = "Admin note: ${report.adminNotes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (report.status == "open") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item {
                                            Button(
                                                onClick = {
                                                    noteDialogReport = report
                                                    noteDialogAction = "hide_and_resolve"
                                                    noteInput = "Removed/hidden by moderator"
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.Shield, null, Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Hide Target & Resolve")
                                            }
                                        }
                                        item {
                                            OutlinedButton(
                                                onClick = {
                                                    noteDialogReport = report
                                                    noteDialogAction = "resolved"
                                                    noteInput = "Verified safe / resolved"
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Resolve")
                                            }
                                        }
                                        item {
                                            TextButton(
                                                onClick = { onResolveReport(report, "dismissed", "Dismissed") }
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

    if (noteDialogReport != null) {
        val target = noteDialogReport!!
        AlertDialog(
            onDismissRequest = { noteDialogReport = null },
            title = {
                Text(
                    if (noteDialogAction == "hide_and_resolve") "Hide Target & Resolve Report"
                    else "Resolve Report"
                )
            },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Admin resolution note") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (noteDialogAction == "hide_and_resolve") {
                        onHideReportedTargetAndResolve(target, noteInput)
                    } else {
                        onResolveReport(target, "resolved", noteInput)
                    }
                    noteDialogReport = null
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteDialogReport = null }) { Text("Cancel") }
            }
        )
    }
}
