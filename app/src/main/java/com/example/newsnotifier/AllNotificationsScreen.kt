package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Added import for TextAlign
import androidx.compose.ui.unit.dp
import com.example.newsnotifier.data.NotificationItem
import com.example.newsnotifier.utils.NotificationHelper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.newsnotifier.data.AppDefaults // For getInitials
import com.example.newsnotifier.ui.theme.TagBreaking
import com.example.newsnotifier.ui.theme.TagNew
import com.example.newsnotifier.ui.theme.TagText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllNotificationsScreen(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    notificationIdToFocus: String? = null // Notification ID to scroll to and highlight
) {
    val allNotifications by NotificationHelper.notificationsFlow.collectAsState() // Correct variable name
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    var selectedFilter by remember { mutableStateOf("All") } // "All", "Unread", "Breaking", "Saved"
    var showActionButtonsForNotificationId by remember { mutableStateOf<String?>(null) } // Track which notification's buttons are shown

    val filteredNotifications = remember(allNotifications, selectedFilter) {
        when (selectedFilter) {
            "All" -> allNotifications
            "Unread" -> allNotifications.filter { !it.isRead } // Assuming isRead property
            "Breaking" -> allNotifications.filter { it.isBreaking }
            "Saved" -> allNotifications.filter { it.isSaved } // Assuming isSaved property
            else -> allNotifications
        }
    }

    // Group notifications by date
    val groupedNotifications = remember(filteredNotifications) {
        filteredNotifications
            .groupBy {
                val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault())
                val today = LocalDateTime.now(ZoneId.systemDefault()).toLocalDate()
                val yesterday = today.minusDays(1)

                when (dateTime.toLocalDate()) {
                    today -> "TODAY"
                    yesterday -> "YESTERDAY"
                    else -> dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                }
            }
            .toSortedMap(compareByDescending { it }) // Sort groups by date descending
    }

    BackHandler {
        onNavigateBack()
    }

    val swipeThreshold = with(LocalDensity.current) { 100.dp.toPx() }
    val currentDragOffset = remember { mutableFloatStateOf(0f) }

    val draggableState = rememberDraggableState(
        onDelta = { delta ->
            currentDragOffset.floatValue += delta
        }
    )

    LaunchedEffect(notificationIdToFocus, allNotifications) { // Use allNotifications here
        if (notificationIdToFocus != null && allNotifications.isNotEmpty()) { // Use allNotifications here
            val index = allNotifications.indexOfFirst { it.id == notificationIdToFocus } // Use allNotifications here
            if (index != -1) {
                lazyListState.animateScrollToItem(index)
                showActionButtonsForNotificationId = notificationIdToFocus // Show buttons for focused item
                scope.launch {
                    snackbarHostState.showSnackbar("Focusing on notification: ${allNotifications[index].title}") // Use allNotifications here
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("All Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Manage Subscriptions")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle refresh */
                        scope.launch { snackbarHostState.showSnackbar("Refreshing notifications...") }
                        // Trigger data fetch again
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { /* Handle settings */
                        scope.launch { snackbarHostState.showSnackbar("Notification settings (Not implemented)") }
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        if (currentDragOffset.floatValue > swipeThreshold) {
                            onNavigateBack()
                        }
                        currentDragOffset.floatValue = 0f
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            // Pull to refresh indicator (visual only for now)
            Text(
                text = "Pull to refresh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Filter Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Unread", "Breaking", "Saved").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (filteredNotifications.isEmpty()) {
                Text(
                    text = "No notifications matching your filter.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedNotifications.forEach { (dateGroup, notificationsInGroup) ->
                        item {
                            Text(
                                text = dateGroup,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(notificationsInGroup, key = { it.id }) { notification ->
                            NotificationItemCard(
                                notification = notification,
                                showActionButtons = showActionButtonsForNotificationId == notification.id,
                                onClick = {
                                    showActionButtonsForNotificationId = if (showActionButtonsForNotificationId == notification.id) null else notification.id
                                },
                                onReadClick = { scope.launch { snackbarHostState.showSnackbar("Marked as Read: ${it.title}") } },
                                onSaveClick = { scope.launch { snackbarHostState.showSnackbar("Saved: ${it.title}") } },
                                onShareClick = { scope.launch { snackbarHostState.showSnackbar("Share: ${it.title}") } }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    NotificationHelper.clearAllNotifications()
                    scope.launch {
                        snackbarHostState.showSnackbar("All notifications cleared!")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = allNotifications.isNotEmpty() // Use allNotifications here
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear all notifications")
                Spacer(Modifier.width(8.dp))
                Text("Clear All Notifications")
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: NotificationItem,
    showActionButtons: Boolean,
    onClick: (NotificationItem) -> Unit,
    onReadClick: (NotificationItem) -> Unit,
    onSaveClick: (NotificationItem) -> Unit,
    onShareClick: (NotificationItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(notification) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initial icon (e.g., FT, AJ, G)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppDefaults.getInitials(notification.sourceName),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        notification.tag?.let { tag ->
                            Spacer(Modifier.width(8.dp))
                            val tagColor = when (tag) {
                                "NEW" -> TagNew
                                "BREAKING" -> TagBreaking
                                else -> MaterialTheme.colorScheme.secondary // Default tag color
                            }
                            AssistChip(
                                onClick = { /* Tag click action if any */ },
                                label = { Text(tag, color = TagText) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = tagColor),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    Text(
                        text = notification.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(notification.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (showActionButtons) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TextButton(onClick = { onReadClick(notification) }) { Text("Read") }
                    TextButton(onClick = { onSaveClick(notification) }) { Text("Save") }
                    TextButton(onClick = { onShareClick(notification) }) { Text("Share") }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val today = LocalDateTime.now(ZoneId.systemDefault()).toLocalDate()
    val yesterday = today.minusDays(1)

    return when (dateTime.toLocalDate()) {
        today -> dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        yesterday -> "Yesterday"
        else -> dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
}
