package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newsnotifier.data.Subscription
import com.example.newsnotifier.data.SubscriptionType
import com.example.newsnotifier.data.AppDefaults
import com.example.newsnotifier.utils.SubscriptionManager
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.clickable // Added import for clickable modifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionSelectionScreen(
    subscriptionManager: SubscriptionManager,
    currentActiveSubscriptions: List<Subscription>,
    onSubscriptionsChanged: () -> Unit,
    onNavigateToManage: () -> Unit,
    onNavigateToWelcome: () -> Unit,
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubscriptionName by remember { mutableStateOf("") }
    var newSubscriptionSource by remember { mutableStateOf("") }
    var newSubscriptionType by remember { mutableStateOf(SubscriptionType.TWITTER) }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // State for predefined selections, initialized from currentActiveSubscriptions
    val selectedNewsChannels = remember { mutableStateListOf<AppDefaults.PredefinedSource>() }
    val selectedXPersonalities = remember { mutableStateListOf<AppDefaults.PredefinedSource>() }

    // Initialize selections based on currentActiveSubscriptions when the screen is first shown
    LaunchedEffect(currentActiveSubscriptions) {
        selectedNewsChannels.clear()
        selectedXPersonalities.clear()

        currentActiveSubscriptions.forEach { activeSub ->
            // Check if the active subscription matches any predefined news channel
            AppDefaults.newsChannels.find { it.name == activeSub.name && it.sourceUrl == activeSub.sourceUrl && it.type == activeSub.type }
                ?.let {
                    selectedNewsChannels.add(it)
                }
            // Check if the active subscription matches any predefined X personality
            AppDefaults.xPersonalities.find { it.name == activeSub.name && it.sourceUrl == activeSub.sourceUrl && it.type == activeSub.type }
                ?.let {
                    selectedXPersonalities.add(it)
                }
        }
    }


    // Check if minimum total selections are met (at least 2)
    val canSaveSelections by remember {
        derivedStateOf {
            // This now refers to the total number of items that *will be* active after saving
            // (selected predefined + existing manual subscriptions)
            val currentManualSubsCount = currentActiveSubscriptions.count { sub ->
                // A subscription is considered "manual" if it's not present in our predefined lists
                !(AppDefaults.newsChannels.any { it.name == sub.name && it.sourceUrl == sub.sourceUrl && it.type == sub.type } ||
                        AppDefaults.xPersonalities.any { it.name == sub.name && it.sourceUrl == sub.sourceUrl && it.type == sub.type })
            }
            (selectedNewsChannels.size + selectedXPersonalities.size + currentManualSubsCount) >= 2
        }
    }

    // Handle system back button/gesture
    BackHandler {
        onNavigateToWelcome()
    }

    // Custom swipe to go back (left to right)
    val swipeThreshold =
        with(LocalDensity.current) { 100.dp.toPx() } // Define a swipe distance threshold (e.g., 100dp)
    val currentDragOffset =
        remember { mutableFloatStateOf(0f) } // State to track horizontal drag offset

    val draggableState = rememberDraggableState(
        onDelta = { delta ->
            currentDragOffset.floatValue += delta
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Choose Your Feeds") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = { // Add actions for logged-in users
                    if (isLoggedIn) {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToProfile()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                }
                            )
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
                .verticalScroll(rememberScrollState()) // Make the entire column scrollable
                .draggable( // Add draggable modifier for custom swipe
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        // Check if the total drag offset to the right exceeds the threshold
                        if (currentDragOffset.floatValue > swipeThreshold) {
                            onNavigateToWelcome()
                        }
                        currentDragOffset.floatValue = 0f // Reset offset after drag stops
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Add spacing between sections
        ) {
            // X Personalities Section (Moved to top and changed to list view with Checkboxes)
            Text(
                text = "X Personalities",
                style = MaterialTheme.typography.headlineSmall, // Larger title
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // More prominent card
                shape = RoundedCornerShape(12.dp)
            ) {
                Column( // Changed from FlowRow to Column for list view
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    AppDefaults.xPersonalities.forEach { personality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { // Make the entire row clickable for selection
                                    if (selectedXPersonalities.contains(personality)) {
                                        selectedXPersonalities.remove(personality)
                                    } else {
                                        selectedXPersonalities.add(personality)
                                    }
                                }
                                .padding(vertical = 8.dp), // Add vertical padding for each item
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedXPersonalities.contains(personality),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        selectedXPersonalities.add(personality)
                                    } else {
                                        selectedXPersonalities.remove(personality)
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = personality.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // News Channels Section (Moved to bottom and changed to list view with Checkboxes)
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
                Column( // Changed from FlowRow to Column for list view
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    AppDefaults.newsChannels.forEach { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { // Make the entire row clickable for selection
                                    if (selectedNewsChannels.contains(channel)) {
                                        selectedNewsChannels.remove(channel)
                                    } else {
                                        selectedNewsChannels.add(channel)
                                    }
                                }
                                .padding(vertical = 8.dp), // Add vertical padding for each item
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedNewsChannels.contains(channel),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        selectedNewsChannels.add(channel)
                                    } else {
                                        selectedNewsChannels.remove(channel)
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // "My Subscriptions" Button
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
                        val currentAllSubscriptions = subscriptionManager.getSubscriptions()
                        val predefinedActiveSubs = currentAllSubscriptions.filter { sub ->
                            AppDefaults.newsChannels.any { it.name == sub.name && it.sourceUrl == sub.sourceUrl && it.type == sub.type } ||
                                    AppDefaults.xPersonalities.any { it.name == sub.name && it.sourceUrl == sub.sourceUrl && it.type == sub.type }
                        }.toSet()

                        val newlySelectedPredefined =
                            (selectedNewsChannels + selectedXPersonalities).toSet()

                        // Identify predefined subscriptions to remove
                        val toRemove = predefinedActiveSubs.filter { activeSub ->
                            !newlySelectedPredefined.any { selectedPredefined ->
                                selectedPredefined.name == activeSub.name && selectedPredefined.sourceUrl == activeSub.sourceUrl && selectedPredefined.type == activeSub.type
                            }
                        }

                        // Identify predefined subscriptions to add
                        val toAdd = newlySelectedPredefined.filter { selectedPredefined ->
                            !predefinedActiveSubs.any { activeSub ->
                                selectedPredefined.name == activeSub.name && selectedPredefined.sourceUrl == activeSub.sourceUrl && selectedPredefined.type == activeSub.type
                            }
                        }

                        // Perform removals
                        toRemove.forEach { sub ->
                            subscriptionManager.removeSubscription(sub.id)
                        }

                        // Perform additions
                        toAdd.forEach { predefinedSource ->
                            val newSub = Subscription(
                                id = UUID.randomUUID().toString(),
                                name = predefinedSource.name,
                                type = predefinedSource.type,
                                sourceUrl = predefinedSource.sourceUrl
                            )
                            subscriptionManager.addSubscription(newSub)
                        }

                        onSubscriptionsChanged()
                        scope.launch {
                            snackbarHostState.showSnackbar("Subscriptions updated!")
                        }
                        onNavigateToManage()
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
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newSubscriptionSource,
                                onValueChange = { newSubscriptionSource = it },
                                label = { Text("Source (e.g., https://example.com/rss, @somehandle)") },
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
                                        Text(type.name)
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
                                    onSubscriptionsChanged()
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