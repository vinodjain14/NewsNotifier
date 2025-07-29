package com.example.newsnotifier

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope // Added import for rememberCoroutineScope
import kotlinx.coroutines.launch // Import for snackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onSignInWithGoogle: () -> Unit,
    onNavigateToChooseAccount: () -> Unit, // New parameter for navigating to ChooseAccountScreen
    onNavigateToSelection: () -> Unit, // For "Continue as Guest"
    snackbarHostState: SnackbarHostState // Added snackbar host state
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "News On The Top",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to the Finance News Notifier!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Sign in with Google Button
            Button(
                onClick = onSignInWithGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Sign in with Google", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))

            // Choose Account Button (for email/password or other accounts)
            OutlinedButton(
                onClick = onNavigateToChooseAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Choose Account", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))

            // Continue as a Guest Button
            TextButton(
                onClick = onNavigateToSelection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue as a guest", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
