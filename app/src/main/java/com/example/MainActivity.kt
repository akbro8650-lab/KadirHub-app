package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.CatalogConstants
import com.example.data.repository.CatalogRepository
import com.example.ui.auth.SignInPromptDialog
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.components.UploadOrEditItemForm
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.ThemeMode
import com.example.ui.screens.UploadScreen
import com.example.ui.screens.VaultInfoScreen
import com.example.ui.theme.KadirHubTheme
import com.example.ui.viewmodel.CatalogViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val PREFS_NAME = "kadirhub_prefs"
private const val KEY_THEME_MODE = "theme_mode"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialModeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val initialThemeMode = runCatching {
            ThemeMode.valueOf(initialModeName ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)

        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }
            val useDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            KadirHubTheme(darkTheme = useDark) {
                KadirHubApp(
                    themeMode = themeMode,
                    onSelectThemeMode = { selected ->
                        themeMode = selected
                        prefs.edit().putString(KEY_THEME_MODE, selected.name).apply()
                    }
                )
            }
        }
    }
}

private enum class MainTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    EXPLORE(R.string.tab_explore, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
    UPLOAD(R.string.tab_upload, Icons.Filled.CloudUpload, Icons.Outlined.CloudUpload, "nav_upload"),
    ADMIN(R.string.tab_admin, Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_admin"),
    VAULT(R.string.tab_profile, Icons.Filled.Shield, Icons.Outlined.Shield, "nav_vault")
}

@Composable
fun KadirHubApp(
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onUnauthenticated = {},
            scope = scope
        )
    }

    val sessionKey = currentUser?.uid ?: "guest"
    key(sessionKey) {
        MainAppShell(
            currentUser = currentUser,
            themeMode = themeMode,
            onSelectThemeMode = onSelectThemeMode,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onSignOut = {
                signOut(
                    credentialManager = credentialManager,
                    scope = scope,
                    onSignOutComplete = { currentUser = null }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppShell(
    currentUser: FirebaseUser?,
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onAuthSuccess: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val sessionKey = currentUser?.uid ?: "guest"
    val viewModel: CatalogViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CatalogViewModel(
                    repository = CatalogRepository(db, Firebase.auth),
                    currentUserId = currentUser?.uid,
                    currentUserEmail = currentUser?.email,
                    isEmailVerified = currentUser?.isEmailVerified == true
                )
            }
        }
    )

    val isSignedIn = currentUser != null
    val isAdminAccess by viewModel.isAdminAccess.collectAsStateWithLifecycle()
    val catalogState by viewModel.filteredCatalogState.collectAsStateWithLifecycle()
    val announcements by viewModel.activeAnnouncementsState.collectAsStateWithLifecycle()
    val featuredItems by viewModel.featuredItems.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val myUploadsState by viewModel.myUploadsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val editingItem by viewModel.editingItem.collectAsStateWithLifecycle()
    val itemReviewsState by viewModel.itemReviewsState.collectAsStateWithLifecycle()
    val adminCatalogState by viewModel.adminCatalogState.collectAsStateWithLifecycle()
    val adminReviewsState by viewModel.adminReviewsState.collectAsStateWithLifecycle()
    val adminReportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val adminAnnouncementsState by viewModel.adminAnnouncementsState.collectAsStateWithLifecycle()
    val pendingApprovalCount by viewModel.pendingApprovalCount.collectAsStateWithLifecycle()
    val signInPromptReason by viewModel.signInPromptReason.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val transferProgress by viewModel.transferProgress.collectAsStateWithLifecycle()
    val pendingDownloadResult by viewModel.pendingDownloadResult.collectAsStateWithLifecycle()

    // Admin tab appears ONLY when signed in as verified kadir25.bd@gmail.com (or server admin)
    val visibleTabs = remember(isAdminAccess) {
        if (isAdminAccess) {
            listOf(MainTab.EXPLORE, MainTab.UPLOAD, MainTab.ADMIN, MainTab.VAULT)
        } else {
            listOf(MainTab.EXPLORE, MainTab.UPLOAD, MainTab.VAULT)
        }
    }

    var activeTab by remember { mutableStateOf(MainTab.EXPLORE) }
    LaunchedEffect(visibleTabs) {
        if (activeTab !in visibleTabs) {
            activeTab = MainTab.EXPLORE
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    if (signInPromptReason != null) {
        SignInPromptDialog(
            reason = signInPromptReason!!,
            onDismiss = viewModel::dismissSignInPrompt,
            onAuthSuccess = {
                viewModel.dismissSignInPrompt()
                onAuthSuccess()
            }
        )
    }

    // Full-screen Edit flow (when launched from ItemDetailScreen or AdminScreen)
    val currentEditing = editingItem
    if (currentEditing != null && activeTab != MainTab.UPLOAD && isSignedIn) {
        BackHandler(onBack = { viewModel.stopEditing() })
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit: ${currentEditing.title}", maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.stopEditing() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cancel edit"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = Modifier.fillMaxSize().widthIn(max = 720.dp)) {
                    UploadOrEditItemForm(
                        editingItem = currentEditing,
                        initialDecryptedUrl = viewModel.decryptItemUrl(currentEditing),
                        isAdminMode = isAdminAccess,
                        isBusy = isBusy,
                        transferProgress = transferProgress,
                        onCancelEdit = { viewModel.stopEditing() },
                        onSubmitEdit = { item, title, desc, whatsNew, cat, ver, url, size, icon, fileSource, removeFile, isFeatured, onSuccess ->
                            viewModel.editCatalogItem(
                                existingItem = item,
                                title = title,
                                description = desc,
                                whatsNew = whatsNew,
                                category = cat,
                                version = ver,
                                targetUrl = url,
                                fileSizeLabel = size,
                                iconBase64 = icon,
                                newFileSource = fileSource,
                                removeAttachedFile = removeFile,
                                preserveStatusIfAdmin = isAdminAccess,
                                isFeatured = isFeatured,
                                onEdited = onSuccess
                            )
                        }
                    )
                }
            }
        }
        return
    }

    val currentItem = selectedItem
    if (currentItem != null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            ItemDetailScreen(
                item = currentItem,
                reviewsState = itemReviewsState,
                currentUserId = currentUser?.uid,
                isAdminAccess = isAdminAccess,
                isBookmarked = bookmarkedIds.contains(currentItem.id),
                isBusy = isBusy,
                transferProgress = transferProgress,
                pendingDownloadResult = pendingDownloadResult,
                decryptedLinkPreview = viewModel.decryptItemUrl(currentItem),
                onBack = { viewModel.closeItemDetail() },
                onEditClick = { viewModel.startEditing(it) },
                onToggleBookmark = { viewModel.toggleBookmark(it) },
                onDownloadOrOpen = { viewModel.downloadOrOpenItem(it, context.cacheDir) },
                onClearPendingDownload = { viewModel.clearPendingDownload() },
                onSubmitReview = { item, rating, comment ->
                    viewModel.submitReview(item, rating, comment)
                },
                onFlagReview = { review, reason ->
                    viewModel.flagReview(review, reason)
                },
                onReportItem = { item, reason ->
                    viewModel.reportCatalogItem(item, reason)
                },
                onRequireSignIn = viewModel::requestSignIn,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 640.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        if (!isSignedIn) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.requestSignIn(
                                        "Sign in with Google to download, rate, bookmark, or upload releases."
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .testTag("topbar_sign_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In")
                            }
                        } else {
                            Text(
                                text = CatalogConstants.CREATOR_TIKTOK_HANDLE,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        visibleTabs.forEach { tab ->
                            val selected = activeTab == tab
                            val badgeCount = if (tab == MainTab.ADMIN) pendingApprovalCount else 0
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        icon = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                        badgeCount = badgeCount
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        visibleTabs.forEach { tab ->
                            val selected = activeTab == tab
                            val badgeCount = if (tab == MainTab.ADMIN) pendingApprovalCount else 0
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        icon = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                        badgeCount = badgeCount
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (activeTab) {
                        MainTab.EXPLORE -> ExploreScreen(
                            catalogState = catalogState,
                            announcements = announcements,
                            featuredItems = featuredItems,
                            bookmarkedIds = bookmarkedIds,
                            isSignedIn = isSignedIn,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            selectedTypeFilter = selectedTypeFilter,
                            sortOption = sortOption,
                            onSearchChange = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectCategory,
                            onSelectTypeFilter = viewModel::selectTypeFilter,
                            onSelectSort = viewModel::selectSortOption,
                            onToggleBookmark = viewModel::toggleBookmark,
                            onRequestSignIn = viewModel::requestSignIn,
                            onItemClick = { viewModel.openItemDetail(it.id) }
                        )
                        MainTab.UPLOAD -> UploadScreen(
                            isSignedIn = isSignedIn,
                            myUploadsState = myUploadsState,
                            editingItem = editingItem,
                            isBusy = isBusy,
                            transferProgress = transferProgress,
                            decryptUrl = viewModel::decryptItemUrl,
                            onRequestSignIn = viewModel::requestSignIn,
                            onStartEdit = viewModel::startEditing,
                            onCancelEdit = viewModel::stopEditing,
                            onSubmitNew = { title, desc, whatsNew, cat, ver, url, size, icon, fileSource, isFeatured, onSuccess ->
                                viewModel.publishCatalogItem(
                                    title = title,
                                    description = desc,
                                    whatsNew = whatsNew,
                                    category = cat,
                                    version = ver,
                                    targetUrl = url,
                                    fileSizeLabel = size,
                                    iconBase64 = icon,
                                    fileSource = fileSource,
                                    publishAsApproved = false,
                                    isFeatured = isFeatured,
                                    onPublished = onSuccess
                                )
                            },
                            onSubmitEdit = { item, title, desc, whatsNew, cat, ver, url, size, icon, fileSource, removeFile, isFeatured, onSuccess ->
                                viewModel.editCatalogItem(
                                    existingItem = item,
                                    title = title,
                                    description = desc,
                                    whatsNew = whatsNew,
                                    category = cat,
                                    version = ver,
                                    targetUrl = url,
                                    fileSizeLabel = size,
                                    iconBase64 = icon,
                                    newFileSource = fileSource,
                                    removeAttachedFile = removeFile,
                                    preserveStatusIfAdmin = false,
                                    isFeatured = isFeatured,
                                    onEdited = onSuccess
                                )
                            },
                            onDeleteItem = viewModel::deleteItem,
                            onOpenDetail = { viewModel.openItemDetail(it.id) }
                        )
                        MainTab.ADMIN -> {
                            if (isAdminAccess) {
                                AdminScreen(
                                    adminEmail = currentUser?.email ?: CatalogConstants.ADMIN_EMAIL,
                                    isBusy = isBusy,
                                    transferProgress = transferProgress,
                                    adminCatalogState = adminCatalogState,
                                    adminReviewsState = adminReviewsState,
                                    adminReportsState = adminReportsState,
                                    adminAnnouncementsState = adminAnnouncementsState,
                                    pendingApprovalCount = pendingApprovalCount,
                                    onPublishItem = { title, desc, whatsNew, cat, ver, url, size, icon, fileSource, isFeatured, onSuccess ->
                                        viewModel.publishCatalogItem(
                                            title = title,
                                            description = desc,
                                            whatsNew = whatsNew,
                                            category = cat,
                                            version = ver,
                                            targetUrl = url,
                                            fileSizeLabel = size,
                                            iconBase64 = icon,
                                            fileSource = fileSource,
                                            publishAsApproved = true,
                                            isFeatured = isFeatured,
                                            onPublished = onSuccess
                                        )
                                    },
                                    onOpenItemDetail = { viewModel.openItemDetail(it.id) },
                                    onEditItem = { viewModel.startEditing(it) },
                                    onSetItemStatus = viewModel::adminSetItemStatus,
                                    onToggleFeatured = viewModel::adminSetFeatured,
                                    onBulkApprovePending = viewModel::adminBulkApprovePending,
                                    onDeleteItem = viewModel::deleteItem,
                                    onPublishAnnouncement = viewModel::adminPublishAnnouncement,
                                    onToggleAnnouncement = viewModel::adminToggleAnnouncement,
                                    onDeleteAnnouncement = viewModel::adminDeleteAnnouncement,
                                    onModerateReview = { review, status ->
                                        viewModel.adminModerateReview(review, status)
                                    },
                                    onDeleteReview = viewModel::adminDeleteReview,
                                    onResolveReport = viewModel::adminResolveReport,
                                    onHideReportedTargetAndResolve = viewModel::adminHideReportedTargetAndResolve
                                )
                            }
                        }
                        MainTab.VAULT -> VaultInfoScreen(
                            isSignedIn = isSignedIn,
                            userEmail = currentUser?.email,
                            userDisplayName = currentUser?.displayName,
                            isAdminAccess = isAdminAccess,
                            myUploadsCount = (myUploadsState as? UiState.Success)?.data?.size ?: 0,
                            bookmarksCount = bookmarkedIds.size,
                            themeMode = themeMode,
                            onSelectThemeMode = onSelectThemeMode,
                            onCacheCleared = viewModel::showMessage,
                            onRequestSignIn = viewModel::requestSignIn,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    icon: ImageVector,
    contentDescription: String,
    badgeCount: Int
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(text = if (badgeCount > 99) "99+" else "$badgeCount")
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    } else {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
