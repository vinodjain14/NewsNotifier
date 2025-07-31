package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsnotifier.data.NotificationItem
import com.example.newsnotifier.ui.components.*
import com.example.newsnotifier.ui.theme.*
import com.example.newsnotifier.utils.ReadingListManager
import com.example.newsnotifier.utils.SharingUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    readingListManager: ReadingListManager,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val savedArticles by readingListManager.readingListFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    BackHandler {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedItems = emptySet()
        } else {
            onNavigateBack()
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
                            if (isSelectionMode) "${selectedItems.size} Selected" else "Reading List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedItems = emptySet()
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (isSelectionMode && selectedItems.isNotEmpty()) {
                            IconButton(onClick = {
                                val itemsToShare = savedArticles.filter { it.id in selectedItems }
                                SharingUtil.shareMultipleArticles(context, itemsToShare)
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share Selected")
                            }

                            IconButton(onClick = {
                                selectedItems.forEach { id ->
                                    readingListManager.removeFromReadingList(id)
                                }
                                isSelectionMode = false
                                selectedItems = emptySet()
                                scope.launch {
                                    snackbarHostState.showSnackbar("${selectedItems.size} items removed")
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Selected")
                            }
                        } else if (!isSelectionMode && savedArticles.isNotEmpty()) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Select Items")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->
            if (savedArticles.isEmpty()) {
                EmptyReadingListCard()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(savedArticles, key = { it.id }) { article ->
                        SavedArticleCard(
                            article = article,
                            isSelected = article.id in selectedItems,
                            isSelectionMode = isSelectionMode,
                            onToggleSelection = {
                                selectedItems = if (article.id in selectedItems) {
                                    selectedItems - article.id
                                } else {
                                    selectedItems + article.id
                                }
                            },
                            onRemove = {
                                readingListManager.removeFromReadingList(article.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Removed from reading list")
                                }
                            },
                            onShare = {
                                SharingUtil.shareArticle(context, article)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReadingListCard() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ElevatedModernCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "No Saved Articles",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Save articles to read them later, even offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedArticleCard(
    article: NotificationItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit
) {
    ElevatedModernCard(
        onClick = if (isSelectionMode) onToggleSelection else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = article.sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (article.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = article.message,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                }

                if (!isSelectionMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = onShare,
                            label = { Text("Share") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        AssistChip(
                            onClick = onRemove,
                            label = { Text("Remove") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
