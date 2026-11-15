package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.CatalogConstants
import com.example.data.model.CatalogItem
import com.example.ui.components.AnnouncementBannerCard
import com.example.ui.components.CatalogItemIcon
import com.example.ui.components.FeaturedSpotlightCarousel
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.TypeFilter
import com.example.ui.viewmodel.UiState
import java.util.Locale

@Composable
fun ExploreScreen(
    catalogState: UiState<List<CatalogItem>>,
    announcements: List<Announcement>,
    featuredItems: List<CatalogItem>,
    bookmarkedIds: Set<String>,
    isSignedIn: Boolean,
    searchQuery: String,
    selectedCategory: String,
    selectedTypeFilter: TypeFilter,
    sortOption: SortOption,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectTypeFilter: (TypeFilter) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onToggleBookmark: (CatalogItem) -> Unit,
    onRequestSignIn: (String) -> Unit,
    onItemClick: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CreatorHeroCard(
                    itemCount = (catalogState as? UiState.Success)?.data?.size ?: 0,
                    onTikTokClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(CatalogConstants.CREATOR_TIKTOK_URL))
                        context.startActivity(intent)
                    }
                )
            }

            if (!isSignedIn) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                RoundedCornerShape(18.dp)
                            )
                            .testTag("guest_browse_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Browsing in Guest Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "View apps and reviews freely. Sign in with Google to download, save, rate, or upload.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    onRequestSignIn("Sign in with Google to download APKs, bookmark items, rate releases, or upload.")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("guest_banner_sign_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In")
                            }
                        }
                    }
                }
            }

            if (announcements.isNotEmpty()) {
                items(announcements.take(2), key = { "ann_${it.id}" }) { announcement ->
                    AnnouncementBannerCard(
                        announcement = announcement,
                        onActionClick = { url ->
                            if (!isSignedIn) {
                                onRequestSignIn("Sign in with Google to open announcement links.")
                            } else {
                                val normalized = if (url.startsWith("http://") || url.startsWith("https://")) {
                                    url
                                } else {
                                    "https://$url"
                                }
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                                }
                            }
                        }
                    )
                }
            }

            if (featuredItems.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "All" && selectedTypeFilter == TypeFilter.ALL) {
                item {
                    FeaturedSpotlightCarousel(
                        featuredItems = featuredItems,
                        onItemClick = onItemClick
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search catalog"
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_input")
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CatalogConstants.CATEGORIES) { category ->
                            val selected = selectedCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCategory(category) },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("category_chip_${category.lowercase()}")
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(TypeFilter.entries) { filter ->
                            val selected = selectedTypeFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectTypeFilter(filter) },
                                label = {
                                    Text(
                                        text = if (filter == TypeFilter.SAVED && bookmarkedIds.isNotEmpty()) {
                                            "Saved (${bookmarkedIds.size})"
                                        } else {
                                            filter.label
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("type_chip_${filter.name.lowercase()}")
                            )
                        }
                        item {
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                        items(SortOption.entries) { option ->
                            val selected = sortOption == option
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectSort(option) },
                                label = {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("sort_chip_${option.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            when (catalogState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = catalogState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (catalogState.data.isEmpty()) {
                        item {
                            EmptyCatalogCard(
                                isFiltered = searchQuery.isNotBlank() ||
                                    selectedCategory != "All" ||
                                    selectedTypeFilter != TypeFilter.ALL
                            )
                        }
                    } else {
                        items(catalogState.data, key = { it.id }) { item ->
                            CatalogItemCard(
                                item = item,
                                isBookmarked = bookmarkedIds.contains(item.id),
                                onToggleBookmark = { onToggleBookmark(item) },
                                onClick = { onItemClick(item) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CreatorHeroCard(
    itemCount: Int,
    onTikTokClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                RoundedCornerShape(22.dp)
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_vault_1794626287324),
            contentDescription = "KadirHub Vault Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onTikTokClick)
                        .testTag("creator_tiktok_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TikTok ${CatalogConstants.CREATOR_TIKTOK_HANDLE}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open creator TikTok profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AES-256",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "KadirHub Apps & Links",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$itemCount verified releases • encrypted vault & community ratings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CatalogItemCard(
    item: CatalogItem,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (item.isFeatured)
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                RoundedCornerShape(18.dp)
            )
            .testTag("catalog_item_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CatalogItemIcon(
                    iconBase64 = item.iconBase64,
                    category = item.category,
                    size = 50.dp,
                    cornerRadius = 14.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.isFeatured) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Featured",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${item.category} • v${item.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.fileSizeLabel.isNotBlank()) {
                            Text(
                                text = " • ${item.fileSizeLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    color = if (item.itemType == "encrypted_file")
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.itemType == "encrypted_file")
                                Icons.Filled.FolderZip
                            else
                                Icons.Filled.Link,
                            contentDescription = null,
                            tint = if (item.itemType == "encrypted_file")
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.itemType == "encrypted_file") "Vault APK" else "Link",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.itemType == "encrypted_file")
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_button_${item.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Save bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val ratingText = if (item.reviewCount > 0) {
                        String.format(Locale.US, "%.1f (%d)", item.averageRating, item.reviewCount)
                    } else {
                        "No ratings yet"
                    }
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.downloadCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val attribution = if (item.uploaderName.isNotBlank()) {
                    "By ${item.uploaderName}"
                } else {
                    item.creatorHandle
                }
                Text(
                    text = attribution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyCatalogCard(isFiltered: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_catalog_1794626285921),
                contentDescription = "Empty vault illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isFiltered) "No matching items found" else "Vault catalog is ready",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isFiltered) {
                    "Try clearing your search or switching category filters."
                } else {
                    "Sign in and upload an APK or link from the Upload tab — admin approves releases for everyone."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
