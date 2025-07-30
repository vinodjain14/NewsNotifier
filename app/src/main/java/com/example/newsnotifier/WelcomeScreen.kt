package com.example.newsnotifier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsnotifier.ui.components.*
import com.example.newsnotifier.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onSignInWithGoogle: () -> Unit,
    onNavigateToChooseAccount: () -> Unit,
    onNavigateToSelection: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()

    GradientBackground(
        isAnimated = true,
        colors = listOf(
            GradientStart,
            GradientMiddle,
            GradientEnd
        )
    ) {
        // Floating elements for visual appeal
        FloatingElements(elementCount = 4)

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "News On The Top",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = TextOnGradient
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextOnGradient
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Hero Section
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome to the Future",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ),
                            color = TextOnGradient,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Stay ahead with personalized news and real-time updates from your favorite sources",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                lineHeight = 26.sp
                            ),
                            color = TextOnGradient.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sign in with Google Button
                    AnimatedGradientButton(
                        text = "Continue with Google",
                        onClick = onSignInWithGoogle,
                        icon = Icons.Filled.AccountCircle,
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = listOf(
                            Color(0xFF4285F4),
                            Color(0xFF1976D2)
                        )
                    )

                    // Choose Account Button
                    AnimatedOutlinedButton(
                        text = "Choose Account",
                        onClick = onNavigateToChooseAccount,
                        icon = Icons.Filled.Email,
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = Color.White,
                        textColor = Color.White
                    )

                    // Guest Button
                    AnimatedOutlinedButton(
                        text = "Continue as Guest",
                        onClick = onNavigateToSelection,
                        icon = Icons.Filled.PersonOutline,
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = Color.White.copy(alpha = 0.6f),
                        textColor = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Features Section
                ElevatedModernCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Why Choose NOTT?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        FeatureItem(
                            title = "Real-time Updates",
                            description = "Get instant notifications from trusted news sources"
                        )

                        FeatureItem(
                            title = "Personalized Feed",
                            description = "Customize your news experience with your favorite topics"
                        )

                        FeatureItem(
                            title = "Smart Filtering",
                            description = "AI-powered filtering to show you what matters most"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FeatureItem(
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}