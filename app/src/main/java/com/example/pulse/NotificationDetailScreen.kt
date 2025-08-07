package com.example.pulse

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

// Category visual mapping - same as AllNotificationsScreen
private object DetailCategoryVisuals {
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

    // Enhanced WebView state management
    var isLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var showWebView by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var currentUrl by remember { mutableStateOf(sourceUrl ?: "") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Scroll state for hiding/showing top bar
    val listState = rememberLazyListState()
    val isScrollingUp = remember { mutableStateOf(true) }
    val previousScrollOffset = remember { mutableStateOf(0) }

    // Track scroll direction for top bar visibility
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { currentScrollOffset ->
                val scrollDelta = currentScrollOffset - previousScrollOffset.value
                isScrollingUp.value = scrollDelta <= 0 || currentScrollOffset == 0
                previousScrollOffset.value = currentScrollOffset
            }
    }

    // Mark as read when screen opens
    LaunchedEffect(notification.id) {
        if (!notification.isRead) {
            NotificationHelper.markNotificationAsRead(notification.id)
        }
    }

    BackHandler {
        if (showWebView && canGoBack && webView != null) {
            webView?.goBack()
        } else {
            onNavigateBack()
        }
    }

    StaticGradientBackground(
        colors = listOf(BackgroundLight, Color.White),
        direction = GradientDirection.TopToBottom
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isScrollingUp.value) 64.dp else 0.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Article header card
                item {
                    EnhancedArticleHeaderCard(notification = notification)
                }

                // Enhanced web view toggle card
                if (sourceUrl != null && sourceUrl.isNotBlank()) {
                    item {
                        EnhancedWebViewToggleCard(
                            showWebView = showWebView,
                            onToggleWebView = {
                                showWebView = !showWebView
                                if (showWebView) {
                                    isLoading = true
                                    webViewError = null
                                }
                            },
                            sourceUrl = sourceUrl
                        )
                    }

                    // Enhanced Web view with controls
                    if (showWebView) {
                        // Navigation controls
                        item {
                            WebViewNavigationControls(
                                canGoBack = canGoBack,
                                canGoForward = canGoForward,
                                isLoading = isLoading,
                                currentUrl = currentUrl,
                                onBack = { webView?.goBack() },
                                onForward = { webView?.goForward() },
                                onRefresh = { webView?.reload() },
                                onOpenInBrowser = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                    context.startActivity(intent)
                                },
                                onShare = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, notification.title)
                                        putExtra(Intent.EXTRA_TEXT, "$currentUrl\n\n${notification.title}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                                }
                            )
                        }

                        // Enhanced WebView
                        item {
                            ElevatedModernCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(700.dp) // Increased height for better experience
                            ) {
                                if (webViewError != null) {
                                    WebViewErrorContent(
                                        error = webViewError!!,
                                        onRetry = {
                                            webViewError = null
                                            isLoading = true
                                            webView?.reload()
                                        }
                                    )
                                } else {
                                    Column {
                                        // Progress indicator
                                        if (isLoading && loadingProgress < 100) {
                                            LinearProgressIndicator(
                                                progress = loadingProgress / 100f,
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Primary
                                            )
                                        }

                                        Box(modifier = Modifier.weight(1f)) {
                                            AndroidView(
                                                factory = { context ->
                                                    WebView(context).apply {
                                                        webView = this

                                                        webViewClient = object : WebViewClient() {
                                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                                super.onPageStarted(view, url, favicon)
                                                                isLoading = true
                                                                url?.let { currentUrl = it }
                                                                webViewError = null
                                                            }

                                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                                super.onPageFinished(view, url)
                                                                isLoading = false
                                                                loadingProgress = 100
                                                                canGoBack = view?.canGoBack() ?: false
                                                                canGoForward = view?.canGoForward() ?: false
                                                                url?.let { currentUrl = it }
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

                                                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                                // Allow navigation within the WebView
                                                                return false
                                                            }
                                                        }

                                                        webChromeClient = object : WebChromeClient() {
                                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                                super.onProgressChanged(view, newProgress)
                                                                loadingProgress = newProgress
                                                                isLoading = newProgress < 100
                                                            }
                                                        }

                                                        settings.apply {
                                                            javaScriptEnabled = true
                                                            domStorageEnabled = true
                                                            databaseEnabled = true

                                                            // Better layout and viewport
                                                            loadWithOverviewMode = true
                                                            useWideViewPort = true

                                                            // Zoom controls
                                                            setSupportZoom(true)
                                                            builtInZoomControls = true
                                                            displayZoomControls = false

                                                            // Text and content
                                                            textZoom = 100
                                                            minimumFontSize = 8

                                                            // Performance and caching
                                                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                                                            // Security (allow mixed content for compatibility)
                                                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                                                            // User agent (use default mobile user agent)
                                                            userAgentString = userAgentString?.replace("; wv", "")
                                                        }

                                                        loadUrl(sourceUrl)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Loading overlay for initial load
                                            if (isLoading && loadingProgress == 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        CircularProgressIndicator(color = Primary)
                                                        Text(
                                                            text = "Loading article...",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "This may take a few moments",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                    }
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

            // Animated Top App Bar
            androidx.compose.animation.AnimatedVisibility(
                visible = isScrollingUp.value,
                enter = androidx.compose.animation.slideInVertically(),
                exit = androidx.compose.animation.slideOutVertically(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
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
                            // Bookmark button with category color
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
                                    tint = if (isBookmarked) DetailCategoryVisuals.getColor(notification.category) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Share button
                            IconButton(
                                onClick = {
                                    val shareText = if (currentUrl.isNotEmpty() && showWebView) {
                                        "${notification.title}\n\n$currentUrl"
                                    } else {
                                        "${notification.title}\n\n${notification.message}"
                                    }

                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, notification.title)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
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
            }

            // Snackbar Host
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(snackbarHostState)
            }
        }
    }
}

@Composable
private fun EnhancedArticleHeaderCard(notification: NotificationItem) {
    val categoryColor = DetailCategoryVisuals.getColor(notification.category)
    val categoryIcon = DetailCategoryVisuals.getIcon(notification.category)

    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source info row with enhanced category visuals
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
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(categoryColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = notification.category,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = notification.sourceName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(categoryColor)
                            )
                            Text(
                                text = "${notification.market} • ${notification.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Enhanced tags with category colors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notification.isBreaking) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "BREAKING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Emergency,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Error.copy(alpha = 0.1f),
                                labelColor = Error,
                                leadingIconContentColor = Error
                            )
                        )
                    }
                    if (notification.isNew) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "NEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FiberNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
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

            Divider(color = categoryColor.copy(alpha = 0.2f), thickness = 2.dp)

            // Title with category accent
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(categoryColor)
                )
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // Message/Description
            if (notification.message.isNotBlank()) {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                )
            }

            // Enhanced timestamp with category color
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = categoryColor
                )
                Text(
                    text = "Published ${TimeUtils.formatRelativeTime(notification.timestamp)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EnhancedWebViewToggleCard(
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (showWebView) Icons.Default.VisibilityOff else Icons.Default.Language,
                        contentDescription = null,
                        tint = Primary
                    )
                    Column {
                        Text(
                            text = if (showWebView) "Hide Original Article" else "View Original Article",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (showWebView) "Collapse the web browser view" else "Load the full article from the source website",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = showWebView,
                    onCheckedChange = { onToggleWebView() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.3f)
                    )
                )
            }

            if (!showWebView) {
                Text(
                    text = "Source: ${sourceUrl.take(60)}${if (sourceUrl.length > 60) "..." else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun WebViewNavigationControls(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    currentUrl: String,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onShare: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // URL display
            if (currentUrl.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Primary
                    )
                    Text(
                        text = currentUrl.take(50) + if (currentUrl.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = if (canGoBack) Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go forward",
                        tint = if (canGoForward) Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(onClick = onRefresh) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Primary
                        )
                    }
                }

                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Primary
                    )
                }

                IconButton(onClick = onOpenInBrowser) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Open in browser",
                        tint = Primary
                    )
                }
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

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}