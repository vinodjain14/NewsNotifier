package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsnotifier.ui.components.*
import com.example.newsnotifier.ui.theme.*
import com.example.newsnotifier.utils.*
import kotlinx.coroutines.launch
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeManager: ThemeManager,
    subscriptionManager: SubscriptionManager,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by themeManager.themeModeFlow.collectAsState()
    val fontSize by themeManager.fontSizeFlow.collectAsState()
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showNotificationCategoryDialog by remember { mutableStateOf(false) }

    val backupRestoreManager = remember { BackupRestoreManager(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val subscriptions = subscriptionManager.getSubscriptions()
            val success = backupRestoreManager.exportSubscriptions(subscriptions, it)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (success) "Backup exported successfully" else "Export failed"
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val importedSubscriptions = backupRestoreManager.importSubscriptions(it)
            if (importedSubscriptions != null) {
                subscriptionManager.getSubscriptions().forEach { sub ->
                    subscriptionManager.removeSubscription(sub.id)
                }
                importedSubscriptions.forEach { sub ->
                    subscriptionManager.addSubscription(sub)
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Backup imported successfully")
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Import failed")
                }
            }
        }
    }

    BackHandler { onNavigateBack() }

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
                            "Settings",
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
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SettingsSection(title = "Appearance") {
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        description = "Use dark theme",
                        trailingContent = {
                            Switch(
                                checked = themeMode == ThemeMode.DARK,
                                onCheckedChange = { enabled ->
                                    themeManager.setThemeMode(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
                                }
                            )
                        }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 8.dp))

                    SettingsItem(
                        icon = Icons.Default.TextFields,
                        title = "Font Size",
                        description = fontSize.displayName,
                        onClick = { showFontSizeDialog = true }
                    )
                }

                SettingsSection(title = "Notifications") {
                    SettingsItem(
                        icon = Icons.Default.Category,
                        title = "Notification Categories",
                        description = "Configure notification grouping",
                        onClick = { showNotificationCategoryDialog = true }
                    )
                }

                SettingsSection(title = "Data Management") {
                    SettingsItem(
                        icon = Icons.Default.Upload,
                        title = "Export Subscriptions",
                        description = "Backup your subscriptions",
                        onClick = {
                            exportLauncher.launch(backupRestoreManager.generateBackupFileName())
                        }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 8.dp))

                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "Import Subscriptions",
                        description = "Restore from backup",
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentScale = fontSize,
            onScaleSelected = { scale ->
                themeManager.setFontSize(scale)
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false }
        )
    }

    if (showNotificationCategoryDialog) {
        NotificationCategoryDialog(
            onDismiss = { showNotificationCategoryDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ModernCard(
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        borderColor = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            trailingContent?.invoke()
        }
    }
}

@Composable
private fun FontSizeDialog(
    currentScale: FontSize,
    onScaleSelected: (FontSize) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Choose Font Size",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontSize.values().forEach { scale ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (currentScale == scale) Primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                            .padding(12.dp)
                            .clickable { onScaleSelected(scale) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentScale == scale,
                            onClick = { onScaleSelected(scale) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = scale.displayName,
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

@Composable
private fun NotificationCategoryDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Notification Categories",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Your notifications are automatically grouped by:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "News Sources" to "BBC, CNN, Reuters, etc.",
                    "Social Media" to "X (Twitter) personalities",
                    "Breaking News" to "Urgent updates",
                    "Saved Articles" to "Your reading list"
                ).forEach { (category, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}