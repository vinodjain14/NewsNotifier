package com.example.pulse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.data.NotificationItem
import com.example.pulse.ui.components.ElevatedModernCard
import com.example.pulse.ui.components.GradientDirection
import com.example.pulse.ui.components.StaticGradientBackground
import com.example.pulse.ui.components.SwipeableActionsBox
import com.example.pulse.ui.theme.*
import com.example.pulse.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotificationsScreen(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    notificationIdToFocus: String? = null,
    onNavigateToReadingList: () -> Unit,
    readingListManager: ReadingListManager,
    subscriptionManager: SubscriptionManager
) {
    val allNotifications by NotificationHelper.notificationsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(notificationIdToFocus, allNotifications) {
        if (notificationIdToFocus != null) {
            val focusedNotification = allNotifications.find { it.id == notificationIdToFocus }
            if (focusedNotification != null) {
                expandedGroups = expandedGroups + focusedNotification.sourceName
            }
        }
    }

    val groupedNotifications = allNotifications
        .groupBy { it.sourceName }
        .toSortedMap(compareBy { it })

    BackHandler {
        onNavigateBack()
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
                    title = { Text("All Notifications", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
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
                        if (allNotifications.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirmDialog = true }) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear All Notifications")
                            }
                        }
                        IconButton(onClick = onNavigateToReadingList) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Reading List")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->
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
                    if (groupedNotifications.isEmpty()) {
                        item {
                            EmptyNotificationsCard(filter = "All")
                        }
                    } else {
                        groupedNotifications.forEach { (sourceName, notifications) ->
                            item {
                                val isExpanded = expandedGroups.contains(sourceName)
                                NotificationGroupHeader(
                                    sourceName = sourceName,
                                    count = notifications.size,
                                    isExpanded = isExpanded,
                                    onToggle = {
                                        expandedGroups = if (isExpanded) {
                                            expandedGroups - sourceName
                                        } else {
                                            expandedGroups + sourceName
                                        }
                                    }
                                )
                            }

                            if (expandedGroups.contains(sourceName)) {
                                items(notifications, key = { it.id }) { notification ->
                                    SwipeableNotificationCard(
                                        notification = notification,
                                        onRead = {
                                            NotificationHelper.markAsRead(notification.id)
                                            scope.launch { snackbarHostState.showSnackbar("Marked as read.") }
                                        },
                                        onSave = {
                                            readingListManager.addToReadingList(notification)
                                            scope.launch { snackbarHostState.showSnackbar("Saved to reading list.") }
                                        },
                                        onDelete = {
                                            NotificationHelper.deleteNotification(notification.id)
                                            scope.launch { snackbarHostState.showSnackbar("Notification deleted.") }
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
private fun NotificationGroupHeader(
    sourceName: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = sourceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
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
private fun SwipeableNotificationCard(
    notification: NotificationItem,
    onRead: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    SwipeableActionsBox(
        actions = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Error.copy(alpha = 0.8f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRead) {
                    Icon(Icons.Default.Drafts, contentDescription = "Mark as Read", tint = Color.White)
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Save", tint = Color.White)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
    ) {
        ModernNotificationCard(notification = notification)
    }
}

@Composable
private fun ModernNotificationCard(
    notification: NotificationItem
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (notification.message.isNotBlank()) {
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    // --- ADDED TIMESTAMP ---
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = TimeUtils.formatRelativeTime(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    // --- END TIMESTAMP ---
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
