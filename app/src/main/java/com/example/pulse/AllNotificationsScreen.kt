package com.example.pulse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.data.NotificationItem
import com.example.pulse.ui.components.ElevatedModernCard
import com.example.pulse.ui.components.GradientDirection
import com.example.pulse.ui.components.StaticGradientBackground
import com.example.pulse.ui.theme.*
import com.example.pulse.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotificationsScreen(
    onNavigateToManageSubscriptions: () -> Unit,
    onNavigateToMyProfile: () -> Unit,
    onNavigateToReadingList: () -> Unit,
    onNavigateToNotificationDetail: (NotificationItem) -> Unit,
    snackbarHostState: SnackbarHostState,
    notificationIdToFocus: String? = null,
    readingListManager: ReadingListManager,
    subscriptionManager: SubscriptionManager
) {
    val allNotifications by NotificationHelper.notificationsFlow.collectAsState()
    val readingList by readingListManager.readingListFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Search functionality
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(notificationIdToFocus, allNotifications) {
        if (notificationIdToFocus != null) {
            val focusedNotification = allNotifications.find { it.id == notificationIdToFocus }
            if (focusedNotification != null) {
                expandedGroups = expandedGroups + focusedNotification.category
            }
        }
    }

    // Filter notifications based on search query
    val filteredNotifications = remember(allNotifications, searchQuery) {
        if (searchQuery.isBlank()) {
            allNotifications
        } else {
            allNotifications.filter { notification ->
                notification.title.contains(searchQuery, ignoreCase = true) ||
                        notification.message.contains(searchQuery, ignoreCase = true) ||
                        notification.sourceName.contains(searchQuery, ignoreCase = true) ||
                        notification.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val groupedNotifications = filteredNotifications
        .groupBy { it.category }
        .toSortedMap(compareBy { it })

    // Disable back handler
    BackHandler(enabled = true) {
        // Do nothing
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Notifications?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        NotificationHelper.clearAllNotifications()
                        scope.launch { snackbarHostState.showSnackbar("All notifications cleared.") }
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    StaticGradientBackground(
        colors = listOf(BackgroundLight, Color.White),
        direction = GradientDirection.TopToBottom
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Pulse Notifications", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                    actions = {
                        // Search toggle button
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) {
                                searchQuery = ""
                                keyboardController?.hide()
                            }
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (isSearchActive) "Close Search" else "Search Notifications"
                            )
                        }

                        IconButton(onClick = {
                            scope.launch {
                                isLoading = true
                                val notifications = withContext(Dispatchers.IO) {
                                    DataFetcher.fetchAllFeeds()
                                }
                                NotificationHelper.addNotifications(notifications)
                                isLoading = false
                                snackbarHostState.showSnackbar("Refreshed!")
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Notifications")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Manage Subscriptions") },
                                    onClick = {
                                        onNavigateToManageSubscriptions()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.List, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("My Profile") },
                                    onClick = {
                                        onNavigateToMyProfile()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reading List") },
                                    onClick = {
                                        onNavigateToReadingList()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) }
                                )
                                if (allNotifications.isNotEmpty()) {
                                    Divider()
                                    DropdownMenuItem(
                                        text = { Text("Clear All") },
                                        onClick = {
                                            showClearConfirmDialog = true
                                            showMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                // Search bar at the bottom
                if (isSearchActive) {
                    ElevatedModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search notifications...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                cursorColor = Primary
                            )
                        )

                        // Search results info
                        if (searchQuery.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (filteredNotifications.isEmpty()) {
                                        "No results found"
                                    } else {
                                        "${filteredNotifications.size} result${if (filteredNotifications.size != 1) "s" else ""} found"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (filteredNotifications.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            // Expand all groups that have search results
                                            expandedGroups = groupedNotifications.keys.toSet()
                                        }
                                    ) {
                                        Text(
                                            text = "Expand All",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->

            // Auto-focus search field when activated
            LaunchedEffect(isSearchActive) {
                if (isSearchActive) {
                    focusRequester.requestFocus()
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Show search hint when search is active but no query
                    if (isSearchActive && searchQuery.isEmpty()) {
                        item {
                            SearchHintCard()
                        }
                    }

                    if (groupedNotifications.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            NoSearchResultsCard(searchQuery = searchQuery)
                        }
                    } else if (groupedNotifications.isEmpty() && searchQuery.isEmpty()) {
                        item {
                            EmptyNotificationsCard(filter = "All")
                        }
                    } else {
                        groupedNotifications.forEach { (category, notifications) ->
                            item {
                                val isExpanded = expandedGroups.contains(category)
                                val unreadCount = notifications.count { !it.isRead }

                                NotificationGroupHeader(
                                    categoryName = category,
                                    totalCount = notifications.size,
                                    unreadCount = unreadCount,
                                    isExpanded = isExpanded,
                                    onToggle = {
                                        expandedGroups = if (isExpanded) {
                                            expandedGroups - category
                                        } else {
                                            expandedGroups + category
                                        }
                                    },
                                    isSearchResult = searchQuery.isNotEmpty()
                                )
                            }

                            if (expandedGroups.contains(category)) {
                                items(notifications, key = { it.id }) { notification ->
                                    // Check if notification is in reading list
                                    val isBookmarked = readingList.any { it.id == notification.id }

                                    ModernNotificationCard(
                                        notification = notification,
                                        isBookmarked = isBookmarked,
                                        searchQuery = searchQuery,
                                        onNotificationClick = {
                                            // Navigate to detail screen
                                            onNavigateToNotificationDetail(notification)
                                        },
                                        onBookmarkClick = {
                                            if (isBookmarked) {
                                                readingListManager.removeFromReadingList(notification.id)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Removed from reading list")
                                                }
                                            } else {
                                                readingListManager.addToReadingList(notification)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Added to reading list")
                                                }
                                            }
                                        }
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

@Composable
private fun SearchHintCard() {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Search Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Search by title, content, source, or category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NoSearchResultsCard(searchQuery: String) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No Results Found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "No notifications match \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Try different keywords or check your spelling",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NotificationGroupHeader(
    categoryName: String,
    totalCount: Int,
    unreadCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isSearchResult: Boolean = false
) {
    ElevatedModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        containerColor = if (isSearchResult) Primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSearchResult) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search result",
                            modifier = Modifier.size(16.dp),
                            tint = Primary
                        )
                    }
                }
                if (unreadCount > 0) {
                    Text(
                        text = "$unreadCount unread of $totalCount total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "All $totalCount read",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
        }
    }
}

@Composable
private fun ModernNotificationCard(
    notification: NotificationItem,
    isBookmarked: Boolean,
    searchQuery: String = "",
    onNotificationClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val cardColor = if (notification.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = cardColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification.sourceName.take(2).uppercase(),
                        color = Primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNotificationClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 4.dp, end = 8.dp)
                        )

                        // Read indicator and search match indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (searchQuery.isNotEmpty() && (
                                        notification.title.contains(searchQuery, ignoreCase = true) ||
                                                notification.message.contains(searchQuery, ignoreCase = true)
                                        )) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Success)
                                )
                            }

                            if (!notification.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Primary)
                                )
                            }
                        }
                    }

                    if (notification.message.isNotBlank()) {
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = TimeUtils.formatRelativeTime(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save/Bookmark button
                IconButton(
                    onClick = onBookmarkClick
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove from reading list" else "Add to reading list",
                        tint = if (isBookmarked) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Read status indicator text
                if (notification.isRead) {
                    Text(
                        text = "Read",
                        style = MaterialTheme.typography.labelSmall,
                        color = Success,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                } else {
                    TextButton(
                        onClick = onNotificationClick
                    ) {
                        Text(
                            text = "Mark as Read",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsCard(filter: String) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No ${filter.lowercase()} notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}