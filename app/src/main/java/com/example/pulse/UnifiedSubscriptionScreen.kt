package com.example.pulse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// Screen mode to determine behavior
enum class SubscriptionScreenMode {
    ONBOARDING,    // First-time user setup
    MANAGEMENT     // Managing existing subscriptions
}

// Enhanced category visual mapping with descriptions
private object UnifiedCategoryVisuals {
    private val categoryMap = mapOf(
        "Finance" to CategoryInfo(
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF2E7D32),
            description = "Market trends, stocks, and financial news"
        ),
        "Technology" to CategoryInfo(
            icon = Icons.Default.Computer,
            color = Color(0xFF1976D2),
            description = "Latest tech innovations and gadgets"
        ),
        "Sports" to CategoryInfo(
            icon = Icons.Default.Sports,
            color = Color(0xFFE65100),
            description = "Games, scores, and sports updates"
        ),
        "Politics" to CategoryInfo(
            icon = Icons.Default.Gavel,
            color = Color(0xFF7B1FA2),
            description = "Government news and policy updates"
        ),
        "Health" to CategoryInfo(
            icon = Icons.Default.LocalHospital,
            color = Color(0xFFD32F2F),
            description = "Medical news and health tips"
        ),
        "Entertainment" to CategoryInfo(
            icon = Icons.Default.Movie,
            color = Color(0xFFF57C00),
            description = "Movies, TV shows, and celebrity news"
        ),
        "Business" to CategoryInfo(
            icon = Icons.Default.Business,
            color = Color(0xFF388E3C),
            description = "Corporate news and market analysis"
        ),
        "Science" to CategoryInfo(
            icon = Icons.Default.Science,
            color = Color(0xFF0288D1),
            description = "Research breakthroughs and discoveries"
        ),
        "World" to CategoryInfo(
            icon = Icons.Default.Public,
            color = Color(0xFF5D4037),
            description = "International news and global events"
        ),
        "Breaking" to CategoryInfo(
            icon = Icons.Default.Emergency,
            color = Color(0xFFD32F2F),
            description = "Urgent and breaking news alerts"
        ),
        "General" to CategoryInfo(
            icon = Icons.Default.Article,
            color = Color(0xFF616161),
            description = "General news and miscellaneous updates"
        ),
        "Companies" to CategoryInfo(
            icon = Icons.Default.Domain,
            color = Color(0xFF00695C),
            description = "Company announcements and corporate news"
        ),
        "Earnings" to CategoryInfo(
            icon = Icons.Default.Assessment,
            color = Color(0xFF1565C0),
            description = "Quarterly reports and earnings updates"
        )
    )

    data class CategoryInfo(val icon: ImageVector, val color: Color, val description: String)

    fun getIcon(category: String): ImageVector {
        return categoryMap[category]?.icon ?: Icons.Default.Article
    }

    fun getColor(category: String): Color {
        return categoryMap[category]?.color ?: Color(0xFF616161)
    }

    fun getDescription(category: String): String {
        return categoryMap[category]?.description ?: "Stay updated with the latest news"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSubscriptionScreen(
    mode: SubscriptionScreenMode,
    subscriptionManager: SubscriptionManager? = null, // Only needed for MANAGEMENT mode
    onComplete: () -> Unit, // Called when setup/changes are complete
    onNavigateBack: (() -> Unit)? = null, // Back navigation for MANAGEMENT mode
    onNavigateToAllNotifications: (() -> Unit)? = null, // Navigation options for MANAGEMENT mode
    onNavigateToReadingList: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load market feeds from JSON
    val marketList: List<MarketFeed> = remember {
        val jsonString = context.assets.open("Finance_Market_RSS_Feeds.json").bufferedReader().use { it.readText() }
        val marketListType = object : TypeToken<List<MarketFeed>>() {}.type
        Gson().fromJson(jsonString, marketListType)
    }

    // Get all available categories
    val categories = remember { marketList.map { it.category }.distinct().sorted() }

    // Get current subscriptions - handle both modes properly
    val allSubscriptions = if (subscriptionManager != null) {
        val subscriptionsState by subscriptionManager.subscriptionsFlow.collectAsState()
        subscriptionsState
    } else {
        emptyList()
    }

    // Calculate currently subscribed categories based on mode
    val currentSubscribedCategories = remember(allSubscriptions, mode) {
        if (mode == SubscriptionScreenMode.MANAGEMENT) {
            allSubscriptions.map { it.category }.distinct()
        } else {
            emptyList()
        }
    }

    // State for selected categories
    val selectedCategories = remember(currentSubscribedCategories) {
        mutableStateListOf<String>().apply {
            addAll(currentSubscribedCategories)
        }
    }

    // Progress calculation for onboarding
    val progress by animateFloatAsState(
        targetValue = if (selectedCategories.isNotEmpty()) 1f else 0.5f,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    // Back handler
    BackHandler {
        when (mode) {
            SubscriptionScreenMode.ONBOARDING -> { /* Do nothing - prevent back navigation */ }
            SubscriptionScreenMode.MANAGEMENT -> onNavigateBack?.invoke()
        }
    }

    StaticGradientBackground(
        colors = listOf(BackgroundLight, Color.White),
        direction = GradientDirection.TopToBottom
    ) {
        Scaffold(
            snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
            containerColor = Color.Transparent,
            topBar = {
                when (mode) {
                    SubscriptionScreenMode.ONBOARDING -> {
                        // Simple top bar for onboarding
                        TopAppBar(
                            title = { Text("Setup Your Interests") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                    SubscriptionScreenMode.MANAGEMENT -> {
                        // Full top bar for management
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ManageAccounts,
                                        contentDescription = null,
                                        tint = Primary
                                    )
                                    Text(
                                        "Manage Subscriptions",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { onNavigateBack?.invoke() }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                onNavigateToReadingList?.let { navigate ->
                                    IconButton(onClick = navigate) {
                                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Reading List")
                                    }
                                }
                                onNavigateToAllNotifications?.let { navigate ->
                                    IconButton(onClick = navigate) {
                                        Icon(Icons.Default.Notifications, contentDescription = "All Notifications")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                }
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
                HeaderSection(
                    mode = mode,
                    progress = progress,
                    selectedCount = selectedCategories.size,
                    totalCount = categories.size
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Categories list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(categories) { category ->
                        ModernCategoryCard(
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

                // Action button
                ActionButton(
                    mode = mode,
                    selectedCategories = selectedCategories,
                    onComplete = {
                        // Create subscriptions based on selected categories
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

                        // Update subscriptions based on mode
                        when (mode) {
                            SubscriptionScreenMode.ONBOARDING -> {
                                subscriptionManager?.overwriteSubscriptions(newSubscriptions)
                            }
                            SubscriptionScreenMode.MANAGEMENT -> {
                                subscriptionManager?.overwriteSubscriptions(newSubscriptions)
                                scope.launch {
                                    snackbarHostState?.showSnackbar(
                                        "✨ Subscriptions updated! You'll receive notifications from ${selectedCategories.size} categories."
                                    )
                                }
                            }
                        }

                        onComplete()
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    mode: SubscriptionScreenMode,
    progress: Float,
    selectedCount: Int,
    totalCount: Int
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (mode) {
                        SubscriptionScreenMode.ONBOARDING -> Icons.Default.Tune
                        SubscriptionScreenMode.MANAGEMENT -> Icons.Default.ManageAccounts
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Primary
                )
            }

            // Title and description based on mode
            when (mode) {
                SubscriptionScreenMode.ONBOARDING -> {
                    Text(
                        text = "Choose Your Interests",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Select the news categories you'd like to follow. You can always change these later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Progress indicator for onboarding
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Primary,
                            trackColor = Primary.copy(alpha = 0.1f)
                        )
                        Text(
                            text = "$selectedCount of $totalCount categories selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedCount > 0) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                SubscriptionScreenMode.MANAGEMENT -> {
                    Text(
                        text = "Manage Your Subscriptions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Select the categories you want to receive notifications from",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Selection counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = selectedCount.toFloat() / totalCount.toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Primary,
                            trackColor = Primary.copy(alpha = 0.1f)
                        )
                        Text(
                            text = "${selectedCount}/${totalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernCategoryCard(
    category: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val categoryColor = UnifiedCategoryVisuals.getColor(category)
    val categoryIcon = UnifiedCategoryVisuals.getIcon(category)
    val categoryDescription = UnifiedCategoryVisuals.getDescription(category)

    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val cardColor by animateColorAsState(
        targetValue = if (isSelected) {
            categoryColor.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300)
    )

    val interactionSource = remember { MutableInteractionSource() }

    ElevatedModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onToggle() },
        containerColor = cardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected)
                            categoryColor.copy(alpha = 0.15f)
                        else
                            categoryColor.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = category,
                    tint = categoryColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Content section
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = categoryDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        categoryColor.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                // Selection status
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = categoryColor
                        )
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Selection indicator
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(categoryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    mode: SubscriptionScreenMode,
    selectedCategories: List<String>,
    onComplete: () -> Unit
) {
    AnimatedVisibility(
        visible = selectedCategories.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when (mode) {
                    SubscriptionScreenMode.ONBOARDING ->
                        "You'll receive notifications from ${selectedCategories.size} categor${if (selectedCategories.size == 1) "y" else "ies"}"
                    SubscriptionScreenMode.MANAGEMENT ->
                        "You'll receive notifications from ${selectedCategories.size} categor${if (selectedCategories.size == 1) "y" else "ies"}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            AnimatedGradientButton(
                text = when (mode) {
                    SubscriptionScreenMode.ONBOARDING ->
                        if (selectedCategories.isEmpty()) "Select at least one category"
                        else "Complete Setup (${selectedCategories.size} selected)"
                    SubscriptionScreenMode.MANAGEMENT -> "Save Changes"
                },
                onClick = onComplete,
                enabled = selectedCategories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Check
            )
        }
    }
}