package com.example.newsnotifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsnotifier.data.AppDefaults
import com.example.newsnotifier.data.Subscription
import com.example.newsnotifier.data.SubscriptionType
import com.example.newsnotifier.ui.components.*
import com.example.newsnotifier.ui.theme.*
import com.example.newsnotifier.utils.SubscriptionManager
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
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

    val selectedNewsChannels = remember { mutableStateListOf<AppDefaults.PredefinedSource>() }
    val selectedXPersonalities = remember { mutableStateListOf<AppDefaults.PredefinedSource>() }

    LaunchedEffect(currentActiveSubscriptions) {
        selectedNewsChannels.clear()
        selectedXPersonalities.clear()

        currentActiveSubscriptions.forEach { activeSub ->
            AppDefaults.newsChannels.find { it.name == activeSub.name && it.sourceUrl == activeSub.sourceUrl }
                ?.let { selectedNewsChannels.add(it) }
            AppDefaults.xPersonalities.find { it.name == activeSub.name && it.sourceUrl == activeSub.sourceUrl }
                ?.let { selectedXPersonalities.add(it) }
        }
    }

    // *** FIX 1: Changed minimum selection to 1 ***
    val canSaveSelections by remember {
        derivedStateOf {
            (selectedNewsChannels.size + selectedXPersonalities.size) >= 1
        }
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
                            "Choose Your Feeds",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Settings, contentDescription = "Profile")
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
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    // Header Section
                    ElevatedModernCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Personalize Your Experience",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // *** FIX 1: Updated UI text ***
                            Text(
                                text = "Select at least 1 source to get started with personalized news updates",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    // Selection Progress
                    val totalSelected = selectedNewsChannels.size + selectedXPersonalities.size
                    SelectionProgress(
                        current = totalSelected,
                        // *** FIX 1: Updated required count ***
                        required = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // News Channels Section
                    SourceSection(
                        title = "📰 News Channels",
                        subtitle = "Trusted news sources",
                        sources = AppDefaults.newsChannels,
                        selectedSources = selectedNewsChannels,
                        onSourceToggle = { source ->
                            if (selectedNewsChannels.contains(source)) {
                                selectedNewsChannels.remove(source)
                            } else {
                                selectedNewsChannels.add(source)
                            }
                        }
                    )
                }

                item {
                    // X Personalities Section
                    SourceSection(
                        title = "🐦 X Personalities",
                        subtitle = "Follow influential voices",
                        sources = AppDefaults.xPersonalities,
                        selectedSources = selectedXPersonalities,
                        onSourceToggle = { source ->
                            if (selectedXPersonalities.contains(source)) {
                                selectedXPersonalities.remove(source)
                            } else {
                                selectedXPersonalities.add(source)
                            }
                        }
                    )
                }

                item {
                    // Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedGradientButton(
                            text = "Save Selections",
                            onClick = {
                                // *** FIX 2: Added login check to prevent crash ***
                                if (isLoggedIn) {
                                    scope.launch {
                                        val allSelections = (selectedNewsChannels + selectedXPersonalities).map { predefinedSource ->
                                            Subscription(
                                                id = UUID.randomUUID().toString(),
                                                name = predefinedSource.name,
                                                type = predefinedSource.type,
                                                sourceUrl = predefinedSource.sourceUrl
                                            )
                                        }
                                        subscriptionManager.overwriteSubscriptions(allSelections)
                                        onSubscriptionsChanged()
                                        snackbarHostState.showSnackbar("Subscriptions saved!")
                                        onNavigateToManage()
                                    }
                                } else {
                                    // Show a message if the user is not logged in
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please log in to save your selections.")
                                    }
                                }
                            },
                            enabled = canSaveSelections,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Check
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnimatedOutlinedButton(
                                text = "Add Manually",
                                onClick = { showAddDialog = true },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Add
                            )

                            AnimatedOutlinedButton(
                                text = "My Subscriptions",
                                onClick = onNavigateToManage,
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Settings
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        if (showAddDialog) {
            AddSubscriptionDialog(
                name = newSubscriptionName,
                onNameChange = { newSubscriptionName = it },
                source = newSubscriptionSource,
                onSourceChange = { newSubscriptionSource = it },
                type = newSubscriptionType,
                onTypeChange = { newSubscriptionType = it },
                onConfirm = {
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
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
private fun SelectionProgress(
    current: Int,
    required: Int,
    modifier: Modifier = Modifier
) {
    ElevatedModernCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Selection Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$current of $required minimum source selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (current >= required) Success else MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = current.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (current >= required) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SourceSection(
    title: String,
    subtitle: String,
    sources: List<AppDefaults.PredefinedSource>,
    selectedSources: List<AppDefaults.PredefinedSource>,
    onSourceToggle: (AppDefaults.PredefinedSource) -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(sources) { source ->
                    SourceChip(
                        source = source,
                        isSelected = selectedSources.contains(source),
                        onClick = { onSourceToggle(source) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceChip(
    source: AppDefaults.PredefinedSource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    ModernCard(
        onClick = onClick,
        backgroundColor = backgroundColor,
        borderColor = if (isSelected) Primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = source.name,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            source.tag?.let { tag ->
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Primary,
                    modifier = Modifier
                        .background(
                            color = if (isSelected) Color.White.copy(alpha = 0.2f) else Primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AddSubscriptionDialog(
    name: String,
    onNameChange: (String) -> Unit,
    source: String,
    onSourceChange: (String) -> Unit,
    type: SubscriptionType,
    onTypeChange: (SubscriptionType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Custom Source",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Source Name") },
                    placeholder = { Text("e.g., Custom Blog, @Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = source,
                    onValueChange = onSourceChange,
                    label = { Text("Source URL/Handle") },
                    placeholder = { Text("e.g., https://example.com/rss, @handle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        "Source Type:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SubscriptionType.values().forEach { subscriptionType ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = (subscriptionType == type),
                                    onClick = { onTypeChange(subscriptionType) }
                                )
                                Text(
                                    text = subscriptionType.name.replace("_", " "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AnimatedGradientButton(
                text = "Add Source",
                onClick = onConfirm,
                modifier = Modifier.height(40.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
