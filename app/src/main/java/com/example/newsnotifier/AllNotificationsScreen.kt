package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Refresh // Added import for refresh icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newsnotifier.data.NotificationItem
import com.example.newsnotifier.utils.DataFetcher
import com.example.newsnotifier.utils.NotificationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotificationsScreen(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    notificationIdToFocus: String? = null
) {
    val notifications by NotificationHelper.notificationsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    // State for managing refresh indicator visibility
    var isRefreshing by remember { mutableStateOf(false) }

    // Scroll to focused item whenever notifications list changes or notificationIdToFocus changes
    LaunchedEffect(notifications, notificationIdToFocus) {
        notificationIdToFocus?.let { idToFocus ->
            val index = notifications.indexOfFirst { it.id == idToFocus }
            if (index != -1) {
                scope.launch {
                    lazyListState.animateScrollToItem(index)
                }
            }
        }
    }

    // Handle system back button/gesture
    BackHandler {
        onNavigateBack()
    }

    // Custom swipe to go back (left to right)
    val swipeThreshold = with(LocalDensity.current) { 100.dp.toPx() }
    val currentDragOffset = remember { mutableFloatStateOf(0f) }

    val draggableState = rememberDraggableState(
        onDelta = { delta ->
            currentDragOffset.floatValue += delta
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("All Notifications") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Refresh Button
                    IconButton(
                        onClick = {
                            if (!isRefreshing) { // Prevent multiple clicks while refreshing
                                scope.launch {
                                    isRefreshing = true
                                    val newContentFound = DataFetcher.fetchAndNotifyNewContent()
                                    if (newContentFound) {
                                        snackbarHostState.showSnackbar("New content found and notified!")
                                    } else {
                                        snackbarHostState.showSnackbar("No new content found.")
                                    }
                                    isRefreshing = false
                                }
                            }
                        },
                        enabled = !isRefreshing // Disable button while refreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh notifications")
                        }
                    }

                    // Clear All Button
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = {
                            NotificationHelper.clearAllNotifications()
                            scope.launch { snackbarHostState.showSnackbar("All notifications cleared.") }
                        }) {
                            Icon(Icons.Filled.ClearAll, contentDescription = "Clear all notifications")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (currentDragOffset.floatValue > swipeThreshold) {
                            onNavigateBack()
                        }
                        currentDragOffset.floatValue = 0f
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (notifications.isEmpty()) {
                Text(
                    text = "No past notifications to display.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            isFocused = notification.id == notificationIdToFocus
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationItem, isFocused: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
