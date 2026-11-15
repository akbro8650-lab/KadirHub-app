package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogConstants
import com.example.ui.components.formatFileSize
import java.io.File

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

@Composable
fun VaultInfoScreen(
    isSignedIn: Boolean,
    userEmail: String?,
    userDisplayName: String?,
    isAdminAccess: Boolean,
    myUploadsCount: Int,
    bookmarksCount: Int,
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onCacheCleared: (String) -> Unit,
    onRequestSignIn: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cacheBytes by remember {
        mutableLongStateOf(computeVaultCacheSize(context.cacheDir))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
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
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = "Creator verified badge",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Created by ${CatalogConstants.CREATOR_TIKTOK_HANDLE}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Official KadirHub Creator & Moderator",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(CatalogConstants.CREATOR_TIKTOK_URL)
                                )
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_tiktok_button")
                        ) {
                            Text("Follow ${CatalogConstants.CREATOR_TIKTOK_HANDLE} on TikTok")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Appearance & Vault Storage",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { onSelectThemeMode(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Decrypted Temp Cache",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (cacheBytes > 0L) "${formatFileSize(cacheBytes)} stored locally" else "Cache is clean (0 B)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val cleared = clearVaultCache(context.cacheDir)
                                    cacheBytes = computeVaultCacheSize(context.cacheDir)
                                    onCacheCleared(
                                        if (cleared > 0L) "Cleared ${formatFileSize(cleared)} of decrypted vault temp files."
                                        else "Vault temp cache is already empty."
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("clear_vault_cache_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CleaningServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Encrypted Storage & Moderation",
                            style = MaterialTheme.typography.titleMedium
                        )
                        SecuritySpecRow(
                            icon = Icons.Filled.EnhancedEncryption,
                            title = "AES-256-GCM Authenticated Encryption",
                            subtitle = "All uploaded file blobs and target URLs are encrypted with a unique 96-bit IV and 128-bit authentication tag before storage."
                        )
                        SecuritySpecRow(
                            icon = Icons.Filled.Fingerprint,
                            title = "SHA-256 Payload Integrity",
                            subtitle = "Every file and link stores a SHA-256 digest verified automatically upon decryption and download."
                        )
                        SecuritySpecRow(
                            icon = Icons.Filled.GppGood,
                            title = "Admin Approval & Zero-Trust Moderation",
                            subtitle = "Community APK uploads (up to 50 MB) and links stay encrypted in a pending queue until approved by ${CatalogConstants.ADMIN_EMAIL}."
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isSignedIn) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Guest Mode",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Sign in with Google to unlock downloads, uploads, bookmarks, and reviews.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onRequestSignIn("Sign in with Google to interact, bookmark, review, or upload releases.")
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vault_sign_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userDisplayName ?: "Signed-in Member",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = userEmail ?: "Google Account",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = if (isAdminAccess) "Admin" else "Member",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "My Uploads: $myUploadsCount • Saved Bookmarks: $bookmarksCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun computeVaultCacheSize(cacheDir: File): Long {
    val dir = File(cacheDir, "vault_downloads")
    if (!dir.exists() || !dir.isDirectory) return 0L
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

private fun clearVaultCache(cacheDir: File): Long {
    val before = computeVaultCacheSize(cacheDir)
    val dir = File(cacheDir, "vault_downloads")
    if (dir.exists()) {
        dir.deleteRecursively()
    }
    return before
}

@Composable
private fun SecuritySpecRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
