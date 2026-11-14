package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.repository.UploadFileSource
import com.example.ui.components.CatalogItemIcon
import com.example.ui.components.StatusBadge
import com.example.ui.components.UploadOrEditItemForm
import com.example.ui.viewmodel.UiState

@Composable
fun UploadScreen(
    myUploadsState: UiState<List<CatalogItem>>,
    editingItem: CatalogItem?,
    isBusy: Boolean,
    transferProgress: Float?,
    decryptUrl: (CatalogItem) -> String,
    onStartEdit: (CatalogItem) -> Unit,
    onCancelEdit: () -> Unit,
    onSubmitNew: (
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
    onSubmitEdit: (
        existingItem: CatalogItem,
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        iconBase64: String,
        newFileSource: UploadFileSource?,
        removeAttachedFile: Boolean,
        onSuccess: () -> Unit
    ) -> Unit,
    onDeleteItem: (CatalogItem) -> Unit,
    onOpenDetail: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var deleteConfirmTarget by remember { mutableStateOf<CatalogItem?>(null) }

    if (editingItem != null) {
        BackHandler(onBack = onCancelEdit)
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                Text(
                    text = "Editing: ${editingItem.title}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                UploadOrEditItemForm(
                    editingItem = editingItem,
                    initialDecryptedUrl = decryptUrl(editingItem),
                    isAdminMode = false,
                    isBusy = isBusy,
                    transferProgress = transferProgress,
                    onCancelEdit = onCancelEdit,
                    onSubmitEdit = onSubmitEdit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        return
    }

    val uploadCount = (myUploadsState as? UiState.Success)?.data?.size ?: 0
    val subTabs = listOf("Upload APK / Link", "My Uploads ($uploadCount)")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            TabRow(selectedTabIndex = selectedSubTab) {
                subTabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = { selectedSubTab = index },
                        text = { Text(label) },
                        modifier = Modifier.testTag("upload_tab_$index")
                    )
                }
            }

            when (selectedSubTab) {
                0 -> UploadOrEditItemForm(
                    editingItem = null,
                    isAdminMode = false,
                    isBusy = isBusy,
                    transferProgress = transferProgress,
                    onSubmitNew = { title, desc, cat, ver, url, size, icon, file, onSuccess ->
                        onSubmitNew(title, desc, cat, ver, url, size, icon, file) {
                            onSuccess()
                            selectedSubTab = 1
                        }
                    }
                )

                1 -> MyUploadsList(
                    myUploadsState = myUploadsState,
                    onOpenDetail = onOpenDetail,
                    onEditClick = onStartEdit,
                    onDeleteClick = { deleteConfirmTarget = it }
                )
            }
        }
    }

    if (deleteConfirmTarget != null) {
        val target = deleteConfirmTarget!!
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            title = { Text("Delete Upload?") },
            text = {
                Text("Remove \"${target.title}\" and any attached encrypted APK from KadirHub?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteItem(target)
                        deleteConfirmTarget = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MyUploadsList(
    myUploadsState: UiState<List<CatalogItem>>,
    onOpenDetail: (CatalogItem) -> Unit,
    onEditClick: (CatalogItem) -> Unit,
    onDeleteClick: (CatalogItem) -> Unit
) {
    when (myUploadsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(text = myUploadsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            if (myUploadsState.data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No uploads yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share an APK (up to 50 MB) or link using the Upload tab. Once approved by the admin, it goes live in Explore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myUploadsState.data, key = { it.id }) { item ->
                        Card(
                            onClick = { onOpenDetail(item) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(18.dp)
                                )
                                .testTag("my_upload_card_${item.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CatalogItemIcon(
                                        iconBase64 = item.iconBase64,
                                        category = item.category,
                                        size = 48.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.category} • v${item.version}" +
                                                if (item.fileSizeLabel.isNotBlank()) " • ${item.fileSizeLabel}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusBadge(status = item.status)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onEditClick(item) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("edit_upload_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit")
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDeleteClick(item) },
                                        modifier = Modifier.testTag("delete_upload_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete upload",
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
