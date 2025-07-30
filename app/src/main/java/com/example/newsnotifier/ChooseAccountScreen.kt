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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
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
fun ChooseAccountScreen(
    authManager: AuthManager,
    onNavigateToSelection: () -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val loggedInUser by authManager.loggedInUserFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

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
                            "Choose Account",
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Header Section
                HeaderCard()

                // Current User Section
                loggedInUser?.let { user ->
                    CurrentUserCard(
                        user = user,
                        onContinue = onNavigateToSelection
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Account Options
                AccountOptionsCard(
                    onUseAnotherAccount = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Feature: Use another account (Not implemented)")
                        }
                    }
                )

                // Sign Out Section
                if (loggedInUser != null) {
                    SignOutCard(
                        onSignOut = {
                            authManager.logoutUser()
                            scope.launch {
                                snackbarHostState.showSnackbar("Logged out successfully.")
                            }
                            onNavigateBack()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
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
                    contentDescription = "Account",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }

            Text(
                text = "Select Your Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Choose an account to continue with personalized news",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CurrentUserCard(
    user: com.google.firebase.auth.FirebaseUser,
    onContinue: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Current Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            ModernAccountCard(
                icon = Icons.Filled.Person,
                title = user.displayName ?: "User",
                description = user.email ?: "No email",
                isSelected = true,
                onClick = onContinue
            )
        }
    }
}

@Composable
private fun AccountOptionsCard(
    onUseAnotherAccount: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Other Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ModernAccountCard(
                icon = Icons.Filled.Email,
                title = "Use Another Account",
                description = "Sign in with a different account or create new one",
                isSelected = false,
                onClick = onUseAnotherAccount
            )
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
                text = "Account Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            AnimatedGradientButton(
                text = "Sign Out",
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(Error, Error.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
private fun ModernAccountCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val borderColor = if (isSelected) {
        Primary
    } else {
        Color.Transparent
    }

    ModernCard(
        onClick = onClick,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}