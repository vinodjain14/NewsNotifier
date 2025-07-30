package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import com.example.newsnotifier.data.NotificationItem
import com.example.newsnotifier.ui.components.*
import com.example.newsnotifier.ui.theme.*
import com.example.newsnotifier.utils.NotificationHelper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotificationsScreen(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    notificationIdToFocus: String? = null
) {
    val allNotifications by NotificationHelper.notificationsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    var selectedFilter by remember { mutableStateOf("All") }
    var showActionButtonsForNotificationId by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val filteredNotifications = remember(allNotifications, selectedFilter) {
        when (selectedFilter) {
            "All" -> allNotifications
            "Unread" -> allNotifications.filter { !it.isRead }
            "Breaking" -> allNotifications.filter { it.isBreaking }
            "Saved" -> allNotifications.filter { it.isSaved }
            else -> allNotifications
        }
    }

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
            .toSortedMap(compareByDescending { it })
    }

    BackHandler {
        onNavigateBack()
    }

    // FIXED: Simplified LaunchedEffect for focusing on notification
    LaunchedEffect(notificationIdToFocus) {
        if (notificationIdToFocus != null) {
            showActionButtonsForNotificationId = notificationIdToFocus
            scope.launch {
                val notification = allNotifications.find { it.id == notificationIdToFocus }
                if (notification != null) {
                    snackbarHostState.showSnackbar("Focusing on notification: ${notification.title}")
                } else {
                    snackbarHostState.showSnackbar("Notification not found")
                }
            }
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
                            "All Notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                        }
                        IconButton(onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Refreshing notifications...") }
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
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    // Notifications Summary
                    NotificationsSummaryCard(
                        totalCount = allNotifications.size,
                        unreadCount = allNotifications.count { !it.isRead },
                        breakingCount = allNotifications.count { it.isBreaking },
                        currentFilter = selectedFilter
                    )
                }

                item {
                    // Filter Chips
                    FilterChipsRow(
                        selectedFilter = selectedFilter,
                        onFilterChange = { selectedFilter = it },
                        notificationCounts = mapOf(
                            "All" to allNotifications.size,
                            "Unread" to allNotifications.count { !it.isRead },
                            "Breaking" to allNotifications.count { it.isBreaking },
                            "Saved" to allNotifications.count { it.isSaved }
                        )
                    )
                }

                if (filteredNotifications.isEmpty()) {
                    item {
                        EmptyNotificationsCard(filter = selectedFilter)
                    }
                } else {
                    groupedNotifications.forEach { (dateGroup, notificationsInGroup) ->
                        item {
                            DateGroupHeader(dateGroup = dateGroup)
                        }

                        items(notificationsInGroup, key = { it.id }) { notification ->
                            ModernNotificationCard(
                                notification = notification,
                                showActionButtons = showActionButtonsForNotificationId == notification.id,
                                onClick = {
                                    showActionButtonsForNotificationId =
                                        if (showActionButtonsForNotificationId == notification.id) null else notification.id
                                },
                                onReadClick = {
                                    scope.launch { snackbarHostState.showSnackbar("Marked as Read: ${it.title}") }
                                },
                                onSaveClick = {
                                    scope.launch { snackbarHostState.showSnackbar("Saved: ${it.title}") }
                                },
                                onShareClick = {
                                    scope.launch { snackbarHostState.showSnackbar("Share: ${it.title}") }
                                }
                            )
                        }
                    }
                }

                item {
                    // Clear All Button
                    if (allNotifications.isNotEmpty()) {
                        ElevatedModernCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedGradientButton(
                                    text = "Clear All Notifications",
                                    onClick = {
                                        NotificationHelper.clearAllNotifications()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("All notifications cleared!")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.Delete,
                                    gradientColors = listOf(Error, Error.copy(alpha = 0.8f))
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        if (showFilterDialog) {
            FilterDialog(
                currentFilter = selectedFilter,
                onFilterSelected = {
                    selectedFilter = it
                    showFilterDialog = false
                },
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@Composable
private fun NotificationsSummaryCard(
    totalCount: Int,
    unreadCount: Int,
    breakingCount: Int,
    currentFilter: String
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Notifications Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryItem(
                    title = "Total",
                    value = totalCount.toString(),
                    color = Primary,
                    isSelected = currentFilter == "All"
                )

                SummaryItem(
                    title = "Unread",
                    value = unreadCount.toString(),
                    color = Warning,
                    isSelected = currentFilter == "Unread"
                )

                SummaryItem(
                    title = "Breaking",
                    value = breakingCount.toString(),
                    color = Error,
                    isSelected = currentFilter == "Breaking"
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    value: String,
    color: Color,
    isSelected: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(300)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
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
private fun FilterChipsRow(
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    notificationCounts: Map<String, Int>
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filter by",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(notificationCounts.toList()) { (filter, count) ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text("$filter ($count)")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DateGroupHeader(dateGroup: String) {
    Text(
        text = dateGroup,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun EmptyNotificationsCard(filter: String) {
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
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "No ${filter.lowercase()} notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = when (filter) {
                    "Unread" -> "All caught up! You've read all your notifications."
                    "Breaking" -> "No breaking news at the moment."
                    "Saved" -> "No saved notifications yet."
                    else -> "No notifications to display."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernNotificationCard(
    notification: NotificationItem,
    showActionButtons: Boolean,
    onClick: (NotificationItem) -> Unit,
    onReadClick: (NotificationItem) -> Unit,
    onSaveClick: (NotificationItem) -> Unit,
    onShareClick: (NotificationItem) -> Unit
) {
    ElevatedModernCard(
        onClick = { onClick(notification) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Source Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppDefaults.getInitials(notification.sourceName),
                        color = Primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        notification.tag?.let { tag ->
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        color = when (tag) {
                                            "BREAKING" -> TagBreaking
                                            "NEW" -> TagNew
                                            "TRENDING" -> TagTrending
                                            else -> Primary
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
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

                    if (notification.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (showActionButtons) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedOutlinedButton(
                        text = "Read",
                        onClick = { onReadClick(notification) },
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    AnimatedOutlinedButton(
                        text = "Save",
                        onClick = { onSaveClick(notification) },
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    AnimatedOutlinedButton(
                        text = "Share",
                        onClick = { onShareClick(notification) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Share,
                        height = 40.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    currentFilter: String,
    onFilterSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Filter Notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Unread", "Breaking", "Saved").forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (currentFilter == filter) Primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFilter == filter,
                            onClick = { onFilterSelected(filter) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
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