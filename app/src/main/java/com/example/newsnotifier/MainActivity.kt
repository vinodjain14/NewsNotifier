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

// Define an enum to represent the different screens in our app
enum class Screen {
    Welcome,
    CreateAccount,
    Login,
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
    private var initialNotificationId: String? = null // To store notification ID from intent

    // Request notification permission for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, can schedule work
            scheduleSubscriptionWorker()
        } else {
            // Permission denied, inform user or disable notification features
            // You might show a Snackbar or AlertDialog here
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscriptionManager = SubscriptionManager(this)
        workManager = WorkManager.getInstance(this)
        authManager = AuthManager(this)

        // Initialize NotificationHelper and DataFetcher early
        NotificationHelper.init(this) // Crucial for initializing notificationsFlow
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
            NewsNotifierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Determine initial screen based on intent
                    var currentScreen by remember {
                        mutableStateOf(
                            if (initialNotificationId != null) Screen.AllNotifications else Screen.Welcome
                        )
                    }

                    // Observe current subscriptions from the manager's flow
                    val currentSubscriptions by subscriptionManager.subscriptionsFlow.collectAsState(initial = emptyList())
                    // Observe logged-in user state from AuthManager
                    val loggedInUser by authManager.loggedInUserFlow.collectAsState(initial = null)
                    val isLoggedIn = loggedInUser != null

                    // SnackbarHostState for showing messages across screens
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()


                    // Callback to update subscriptions and schedule/reschedule the worker
                    val updateSubscriptionsAndScheduleWorker: () -> Unit = {
                        scheduleSubscriptionWorker()
                    }

                    when (currentScreen) {
                        Screen.Welcome -> {
                            WelcomeScreen(
                                onNavigateToSelection = { currentScreen = Screen.Selection },
                                onLoginClick = { currentScreen = Screen.Login },
                                onCreateAccountClick = { currentScreen = Screen.CreateAccount }
                            )
                        }
                        Screen.CreateAccount -> {
                            CreateAccountScreen(
                                authManager = authManager,
                                onAccountCreated = { currentScreen = Screen.Login },
                                onNavigateBack = { currentScreen = Screen.Welcome },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.Login -> {
                            LoginScreen(
                                authManager = authManager,
                                onLoginSuccess = { currentScreen = Screen.Selection },
                                onNavigateBack = { currentScreen = Screen.Welcome },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.Selection -> {
                            SubscriptionSelectionScreen(
                                subscriptionManager = subscriptionManager,
                                currentActiveSubscriptions = currentSubscriptions,
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
                                onNavigateBack = { currentScreen = Screen.Selection }
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
            val periodicWorkRequest = PeriodicWorkRequestBuilder<SubscriptionWorker>(
                15, TimeUnit.MINUTES
            )
                .addTag(workName)
                .build()

            // Enqueue the work. ExistingPeriodicWorkPolicy.UPDATE will update if already scheduled.
            workManager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
        } else {
            // If no subscriptions, cancel the work
            workManager.cancelUniqueWork(workName)
        }
    }
}
