package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications // Added import for notification icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newsnotifier.data.Subscription
import com.example.newsnotifier.utils.SubscriptionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubscriptionsScreen(
    subscriptionManager: SubscriptionManager,
    onSubscriptionsChanged: () -> Unit,
    onNavigateToSelection: () -> Unit,
    onNavigateToAllNotifications: () -> Unit, // NEW: Callback for notifications screen
    snackbarHostState: SnackbarHostState // Pass SnackbarHostState
) {
    // This list will be observed for changes in SharedPreferences
    val subscriptions = remember { mutableStateListOf<Subscription>() }
    val scope = rememberCoroutineScope()

    // Update the list whenever the component is composed or recomposed,
    // ensuring it reflects the latest state from SubscriptionManager.
    LaunchedEffect(Unit) {
        subscriptions.clear()
        subscriptions.addAll(subscriptionManager.getSubscriptions())
    }

    // Handle system back button/gesture
    BackHandler {
        onNavigateToSelection()
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
                title = { Text("Your Subscriptions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToAllNotifications) { // NEW: Notification icon
                        Icon(Icons.Filled.Notifications, contentDescription = "All Notifications")
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
                    onDragStopped = { velocity ->
                        // Check if the total drag offset to the right exceeds the threshold
                        if (currentDragOffset.floatValue > swipeThreshold) {
                            onNavigateToSelection()
                        }
                        currentDragOffset.floatValue = 0f
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (subscriptions.isEmpty()) {
                Text(
                    text = "No active subscriptions. Go back to select some!",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(subscriptions, key = { it.id }) { subscription ->
                        // Determine if delete button should be enabled
                        val canDelete = subscriptions.size > 2
                        SubscriptionCard(
                            subscription = subscription,
                            canDelete = canDelete
                        ) { subToRemove ->
                            subscriptionManager.removeSubscription(subToRemove.id)
                            subscriptions.remove(subToRemove)
                            onSubscriptionsChanged()
                            scope.launch {
                                snackbarHostState.showSnackbar("Removed ${subToRemove.name}")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNavigateToSelection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Subscriptions")
            }
        }
    }
}

@Composable
fun SubscriptionCard(subscription: Subscription, canDelete: Boolean, onDelete: (Subscription) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Type: ${subscription.type.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Source: ${subscription.sourceUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onDelete(subscription) },
                enabled = canDelete
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete subscription",
                    tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}
