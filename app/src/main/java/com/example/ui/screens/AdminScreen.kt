package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.viewmodel.UiState

@Composable
fun AdminScreen(
    isUnlocked: Boolean,
    isServerAdmin: Boolean,
    isBusy: Boolean,
    adminCatalogState: UiState<List<CatalogItem>>,
    adminReviewsState: UiState<List<Review>>,
    adminReportsState: UiState<List<ModerationReport>>,
    onUnlock: (String) -> Boolean,
    onLock: () -> Unit,
    onPublishItem: (
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        attachedFileName: String?,
        attachedMimeType: String?,
        attachedBytes: ByteArray?,
        onSuccess: () -> Unit
    ) -> Unit,
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
            adminCatalogState = adminCatalogState,
            adminReviewsState = adminReviewsState,
            adminReportsState = adminReportsState,
            onLock = onLock,
            onPublishItem = onPublishItem,
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
    adminCatalogState: UiState<List<CatalogItem>>,
    adminReviewsState: UiState<List<Review>>,
    adminReportsState: UiState<List<ModerationReport>>,
    onLock: () -> Unit,
    onPublishItem: (
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        attachedFileName: String?,
        attachedMimeType: String?,
        attachedBytes: ByteArray?,
        onSuccess: () -> Unit
    ) -> Unit,
    onSetItemStatus: (CatalogItem, String) -> Unit,
    onDeleteItem: (CatalogItem) -> Unit,
    onModerateReview: (Review, String) -> Unit,
    onDeleteReview: (Review) -> Unit,
    onResolveReport: (ModerationReport, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Upload", "Catalog", "Moderation")

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
                            text = "Sign in with admin Google account (${CatalogConstants.ADMIN_EMAIL}) for server-side Firestore write clearance.",
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
                        modifier = Modifier.testTag("admin_tab_${title.lowercase()}")
                    )
                }
            }

            when (selectedSubTab) {
                0 -> AdminUploadTab(
                    isBusy = isBusy,
                    onPublishItem = onPublishItem
                )
                1 -> AdminCatalogTab(
                    catalogState = adminCatalogState,
                    onSetItemStatus = onSetItemStatus,
                    onDeleteItem = onDeleteItem
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
private fun AdminUploadTab(
    isBusy: Boolean,
    onPublishItem: (
        title: String,
        description: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        attachedFileName: String?,
        attachedMimeType: String?,
        attachedBytes: ByteArray?,
        onSuccess: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("App") }
    var version by remember { mutableStateOf("1.0.0") }
    var targetUrl by remember { mutableStateOf("") }
    var fileSizeLabel by remember { mutableStateOf("") }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedMimeType by remember { mutableStateOf<String?>(null) }
    var attachedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var displayName = "vault_file.bin"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    fileError = "Selected file is empty."
                } else if (bytes.size > CatalogConstants.MAX_DIRECT_FILE_BYTES) {
                    fileError = "Direct encrypted vault limit is 500 KB (${bytes.size / 1024} KB selected). Host large APKs externally and paste the link above."
                } else {
                    fileError = null
                    attachedFileName = displayName
                    attachedMimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    attachedBytes = bytes
                    if (fileSizeLabel.isBlank()) {
                        val kb = (bytes.size / 1024.0).coerceAtLeast(0.1)
                        fileSizeLabel = String.format(java.util.Locale.US, "%.1f KB", kb)
                    }
                }
            } catch (e: Exception) {
                fileError = "Could not read file: ${e.localizedMessage}"
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Links and attached files are encrypted client-side with AES-256-GCM + SHA-256 before writing to Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("App or Link Title *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_title_input")
            )
        }

        item {
            Column {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CatalogConstants.UPLOAD_CATEGORIES) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Version") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fileSizeLabel,
                    onValueChange = { fileSizeLabel = it },
                    label = { Text("Size (e.g. 12 MB)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = targetUrl,
                onValueChange = { targetUrl = it },
                label = { Text("Download / Mirror Link URL") },
                placeholder = { Text("https://...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_url_input")
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & Release Notes *") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_description_input")
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Encrypted Vault File (Optional)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = attachedFileName ?: "Attach a file ≤ 500 KB for direct AES-256-GCM storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (attachedFileName != null) {
                            IconButton(onClick = {
                                attachedFileName = null
                                attachedMimeType = null
                                attachedBytes = null
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove attached file"
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select")
                            }
                        }
                    }
                    if (fileError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = fileError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            val canPublish = title.isNotBlank() &&
                description.isNotBlank() &&
                (targetUrl.isNotBlank() || attachedBytes != null) &&
                !isBusy

            Button(
                onClick = {
                    onPublishItem(
                        title,
                        description,
                        category,
                        version,
                        targetUrl,
                        fileSizeLabel,
                        attachedFileName,
                        attachedMimeType,
                        attachedBytes
                    ) {
                        title = ""
                        description = ""
                        targetUrl = ""
                        fileSizeLabel = ""
                        attachedFileName = null
                        attachedMimeType = null
                        attachedBytes = null
                    }
                },
                enabled = canPublish,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_publish_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Publish,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Encrypt & Publish", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminCatalogTab(
    catalogState: UiState<List<CatalogItem>>,
    onSetItemStatus: (CatalogItem, String) -> Unit,
    onDeleteItem: (CatalogItem) -> Unit
) {
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
            if (catalogState.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No uploaded items yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(catalogState.data, key = { it.id }) { item ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${item.category} • v${item.version} • Status: ${item.status}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("approved", "flagged", "archived").forEach { st ->
                                        FilterChip(
                                            selected = item.status == st,
                                            onClick = { onSetItemStatus(item, st) },
                                            label = { Text(st.replaceFirstChar { it.uppercase() }) }
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
