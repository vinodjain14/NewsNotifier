package com.example.newsnotifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.newsnotifier.ui.theme.NewsNotifierTheme
import com.example.newsnotifier.utils.NotificationHelper
import com.example.newsnotifier.utils.SubscriptionManager
import com.example.newsnotifier.workers.SubscriptionWorker
import com.example.newsnotifier.utils.AuthManager
import com.example.newsnotifier.utils.DataFetcher
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

// Define an enum to represent the different screens in our app
enum class Screen {
    Welcome,
    ChooseAccount,
    Selection,
    Manage,
    MyProfile,
    AllNotifications,
    LoggedOut
}

class MainActivity : ComponentActivity() {

    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var workManager: WorkManager
    private lateinit var authManager: AuthManager
    private var initialNotificationId: String? = null

    // ActivityResultLauncher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            // Handle Google Sign-In result
            lifecycleScope.launch {
                val success = authManager.firebaseAuthWithGoogle(data)
                if (success) {
                    // User successfully signed in
                } else {
                    // Handle failure
                }
            }
        }
    }

    // Request notification permission for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, can schedule work
            scheduleSubscriptionWorker()
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscriptionManager = SubscriptionManager(this)
        workManager = WorkManager.getInstance(this)
        authManager = AuthManager(this)

        // Initialize NotificationHelper and DataFetcher early
        NotificationHelper.init(this)
        DataFetcher.init(this)

        // Check for notification ID from the intent that launched the activity
        initialNotificationId = intent.getStringExtra(NotificationHelper.NOTIFICATION_ID_EXTRA)

        // Request notification permission if needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Permission already granted, schedule work
                scheduleSubscriptionWorker()
            }
        } else {
            // For older Android versions, permission is granted at install time
            scheduleSubscriptionWorker()
        }

        setContent {
            // Enable edge-to-edge display for modern UI
            WindowCompat.setDecorFitsSystemWindows(window, false)
            NewsNotifierTheme {
                // Apply system bars padding to avoid content being hidden behind system bars
                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    color = Color.Transparent
                ) {
                    // SnackbarHostState for showing messages across screens
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()

                    // Observe logged-in user state from AuthManager
                    val loggedInUser by authManager.loggedInUserFlow.collectAsState(initial = null)
                    val isLoggedIn = loggedInUser != null

                    // Determine initial screen based on intent or login state
                    var currentScreen by remember {
                        mutableStateOf(
                            when {
                                initialNotificationId != null -> Screen.AllNotifications
                                isLoggedIn -> Screen.Selection
                                else -> Screen.Welcome
                            }
                        )
                    }

                    // Effect to navigate after successful Google Sign-in
                    LaunchedEffect(loggedInUser) {
                        if (loggedInUser != null && currentScreen == Screen.Welcome) {
                            currentScreen = Screen.Selection
                            scope.launch { snackbarHostState.showSnackbar("Signed in as ${loggedInUser?.displayName ?: loggedInUser?.email}") }
                        }
                    }

                    // Callback to update subscriptions and schedule/reschedule the worker
                    val updateSubscriptionsAndScheduleWorker: () -> Unit = {
                        scheduleSubscriptionWorker()
                    }

                    when (currentScreen) {
                        Screen.Welcome -> {
                            WelcomeScreen(
                                onSignInWithGoogle = {
                                    val signInIntent = authManager.getGoogleSignInIntent()
                                    googleSignInLauncher.launch(signInIntent)
                                },
                                onNavigateToChooseAccount = { currentScreen = Screen.ChooseAccount },
                                onNavigateToSelection = { currentScreen = Screen.Selection },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.ChooseAccount -> {
                            ChooseAccountScreen(
                                authManager = authManager,
                                onNavigateToSelection = { currentScreen = Screen.Selection },
                                onNavigateBack = { currentScreen = Screen.Welcome },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.Selection -> {
                            SubscriptionSelectionScreen(
                                subscriptionManager = subscriptionManager,
                                currentActiveSubscriptions = subscriptionManager.subscriptionsFlow.collectAsState().value,
                                onSubscriptionsChanged = updateSubscriptionsAndScheduleWorker,
                                onNavigateToManage = { currentScreen = Screen.Manage },
                                onNavigateToWelcome = { currentScreen = Screen.Welcome },
                                isLoggedIn = isLoggedIn,
                                onLogout = {
                                    authManager.logoutUser()
                                    currentScreen = Screen.LoggedOut
                                    scope.launch { snackbarHostState.showSnackbar("Logged out successfully.") }
                                },
                                onNavigateToProfile = { currentScreen = Screen.MyProfile },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.Manage -> {
                            ManageSubscriptionsScreen(
                                subscriptionManager = subscriptionManager,
                                onSubscriptionsChanged = updateSubscriptionsAndScheduleWorker,
                                onNavigateToSelection = { currentScreen = Screen.Selection },
                                onNavigateToAllNotifications = { currentScreen = Screen.AllNotifications },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.MyProfile -> {
                            MyProfileScreen(
                                authManager = authManager,
                                onNavigateToSelection = { currentScreen = Screen.Selection },
                                onNavigateBack = { currentScreen = Screen.Selection },
                                onLogout = {
                                    currentScreen = Screen.LoggedOut
                                },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.AllNotifications -> {
                            AllNotificationsScreen(
                                onNavigateBack = { currentScreen = Screen.Manage },
                                snackbarHostState = snackbarHostState,
                                notificationIdToFocus = initialNotificationId
                            )
                            // Clear initialNotificationId after it's consumed
                            DisposableEffect(Unit) {
                                onDispose {
                                    initialNotificationId = null
                                }
                            }
                        }
                        Screen.LoggedOut -> {
                            LoggedOutScreen(
                                onNavigateToWelcome = { currentScreen = Screen.Welcome }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Schedules the periodic work for checking subscriptions.
     */
    private fun scheduleSubscriptionWorker() {
        // Define a unique name for your work request
        val workName = "SubscriptionCheckWork"

        // Get the current subscriptions to decide whether to schedule or cancel work
        val subscriptions = subscriptionManager.getSubscriptions()

        if (subscriptions.isNotEmpty()) {
            // Use minimum allowed interval of 15 minutes for PeriodicWorkRequest
            val periodicWorkRequest = PeriodicWorkRequestBuilder<SubscriptionWorker>(
                15, TimeUnit.MINUTES
            )
                .addTag(workName)
                .build()

            // Enqueue the work. ExistingPeriodicWorkPolicy.REPLACE will replace if already scheduled.
            workManager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )
        } else {
            // If no subscriptions, cancel the work
            workManager.cancelUniqueWork(workName)
        }
    }
}