package com.example.pulse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                        Text(
                            "Reading List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
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
                                Text(
                                    text = "Your Reading List is Empty",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Save notifications to read them later",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    item {
                        ElevatedModernCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Reading List",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${readingList.size} saved item${if (readingList.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(readingList) { item ->
                        ReadingListItemCard(
                            item = item,
                            onClick = {
                                // Navigate to notification detail screen
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

                    item {
                        ElevatedModernCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedGradientButton(
                                    text = "Clear Reading List",
                                    onClick = {
                                        readingListManager.clearReadingList()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Reading list cleared!")
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
            }
        }
    }
}

@Composable
private fun ReadingListItemCard(
    item: com.example.pulse.data.NotificationItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClick() }
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = item.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (item.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from reading list",
                        tint = Error
                    )
                }
            }
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