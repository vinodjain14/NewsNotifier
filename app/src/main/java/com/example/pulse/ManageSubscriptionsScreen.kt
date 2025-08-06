package com.example.pulse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.data.MarketFeed
import com.example.pulse.data.Subscription
import com.example.pulse.data.SubscriptionType
import com.example.pulse.ui.components.*
import com.example.pulse.ui.theme.*
import com.example.pulse.utils.SubscriptionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.UUID

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subscriptions by subscriptionManager.subscriptionsFlow.collectAsState()

    // Load market feeds from JSON
    val marketList: List<MarketFeed> = remember {
        val jsonString = context.assets.open("Finance_Market_RSS_Feeds.json").bufferedReader().use { it.readText() }
        val marketListType = object : TypeToken<List<MarketFeed>>() {}.type
        Gson().fromJson(jsonString, marketListType)
    }

    // Get all available categories
    val categories = remember { marketList.map { it.category }.distinct().sorted() }

    // Get currently subscribed categories
    val currentSubscribedCategories = remember(subscriptions) {
        subscriptions.map { it.category }.distinct()
    }

    // State for selected categories
    val selectedCategories = remember(currentSubscribedCategories) {
        mutableStateListOf<String>().apply {
            addAll(currentSubscribedCategories)
        }
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
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header section
                ElevatedModernCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose Your Interests",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Select the categories you want to receive notifications from",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${selectedCategories.size} of ${categories.size} categories selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary
                        )
                    }
                }

                // Categories list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(categories) { category ->
                        CategorySelectionCard(
                            category = category,
                            isSelected = selectedCategories.contains(category),
                            onToggle = {
                                if (selectedCategories.contains(category)) {
                                    selectedCategories.remove(category)
                                } else {
                                    selectedCategories.add(category)
                                }
                            }
                        )
                    }
                }

                // Save button
                AnimatedGradientButton(
                    text = "Save Changes",
                    onClick = {
                        // Create new subscriptions based on selected categories
                        val newSubscriptions = marketList
                            .filter { it.category in selectedCategories }
                            .map { marketFeed ->
                                Subscription(
                                    id = UUID.randomUUID().toString(),
                                    name = marketFeed.websiteName,
                                    type = SubscriptionType.RSS_FEED,
                                    sourceUrl = marketFeed.url,
                                    market = marketFeed.market,
                                    category = marketFeed.category
                                )
                            }

                        // Update subscriptions
                        subscriptionManager.overwriteSubscriptions(newSubscriptions)
                        onSubscriptionsChanged()

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "${selectedCategories.size} categories selected. Subscriptions updated!"
                            )
                        }
                    },
                    enabled = selectedCategories.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    icon = Icons.Default.Check
                )
            }
        }
    }
}

@Composable
private fun CategorySelectionCard(
    category: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    ElevatedModernCard(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (isSelected) {
            Primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        }
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
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isSelected) "Selected" else "Tap to select",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        Primary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}