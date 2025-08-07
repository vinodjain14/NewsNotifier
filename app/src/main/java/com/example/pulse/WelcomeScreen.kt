package com.example.pulse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.ui.theme.PulseBlue
import com.example.pulse.ui.theme.PulsePurple

@Composable
fun WelcomeScreen(
    onSignInWithGoogle: () -> Unit,
    onNavigateToChooseAccount: () -> Unit,
    onNavigateToSelection: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(PulsePurple, PulseBlue)
    )

    // State to manage which authentication method is shown
    var showAuthOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo()
            Spacer(modifier = Modifier.height(40.dp))
            WelcomeCard()
            Spacer(modifier = Modifier.height(32.dp))

            if (!showAuthOptions) {
                // Initial simplified buttons
                SimplifiedAuthButtons(
                    onShowAuthOptions = { showAuthOptions = true },
                    onContinueAsGuest = onNavigateToSelection
                )
            } else {
                // Expanded authentication options
                ExpandedAuthButtons(
                    onSignInWithGoogle = onSignInWithGoogle,
                    onNavigateToChooseAccount = onNavigateToChooseAccount,
                    onContinueAsGuest = onNavigateToSelection,
                    onBack = { showAuthOptions = false }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            FeaturesSection()
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun AppLogo() {
    val gradient = Brush.linearGradient(
        colors = listOf(Color.White, Color.White.copy(alpha = 0.7f)),
    )
    Text(
        text = "Pulse",
        style = TextStyle(
            brush = gradient,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        ),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun WelcomeCard() {
    GlassmorphismCard {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to the Future of News",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Stay ahead with personalized news and real-time updates from your favorite sources.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun SimplifiedAuthButtons(
    onShowAuthOptions: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Primary sign-in button
        GradientButton(
            text = "Sign In",
            icon = Icons.AutoMirrored.Filled.Login,
            onClick = onShowAuthOptions
        )

        // Secondary guest option
        TextButton(
            onClick = onContinueAsGuest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Continue as Guest",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ExpandedAuthButtons(
    onSignInWithGoogle: () -> Unit,
    onNavigateToChooseAccount: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Back to simplified view
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "← Back",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        // Google sign-in
        GradientButton(
            text = "Continue with Google",
            icon = Icons.Filled.AccountCircle,
            onClick = onSignInWithGoogle
        )

        // Choose account option
        GlassButton(
            text = "Choose Account",
            icon = Icons.AutoMirrored.Filled.Login,
            onClick = onNavigateToChooseAccount
        )

        // Guest option
        GlassButton(
            text = "Continue as Guest",
            icon = Icons.Filled.Person,
            onClick = onContinueAsGuest
        )
    }
}

@Composable
private fun FeaturesSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Why Choose Pulse?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FeatureCard(
                icon = Icons.Filled.Speed,
                title = "Real-time Updates",
                description = "Get instant notifications from trusted news sources and personalities."
            )
            FeatureCard(
                icon = Icons.Filled.Tune,
                title = "Personalized Feed",
                description = "Customize your news experience with your favorite topics and channels."
            )
            FeatureCard(
                icon = Icons.Filled.CloudSync,
                title = "Smart Sync",
                description = "Backup and restore your subscriptions and reading list effortlessly."
            )
        }
    }
}

@Composable
private fun GradientButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF4285F4), Color(0xFF34A853))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GlassButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, description: String) {
    GlassmorphismCard {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PulsePurple, PulseBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun GlassmorphismCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}