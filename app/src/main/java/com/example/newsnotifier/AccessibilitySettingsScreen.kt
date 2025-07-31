package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsnotifier.ui.components.ElevatedModernCard
import com.example.newsnotifier.ui.components.ModernCard
import com.example.newsnotifier.ui.components.StaticGradientBackground
import com.example.newsnotifier.ui.components.GradientDirection
import com.example.newsnotifier.ui.theme.Primary
import com.example.newsnotifier.utils.FontSize
import com.example.newsnotifier.utils.ThemeManager
import com.example.newsnotifier.utils.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    themeManager: ThemeManager,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val themeMode by themeManager.themeModeFlow.collectAsState()
    val fontSize by themeManager.fontSizeFlow.collectAsState()
    var showFontSizeDialog by remember { mutableStateOf(false) }

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
                            "Accessibility",
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SettingsSection(title = "Display") {
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Theme",
                        description = "Current: ${themeMode.displayName}",
                        onClick = { /* Implement theme selection dialog if more than two options */ }
                    ) {
                        Switch(
                            checked = themeMode == ThemeMode.DARK,
                            onCheckedChange = { isChecked ->
                                themeManager.setThemeMode(if (isChecked) ThemeMode.DARK else ThemeMode.LIGHT)
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(horizontal = 8.dp))

                    SettingsItem(
                        icon = Icons.Default.TextFields,
                        title = "Font Size",
                        description = "Current: ${fontSize.displayName}",
                        onClick = { showFontSizeDialog = true }
                    )
                }
            }
        }
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = fontSize,
            onSizeSelected = { newSize ->
                themeManager.setFontSize(newSize)
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedModernCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
    trailingContent: (@Composable () -> Unit)? = null
) {
    ModernCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary
                )
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
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun FontSizeDialog(
    currentSize: FontSize,
    onSizeSelected: (FontSize) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Font Size") },
        text = {
            Column {
                FontSize.values().forEach { size ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSizeSelected(size) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSize == size,
                            onClick = { onSizeSelected(size) }
                        )
                        Text(
                            text = size.displayName,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}