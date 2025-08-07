package com.example.pulse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.ui.components.ElevatedModernCard
import com.example.pulse.ui.components.StaticGradientBackground
import com.example.pulse.ui.components.AnimatedGradientButton
import com.example.pulse.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.pulse.ui.components.GradientDirection

// Category visual mapping - same as other screens
private object ReadingListCategoryVisuals {
    private val categoryMap = mapOf(
        "Finance" to CategoryInfo(Icons.Default.TrendingUp, Color(0xFF2E7D32)), // Green
        "Technology" to CategoryInfo(Icons.Default.Computer, Color(0xFF1976D2)), // Blue
        "Sports" to CategoryInfo(Icons.Default.Sports, Color(0xFFE65100)), // Orange
        "Politics" to CategoryInfo(Icons.Default.Gavel, Color(0xFF7B1FA2)), // Purple
        "Health" to CategoryInfo(Icons.Default.LocalHospital, Color(0xFFD32F2F)), // Red
        "Entertainment" to CategoryInfo(Icons.Default.Movie, Color(0xFFF57C00)), // Amber
        "Business" to CategoryInfo(Icons.Default.Business, Color(0xFF388E3C)), // Dark Green
        "Science" to CategoryInfo(Icons.Default.Science, Color(0xFF0288D1)), // Light Blue
        "World" to CategoryInfo(Icons.Default.Public, Color(0xFF5D4037)), // Brown
        "Breaking" to CategoryInfo(Icons.Default.Emergency, Color(0xFFD32F2F)), // Red
        "General" to CategoryInfo(Icons.Default.Article, Color(0xFF616161)) // Grey
    )

    data class CategoryInfo(val icon: ImageVector, val color: Color)

    fun getIcon(category: String): ImageVector {
        return categoryMap[category]?.icon ?: Icons.Default.Article
    }

    fun getColor(category: String): Color {
        return categoryMap[category]?.color ?: Color(0xFF616161)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    readingListManager: ReadingListManager,
    onNavigateBack: () -> Unit,
    onNavigateToNotificationDetail: (com.example.pulse.data.NotificationItem) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val readingList by readingListManager.readingListFlow.collectAsState(initial = emptyList())

    // Group reading list by category for better organization
    val groupedReadingList = remember(readingList) {
        readingList.groupBy { it.category }
    }

    BackHandler {
        onNavigateBack()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdded,
                                contentDescription = null,
                                tint = Primary
                            )
                            Text(
                                "Reading List",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                if (readingList.isEmpty()) {
                    item {
                        EmptyReadingListCard()
                    }
                } else {
                    item {
                        ReadingListSummaryCard(readingList = readingList)
                    }

                    // Quick stats by category
                    if (groupedReadingList.size > 1) {
                        item {
                            CategoryStatsCard(groupedReadingList = groupedReadingList)
                        }
                    }

                    items(readingList) { item ->
                        EnhancedReadingListItemCard(
                            item = item,
                            onClick = {
                                onNavigateToNotificationDetail(item)
                            },
                            onRemove = {
                                readingListManager.removeFromReadingList(item.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Removed from reading list")
                                }
                            }
                        )
                    }

                    if (readingList.isNotEmpty()) {
                        item {
                            ClearReadingListCard(
                                onClearAll = {
                                    readingListManager.clearReadingList()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Reading list cleared!")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReadingListCard() {
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
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "Your Reading List is Empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Save notifications to read them later by tapping the bookmark icon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReadingListSummaryCard(readingList: List<com.example.pulse.data.NotificationItem>) {
    val unreadCount = readingList.count { !it.isRead }

    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Saved Articles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${readingList.size} total • ${unreadCount} unread",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = readingList.size.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun CategoryStatsCard(groupedReadingList: Map<String, List<com.example.pulse.data.NotificationItem>>) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "By Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            groupedReadingList.forEach { (category, items) ->
                val categoryColor = ReadingListCategoryVisuals.getColor(category)
                val categoryIcon = ReadingListCategoryVisuals.getIcon(category)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = category,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Badge(
                        containerColor = categoryColor.copy(alpha = 0.1f),
                        contentColor = categoryColor
                    ) {
                        Text(
                            text = items.size.toString(),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedReadingListItemCard(
    item: com.example.pulse.data.NotificationItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val categoryColor = ReadingListCategoryVisuals.getColor(item.category)
    val categoryIcon = ReadingListCategoryVisuals.getIcon(item.category)

    ElevatedModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Enhanced category thumbnail
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(categoryColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = item.category,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick() }
                    ) {
                        // Title with read status indicator
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!item.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(categoryColor)
                                        .padding(top = 6.dp)
                                )
                            }

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Source and category info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.sourceName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = categoryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Timestamp with enhanced formatting
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatTimestamp(item.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Message preview
                        if (item.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Enhanced remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = "Remove from reading list",
                        tint = Error.copy(alpha = 0.7f)
                    )
                }
            }

            // Article tags
            if (item.isBreaking || item.isNew) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.isBreaking) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "BREAKING",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Emergency,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Error.copy(alpha = 0.1f),
                                labelColor = Error,
                                leadingIconContentColor = Error
                            )
                        )
                    }
                    if (item.isNew) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "NEW",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FiberNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = categoryColor.copy(alpha = 0.1f),
                                labelColor = categoryColor,
                                leadingIconContentColor = categoryColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClearReadingListCard(onClearAll: () -> Unit) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Error.copy(alpha = 0.7f)
                )
                Text(
                    text = "This will permanently remove all saved articles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedGradientButton(
                text = "Clear Reading List",
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Delete,
                gradientColors = listOf(Error, Error.copy(alpha = 0.8f))
            )
        }
    }
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