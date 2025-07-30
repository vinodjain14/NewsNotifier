package com.example.newsnotifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsnotifier.ui.components.*
import com.example.newsnotifier.ui.theme.*
import com.example.newsnotifier.utils.AuthManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    authManager: AuthManager,
    onNavigateToSelection: () -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit, // ADD THIS PARAMETER
    snackbarHostState: SnackbarHostState
) {
    val loggedInUser by authManager.loggedInUserFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var readingTime by remember { mutableStateOf("Daily: 9:00 AM - 6:00 PM") }

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
                            "My Profile",
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

                // Profile Header
                ProfileHeaderCard(
                    user = loggedInUser,
                    onNavigateToSelection = onNavigateToSelection
                )

                // Stats Section
                StatsCard()

                // Preferences Section
                PreferencesCard(
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsToggle = { notificationsEnabled = it },
                    darkModeEnabled = darkModeEnabled,
                    onDarkModeToggle = { darkModeEnabled = it },
                    readingTime = readingTime,
                    onReadingTimeClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Reading Time settings (Not implemented)")
                        }
                    }
                )

                // Account Section
                AccountCard(
                    onAnalyticsClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Reading Analytics (Not implemented)")
                        }
                    },
                    onExportClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Export Data (Not implemented)")
                        }
                    },
                    onFeedbackClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Send Feedback (Not implemented)")
                        }
                    }
                )

                // Sign Out Section - FIXED: Use onLogout instead of onNavigateBack
                SignOutCard(
                    onSignOut = {
                        authManager.logoutUser()
                        scope.launch {
                            snackbarHostState.showSnackbar("Logged out successfully.")
                        }
                        onLogout() // FIXED: Use proper logout navigation
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    user: com.google.firebase.auth.FirebaseUser?,
    onNavigateToSelection: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Primary, PrimaryDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }

            // User Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = user?.displayName ?: "Test Device",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user?.email ?: "tdevice321@gmail.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Action Button
            AnimatedGradientButton(
                text = "Manage Feeds",
                onClick = onNavigateToSelection,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatsCard() {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("127", "Articles Read", Success)
                StatItem("5", "Active Sources", Primary)
                StatItem("23", "Days Active", Info)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PreferencesCard(
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    darkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    readingTime: String,
    onReadingTimeClick: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Filled.Notifications,
                    title = "Push Notifications",
                    description = "Get notified about breaking news",
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onNotificationsToggle
                        )
                    }
                )

                Divider(modifier = Modifier.padding(horizontal = 8.dp))

                ProfileMenuItem(
                    icon = Icons.Filled.DarkMode,
                    title = "Dark Mode",
                    description = "Easier on your eyes",
                    trailingContent = {
                        Switch(
                            checked = darkModeEnabled,
                            onCheckedChange = onDarkModeToggle
                        )
                    }
                )

                Divider(modifier = Modifier.padding(horizontal = 8.dp))

                ProfileMenuItem(
                    icon = Icons.Filled.Schedule,
                    title = "Reading Schedule",
                    description = readingTime,
                    onClick = onReadingTimeClick
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    onAnalyticsClick: () -> Unit,
    onExportClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Filled.QueryStats,
                    title = "Reading Analytics",
                    description = "View your reading habits and insights",
                    onClick = onAnalyticsClick
                )

                Divider(modifier = Modifier.padding(horizontal = 8.dp))

                ProfileMenuItem(
                    icon = Icons.Filled.Share,
                    title = "Export Data",
                    description = "Download your reading history",
                    onClick = onExportClick
                )

                Divider(modifier = Modifier.padding(horizontal = 8.dp))

                ProfileMenuItem(
                    icon = Icons.Filled.MailOutline,
                    title = "Send Feedback",
                    description = "Help us improve the app",
                    onClick = onFeedbackClick
                )
            }
        }
    }
}

@Composable
private fun SignOutCard(
    onSignOut: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account Management",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            ProfileMenuItem(
                icon = Icons.Filled.ExitToApp,
                title = "Sign Out",
                description = "Sign out of your account",
                tint = Error,
                onClick = onSignOut
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color = Primary,
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
                        .background(tint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
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