package com.example.newsnotifier

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsnotifier.ui.components.AnimatedGradientButton
import com.example.newsnotifier.ui.components.ElevatedModernCard
import com.example.newsnotifier.ui.components.StaticGradientBackground
import com.example.newsnotifier.ui.components.GradientDirection
import com.example.newsnotifier.utils.BackupRestoreManager
import com.example.newsnotifier.utils.SubscriptionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    subscriptionManager: SubscriptionManager,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val backupManager = remember { BackupRestoreManager(context) }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val success = backupManager.exportSubscriptions(subscriptionManager.getSubscriptions(), it)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (success) "Subscriptions exported successfully!" else "Failed to export subscriptions."
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            backupManager.importSubscriptions(it)?.let { importedSubscriptions ->
                // Clear existing subscriptions before importing
                subscriptionManager.getSubscriptions().forEach { sub ->
                    subscriptionManager.removeSubscription(sub.id)
                }
                importedSubscriptions.forEach { subscription ->
                    subscriptionManager.addSubscription(subscription)
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Subscriptions imported successfully!")
                }
            } ?: scope.launch {
                snackbarHostState.showSnackbar("Failed to import subscriptions.")
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    StaticGradientBackground(
        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background),
        direction = GradientDirection.TopToBottom
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Backup & Restore",
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
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ElevatedModernCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Export Subscriptions",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Save your current list of subscriptions to a file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedGradientButton(
                            text = "Export",
                            onClick = { exportLauncher.launch("subscriptions.json") },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Upload
                        )
                    }
                }

                ElevatedModernCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Import Subscriptions",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Restore your subscriptions from a previously exported file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedGradientButton(
                            text = "Import",
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Download
                        )
                    }
                }
            }
        }
    }
}