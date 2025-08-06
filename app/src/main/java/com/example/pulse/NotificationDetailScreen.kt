package com.example.pulse

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pulse.data.NotificationItem
import com.example.pulse.ui.components.ElevatedModernCard
import com.example.pulse.ui.components.GradientDirection
import com.example.pulse.ui.components.StaticGradientBackground
import com.example.pulse.ui.theme.*
import com.example.pulse.utils.NotificationHelper
import com.example.pulse.utils.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notification: NotificationItem,
    readingListManager: ReadingListManager,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    sourceUrl: String? = null // Add sourceUrl parameter for RSS feeds
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val readingList by readingListManager.readingListFlow.collectAsState()
    val isBookmarked = readingList.any { it.id == notification.id }

    var isLoading by remember { mutableStateOf(true) }
    var showWebView by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }

    // Mark as read when screen opens
    LaunchedEffect(notification.id) {
        if (!notification.isRead) {
            NotificationHelper.markNotificationAsRead(notification.id)
        }
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
                        Text(
                            text = notification.sourceName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Bookmark button
                        IconButton(
                            onClick = {
                                if (isBookmarked) {
                                    readingListManager.removeFromReadingList(notification.id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Removed from reading list")
                                    }
                                } else {
                                    readingListManager.addToReadingList(notification)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Added to reading list")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove from reading list" else "Add to reading list",
                                tint = if (isBookmarked) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Share button
                        IconButton(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, notification.title)
                                    putExtra(Intent.EXTRA_TEXT, "${notification.title}\n\n${notification.message}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Article header card
                item {
                    ArticleHeaderCard(notification = notification)
                }

                // Web view toggle card
                if (sourceUrl != null && sourceUrl.isNotBlank()) {
                    item {
                        WebViewToggleCard(
                            showWebView = showWebView,
                            onToggleWebView = { showWebView = !showWebView },
                            sourceUrl = sourceUrl
                        )
                    }

                    // Web view
                    if (showWebView) {
                        item {
                            ElevatedModernCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(600.dp)
                            ) {
                                if (webViewError != null) {
                                    WebViewErrorContent(
                                        error = webViewError!!,
                                        onRetry = {
                                            webViewError = null
                                            isLoading = true
                                        }
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AndroidView(
                                            factory = { context ->
                                                WebView(context).apply {
                                                    webViewClient = object : WebViewClient() {
                                                        override fun onPageFinished(view: WebView?, url: String?) {
                                                            super.onPageFinished(view, url)
                                                            isLoading = false
                                                        }

                                                        override fun onReceivedError(
                                                            view: WebView?,
                                                            errorCode: Int,
                                                            description: String?,
                                                            failingUrl: String?
                                                        ) {
                                                            super.onReceivedError(view, errorCode, description, failingUrl)
                                                            isLoading = false
                                                            webViewError = "Failed to load page: $description"
                                                        }
                                                    }

                                                    webChromeClient = WebChromeClient()

                                                    settings.apply {
                                                        javaScriptEnabled = true
                                                        domStorageEnabled = true
                                                        loadWithOverviewMode = true
                                                        useWideViewPort = true
                                                        setSupportZoom(true)
                                                        builtInZoomControls = true
                                                        displayZoomControls = false
                                                    }

                                                    loadUrl(sourceUrl)
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (isLoading) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    CircularProgressIndicator()
                                                    Text(
                                                        text = "Loading article...",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // No URL available message
                    item {
                        ElevatedModernCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Source URL Not Available",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "This notification doesn't have a source URL to display in the browser.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
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
private fun ArticleHeaderCard(notification: NotificationItem) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notification.sourceName.take(2).uppercase(),
                            color = Primary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = notification.sourceName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${notification.market} • ${notification.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notification.isBreaking) {
                        AssistChip(
                            onClick = { },
                            label = { Text("BREAKING", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Error.copy(alpha = 0.1f),
                                labelColor = Error
                            )
                        )
                    }
                    if (notification.isNew) {
                        AssistChip(
                            onClick = { },
                            label = { Text("NEW", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Primary.copy(alpha = 0.1f),
                                labelColor = Primary
                            )
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Title
            Text(
                text = notification.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Message/Description
            if (notification.message.isNotBlank()) {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                )
            }

            // Timestamp
            Text(
                text = "Published ${TimeUtils.formatRelativeTime(notification.timestamp)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WebViewToggleCard(
    showWebView: Boolean,
    onToggleWebView: () -> Unit,
    sourceUrl: String
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showWebView) "Hide Original Article" else "View Original Article",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Load the full article from the source website",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = showWebView,
                    onCheckedChange = { onToggleWebView() }
                )
            }

            if (!showWebView) {
                Text(
                    text = "Source: ${sourceUrl.take(50)}${if (sourceUrl.length > 50) "..." else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun WebViewErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Failed to Load",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}