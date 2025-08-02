package com.example.pulse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.ui.components.*
import com.example.pulse.ui.theme.*
import com.example.pulse.utils.SubscriptionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubscriptionsScreen(
    subscriptionManager: SubscriptionManager,
    onSubscriptionsChanged: () -> Unit,
    onNavigateToSelection: () -> Unit,
    onNavigateToAllNotifications: () -> Unit,
    onNavigateToReadingList: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val subscriptions by subscriptionManager.subscriptionsFlow.collectAsState()

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
                            "Manage Subscriptions",
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
                        IconButton(onClick = onNavigateToReadingList) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Reading List")
                        }
                        IconButton(onClick = onNavigateToAllNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "All Notifications")
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    ElevatedModernCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Subscription Overview",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "You have ${subscriptions.size} active subscription${if (subscriptions.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (subscriptions.isEmpty()) {
                    item {
                        ElevatedModernCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "No Subscriptions",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Add some news sources to get started with notifications.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                AnimatedGradientButton(
                                    text = "Add Subscriptions",
                                    onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Subscription management coming soon!")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    items(subscriptions) { subscription ->
                        ElevatedModernCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subscription.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Text(
                                            text = subscription.type.name.replace("_", " ").lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = subscription.sourceUrl,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnimatedOutlinedButton(
                                        text = "Remove",
                                        onClick = {
                                            subscriptionManager.removeSubscription(subscription.id)
                                            onSubscriptionsChanged()
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Removed ${subscription.name}")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        height = 40.dp,
                                        borderColor = Error,
                                        textColor = Error
                                    )

                                    AnimatedOutlinedButton(
                                        text = "Settings",
                                        onClick = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Settings for ${subscription.name}")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        height = 40.dp
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