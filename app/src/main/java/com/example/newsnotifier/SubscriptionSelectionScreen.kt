package com.example.newsnotifier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newsnotifier.data.Subscription
import com.example.newsnotifier.data.SubscriptionType // Added import for SubscriptionType
import com.example.newsnotifier.data.AppDefaults // Import AppDefaults
import com.example.newsnotifier.utils.SubscriptionManager
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionSelectionScreen(
    subscriptionManager: SubscriptionManager,
    currentActiveSubscriptions: List<Subscription>, // Parameter to receive active subscriptions
    onSubscriptionsChanged: () -> Unit,
    onNavigateToManage: () -> Unit,
    onNavigateToWelcome: () -> Unit, // Added missing parameter
    isLoggedIn: Boolean, // Added missing parameter
    onLogout: () -> Unit, // Added missing parameter
    onNavigateToProfile: () -> Unit, // Added missing parameter
    snackbarHostState: SnackbarHostState // Added missing parameter
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubscriptionName by remember { mutableStateOf("") }
    var newSubscriptionSource by remember { mutableStateOf("") }
    var newSubscriptionType by remember { mutableStateOf(SubscriptionType.TWITTER) }
    val scope = rememberCoroutineScope()

    // State for predefined selections, initialized from currentActiveSubscriptions
    val selectedNewsChannels = remember { mutableStateListOf<AppDefaults.PredefinedSource>() }
    val selectedXPersonalities = remember { mutableStateListOf<AppDefaults.PredefinedSource>() }

    // Initialize selections based on currentActiveSubscriptions when the screen is first shown
    // or when currentActiveSubscriptions changes (e.g., navigating back from manage screen)
    LaunchedEffect(currentActiveSubscriptions) {
        selectedNewsChannels.clear()
        selectedXPersonalities.clear()

        currentActiveSubscriptions.forEach { activeSub ->
            AppDefaults.newsChannels.find { it.name == activeSub.name && it.sourceUrl == activeSub.sourceUrl }
                ?.let {
                    selectedNewsChannels.add(it)
                }
            AppDefaults.xPersonalities.find { it.name == activeSub.name && it.sourceUrl == activeSub.sourceUrl }
                ?.let {
                    selectedXPersonalities.add(it)
                }
        }
    }


    // Check if minimum total selections are met (at least 2)
    val canSaveSelections by remember {
        derivedStateOf {
            (selectedNewsChannels.size + selectedXPersonalities.size) >= 2
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Choose Your Feeds") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Make the entire column scrollable
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Add spacing between sections
        ) {
            // General instruction
            Text(
                text = "Select at least 2 sources in total to get started:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // News Channels Section
            Text(
                text = "News Channels",
                style = MaterialTheme.typography.headlineSmall, // Larger title
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // More prominent card
                shape = RoundedCornerShape(12.dp)
            ) {
                // Use FlowRow directly inside Card for chips, no need for LazyColumn here if not too many items
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), // Increased padding
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppDefaults.newsChannels.forEach { channel ->
                        FilterChip(
                            selected = selectedNewsChannels.contains(channel),
                            onClick = {
                                if (selectedNewsChannels.contains(channel)) {
                                    selectedNewsChannels.remove(channel)
                                } else {
                                    selectedNewsChannels.add(channel)
                                }
                            },
                            label = { Text(channel.name) },
                            leadingIcon = if (selectedNewsChannels.contains(channel)) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected") }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // X Personalities Section
            Text(
                text = "X Personalities",
                style = MaterialTheme.typography.headlineSmall, // Larger title - Corrected typo
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // More prominent card
                shape = RoundedCornerShape(12.dp)
            ) {
                // Use FlowRow directly inside Card for chips
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), // Increased padding
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppDefaults.xPersonalities.forEach { personality ->
                        FilterChip(
                            selected = selectedXPersonalities.contains(personality),
                            onClick = {
                                if (selectedXPersonalities.contains(personality)) {
                                    selectedXPersonalities.remove(personality)
                                } else {
                                    selectedXPersonalities.add(personality)
                                }
                            },
                            label = { Text(personality.name) },
                            leadingIcon = if (selectedXPersonalities.contains(personality)) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected") }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // "My Subscriptions" Button - NEW
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNavigateToManage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View My Subscriptions")
            }
            Spacer(Modifier.height(8.dp))


            // Buttons at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Add Manually" Button
                ElevatedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add manually")
                    Spacer(Modifier.width(4.dp))
                    Text("Add Manually")
                }

                // "Save Selections" Button
                Button(
                    onClick = {
                        // Clear existing subscriptions before adding new ones from selection
                        // This ensures that deselected items are removed
                        subscriptionManager.getSubscriptions().forEach { sub ->
                            subscriptionManager.removeSubscription(sub.id)
                        }

                        // Add selected news channels
                        selectedNewsChannels.forEach { predefinedSource ->
                            val newSub = Subscription(
                                id = UUID.randomUUID().toString(),
                                name = predefinedSource.name,
                                type = predefinedSource.type,
                                sourceUrl = predefinedSource.sourceUrl
                            )
                            subscriptionManager.addSubscription(newSub)
                        }
                        // Add selected X personalities
                        selectedXPersonalities.forEach { predefinedSource ->
                            val newSub = Subscription(
                                id = UUID.randomUUID().toString(),
                                name = predefinedSource.name,
                                type = predefinedSource.type,
                                sourceUrl = predefinedSource.sourceUrl
                            )
                            subscriptionManager.addSubscription(newSub)
                        }

                        onSubscriptionsChanged() // Re-schedule worker with new subscriptions
                        scope.launch {
                            snackbarHostState.showSnackbar("Subscriptions saved!")
                        }
                        onNavigateToManage() // Navigate to manage screen
                    },
                    enabled = canSaveSelections,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Save Selections")
                }
            }


            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("Add New Subscription Manually") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newSubscriptionName,
                                onValueChange = { newSubscriptionName = it },
                                label = { Text("Name (e.g., Custom Blog, @AnotherUser)") },
                                singleLine = true, // Added singleLine
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newSubscriptionSource,
                                onValueChange = { newSubscriptionSource = it },
                                label = { Text("Source (e.g., https://example.com/rss, @somehandle)") },
                                singleLine = true, // Added singleLine
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Type:")
                                Spacer(Modifier.width(8.dp))
                                SubscriptionType.entries.forEach { type ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = (type == newSubscriptionType),
                                            onClick = { newSubscriptionType = type }
                                        )
                                        Text(type.name) // .name is correct for enum entries
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newSubscriptionName.isNotBlank() && newSubscriptionSource.isNotBlank()) {
                                    val newSub = Subscription(
                                        id = UUID.randomUUID().toString(),
                                        name = newSubscriptionName,
                                        type = newSubscriptionType,
                                        sourceUrl = newSubscriptionSource
                                    )
                                    subscriptionManager.addSubscription(newSub)
                                    onSubscriptionsChanged() // Notify MainActivity to re-schedule worker
                                    newSubscriptionName = ""
                                    newSubscriptionSource = ""
                                    showAddDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Added ${newSub.name}")
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please fill all fields.")
                                    }
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}