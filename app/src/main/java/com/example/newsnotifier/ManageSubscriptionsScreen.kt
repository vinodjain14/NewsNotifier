package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
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
import com.example.newsnotifier.data.AppDefaults
import com.example.newsnotifier.data.Subscription
import com.example.newsnotifier.ui.components.*
import com.example.newsnotifier.ui.theme.*
import com.example.newsnotifier.utils.SubscriptionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubscriptionsScreen(
    subscriptionManager: SubscriptionManager,
    onSubscriptionsChanged: () -> Unit,
    onNavigateToSelection: () -> Unit,
    onNavigateToAllNotifications: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val subscriptions = remember { mutableStateListOf<Subscription>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        subscriptions.clear()
        subscriptions.addAll(subscriptionManager.getSubscriptions())
    }

    BackHandler {
        onNavigateToSelection()
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
                    title = {
                        Text(
                            "My Subscriptions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateToSelection) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Refreshing subscriptions...")
                            }
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    // Subscriptions Summary
                    SubscriptionsSummaryCard(
                        totalCount = subscriptions.size,
                        activeCount = subscriptions.size // Assuming all are active
                    )
                }

                if (subscriptions.isEmpty()) {
                    item {
                        EmptySubscriptionsCard(
                            onNavigateToSelection = onNavigateToSelection
                        )
                    }
                } else {
                    item {
                        // Quick Actions
                        QuickActionsCard(
                            onNavigateToSelection = onNavigateToSelection,
                            onNavigateToAllNotifications = onNavigateToAllNotifications
                        )
                    }

                    items(subscriptions, key = { it.id }) { subscription ->
                        ModernSubscriptionCard(
                            subscription = subscription,
                            canDelete = subscriptions.size > 2,
                            onDelete = { subToRemove ->
                                subscriptionManager.removeSubscription(subToRemove.id)
                                subscriptions.remove(subToRemove)
                                onSubscriptionsChanged()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Removed ${subToRemove.name}")
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsSummaryCard(
    totalCount: Int,
    activeCount: Int
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryItem(
                title = "Total Sources",
                value = totalCount.toString(),
                color = Primary
            )

            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            SummaryItem(
                title = "Active",
                value = activeCount.toString(),
                color = Success
            )

            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            SummaryItem(
                title = "This Week",
                value = "127", // Mock data
                color = Info
            )
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionsCard(
    onNavigateToSelection: () -> Unit,
    onNavigateToAllNotifications: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedOutlinedButton(
                    text = "Edit Sources",
                    onClick = onNavigateToSelection,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Edit
                )

                AnimatedOutlinedButton(
                    text = "All Notifications",
                    onClick = onNavigateToAllNotifications,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Notifications
                )
            }
        }
    }
}

@Composable
private fun EmptySubscriptionsCard(
    onNavigateToSelection: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "No Subscriptions Yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Add some news sources and personalities to start receiving personalized updates",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            AnimatedGradientButton(
                text = "Add Sources",
                onClick = onNavigateToSelection,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ModernSubscriptionCard(
    subscription: Subscription,
    canDelete: Boolean,
    onDelete: (Subscription) -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Source Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = when (subscription.type.name) {
                                "RSS_FEED" -> Primary.copy(alpha = 0.1f)
                                "TWITTER" -> Info.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppDefaults.getInitials(subscription.name),
                        color = when (subscription.type.name) {
                            "RSS_FEED" -> Primary
                            "TWITTER" -> Info
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = subscription.type.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = subscription.sourceUrl.take(30) + if (subscription.sourceUrl.length > 30) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Delete Button
            IconButton(
                onClick = { onDelete(subscription) },
                enabled = canDelete
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete subscription",
                    tint = if (canDelete) Error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}