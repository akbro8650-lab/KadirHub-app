package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.data.model.CatalogConstants
import com.example.data.model.CatalogItem
import com.example.data.repository.UploadFileSource
import com.example.ui.util.IconImageProcessor
import java.util.Locale

fun categoryIconFor(category: String): ImageVector = when (category) {
    "App" -> Icons.Filled.Apps
    "Link" -> Icons.Filled.Link
    "Tool" -> Icons.Filled.Build
    "Game" -> Icons.Filled.Gamepad
    else -> Icons.Filled.FolderZip
}

@Composable
fun CatalogItemIcon(
    iconBase64: String,
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 14.dp
) {
    val decoded = remember(iconBase64) { IconImageProcessor.decodeIconBitmap(iconBase64) }
    val shape = RoundedCornerShape(cornerRadius)
    if (decoded != null) {
        Image(
            bitmap = decoded,
            contentDescription = "$category icon",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
        )
    } else {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
            shape = shape,
            modifier = modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = categoryIconFor(category),
                    contentDescription = category,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when (status.lowercase()) {
        "approved" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Approved"
        )
        "pending" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Pending"
        )
        "flagged" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Flagged"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            status.replaceFirstChar { it.uppercase() }
        )
    }
    Surface(
        color = bg,
        shape = CircleShape,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AnnouncementBannerCard(
    announcement: Announcement,
    onActionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = "Announcement",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (announcement.actionUrl.isNotBlank() && onActionClick != null) {
                TextButton(onClick = { onActionClick(announcement.actionUrl) }) {
                    Text("Open")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open announcement link",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedSpotlightCarousel(
    featuredItems: List<CatalogItem>,
    onItemClick: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredItems.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Featured Spotlight",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(featuredItems, key = { "featured_${it.id}" }) { item ->
                Card(
                    onClick = { onItemClick(item) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .width(248.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                            RoundedCornerShape(20.dp)
                        )
                        .testTag("featured_card_${item.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CatalogItemIcon(
                                iconBase64 = item.iconBase64,
                                category = item.category,
                                size = 46.dp,
                                cornerRadius = 14.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.category} • v${item.version}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (item.reviewCount > 0) String.format(Locale.US, "%.1f", item.averageRating) else "New",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${item.downloadCount}",
                                    style = MaterialTheme.typography.labelSmall
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
fun GuestSignInRequiredCard(
    title: String,
    subtitle: String,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onSignInClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("guest_card_sign_in_button")
                ) {
                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun queryFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
    var displayName = "app_release.apk"
    var byteSize = -1L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) byteSize = cursor.getLong(sizeIdx)
        }
    }
    if (byteSize <= 0L) {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            if (afd.length > 0L) byteSize = afd.length
        }
    }
    if (byteSize <= 0L) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buf = ByteArray(16 * 1024)
            var count = 0L
            while (true) {
                val r = stream.read(buf)
                if (r == -1) break
                count += r
            }
            byteSize = count
        }
    }
    return displayName to byteSize
}

@Composable
fun UploadOrEditItemForm(
    editingItem: CatalogItem?,
    initialDecryptedUrl: String = "",
    isAdminMode: Boolean,
    isBusy: Boolean,
    transferProgress: Float?,
    onCancelEdit: (() -> Unit)? = null,
    onSubmitNew: (
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onSubmitEdit: (
        existingItem: CatalogItem,
        title: String,
        description: String,
        whatsNew: String,
        category: String,
        version: String,
        targetUrl: String,
        fileSizeLabel: String,
        iconBase64: String,
        newFileSource: UploadFileSource?,
        removeAttachedFile: Boolean,
        isFeatured: Boolean,
        onSuccess: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEditing = editingItem != null

    var title by remember(editingItem?.id) { mutableStateOf(editingItem?.title ?: "") }
    var description by remember(editingItem?.id) { mutableStateOf(editingItem?.description ?: "") }
    var whatsNew by remember(editingItem?.id) { mutableStateOf(editingItem?.whatsNew ?: "") }
    var category by remember(editingItem?.id) { mutableStateOf(editingItem?.category ?: "App") }
    var version by remember(editingItem?.id) { mutableStateOf(editingItem?.version ?: "1.0.0") }
    var targetUrl by remember(editingItem?.id) { mutableStateOf(initialDecryptedUrl) }
    var fileSizeLabel by remember(editingItem?.id) { mutableStateOf(editingItem?.fileSizeLabel ?: "") }
    var iconBase64 by remember(editingItem?.id) { mutableStateOf(editingItem?.iconBase64 ?: "") }
    var isFeatured by remember(editingItem?.id) { mutableStateOf(editingItem?.isFeatured ?: false) }
    var iconError by remember(editingItem?.id) { mutableStateOf<String?>(null) }

    var selectedFileSource by remember(editingItem?.id) { mutableStateOf<UploadFileSource.Streamed?>(null) }
    var keepExistingFile by remember(editingItem?.id) {
        mutableStateOf(editingItem?.itemType == "encrypted_file" && editingItem.encryptedFileId.isNotBlank())
    }
    var fileError by remember(editingItem?.id) { mutableStateOf<String?>(null) }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = IconImageProcessor.processIconUri(context, uri)
            if (encoded != null) {
                iconBase64 = encoded
                iconError = null
            } else {
                iconError = "Could not process selected icon image."
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val (displayName, byteSize) = queryFileMetadata(context, uri)
                if (byteSize <= 0L) {
                    fileError = "Selected file is empty."
                } else if (byteSize > CatalogConstants.MAX_DIRECT_FILE_BYTES) {
                    fileError = "Direct encrypted vault limit is 50 MB (${formatFileSize(byteSize)} selected). Host larger files externally and paste the link above."
                } else {
                    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    selectedFileSource = UploadFileSource.Streamed(
                        fileName = displayName,
                        mimeType = mime,
                        byteSize = byteSize,
                        openStream = {
                            context.contentResolver.openInputStream(uri)
                                ?: throw IllegalStateException("Unable to read selected file.")
                        }
                    )
                    keepExistingFile = false
                    fileError = null
                    if (fileSizeLabel.isBlank() || isEditing) {
                        fileSizeLabel = formatFileSize(byteSize)
                    }
                }
            } catch (e: Exception) {
                fileError = "Could not read file: ${e.localizedMessage}"
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
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
                        text = when {
                            isEditing && !isAdminMode ->
                                "Edit your release details, changelog, icon, or APK. Saving resubmits it for admin approval."
                            isAdminMode ->
                                "Admin mode: APKs up to 50 MB and links are AES-256-GCM encrypted and published immediately."
                            else ->
                                "Upload your APK (up to 50 MB), custom icon, or download link. All uploads stay encrypted until approved by admin."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Icon Upload Picker Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CatalogItemIcon(
                            iconBase64 = iconBase64,
                            category = category,
                            size = 58.dp,
                            cornerRadius = 16.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Icon (Optional)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (iconBase64.isNotBlank()) "Custom icon ready" else "Pick PNG/JPG/WebP from gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (iconBase64.isNotBlank()) {
                            IconButton(
                                onClick = { iconBase64 = "" },
                                modifier = Modifier.testTag("remove_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove app icon"
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                iconPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pick_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (iconBase64.isNotBlank()) "Change" else "Icon")
                        }
                    }
                    if (iconError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = iconError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
                    .testTag("upload_title_input")
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
                label = { Text("Download / Mirror Link URL (Optional if APK attached)") },
                placeholder = { Text("https://...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_url_input")
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_description_input")
            )
        }

        item {
            OutlinedTextField(
                value = whatsNew,
                onValueChange = { whatsNew = it },
                label = { Text("What's New / Changelog (Optional)") },
                placeholder = { Text("Bug fixes, new features, speed boost...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_whats_new_input")
            )
        }

        if (isAdminMode) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pin to Featured Spotlight",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Highlight at the top of Explore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isFeatured,
                            onCheckedChange = { isFeatured = it },
                            modifier = Modifier.testTag("admin_featured_switch")
                        )
                    }
                }
            }
        }

        // APK / Vault File Picker Card (up to 50 MB chunked AES-256-GCM)
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
                                text = "APK / Encrypted Vault File",
                                style = MaterialTheme.typography.titleSmall
                            )
                            val fileSubtitle = when {
                                selectedFileSource != null ->
                                    "${selectedFileSource!!.fileName} (${formatFileSize(selectedFileSource!!.byteSize)})"
                                keepExistingFile ->
                                    "Current encrypted vault file attached (tap Replace to swap)"
                                else ->
                                    "Attach .apk or file <= 50 MB for encrypted AES-256-GCM storage"
                            }
                            Text(
                                text = fileSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedFileSource != null || keepExistingFile) {
                            IconButton(onClick = {
                                selectedFileSource = null
                                keepExistingFile = false
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove attached file"
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pick_apk_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedFileSource != null || keepExistingFile) "Replace" else "Select APK")
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

        if (isBusy && transferProgress != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { transferProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "Encrypting & uploading chunks: ${(transferProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            val hasDestination = targetUrl.isNotBlank() || selectedFileSource != null || keepExistingFile
            val canSubmit = title.isNotBlank() && description.isNotBlank() && hasDestination && !isBusy

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isEditing && onCancelEdit != null) {
                    OutlinedButton(
                        onClick = onCancelEdit,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.4f)
                            .height(52.dp)
                            .testTag("cancel_edit_button")
                    ) {
                        Text("Cancel")
                    }
                }

                Button(
                    onClick = {
                        val currentEdit = editingItem
                        if (currentEdit != null) {
                            val hadOriginalFile = currentEdit.itemType == "encrypted_file" &&
                                currentEdit.encryptedFileId.isNotBlank()
                            val removedFile = hadOriginalFile && !keepExistingFile && selectedFileSource == null
                            onSubmitEdit(
                                currentEdit,
                                title,
                                description,
                                whatsNew,
                                category,
                                version,
                                targetUrl,
                                fileSizeLabel,
                                iconBase64,
                                selectedFileSource,
                                removedFile,
                                isFeatured
                            ) {
                                onCancelEdit?.invoke()
                            }
                        } else {
                            onSubmitNew(
                                title,
                                description,
                                whatsNew,
                                category,
                                version,
                                targetUrl,
                                fileSizeLabel,
                                iconBase64,
                                selectedFileSource,
                                isFeatured
                            ) {
                                title = ""
                                description = ""
                                whatsNew = ""
                                targetUrl = ""
                                fileSizeLabel = ""
                                iconBase64 = ""
                                isFeatured = false
                                selectedFileSource = null
                                keepExistingFile = false
                            }
                        }
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("submit_upload_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isEditing) Icons.Filled.Save else Icons.Filled.Publish,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isEditing -> "Save Changes"
                                isAdminMode -> "Encrypt & Publish"
                                else -> "Submit for Approval"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
