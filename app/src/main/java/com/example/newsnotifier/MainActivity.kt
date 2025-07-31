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
import com.example.newsnotifier.utils.ThemeManager
import com.example.newsnotifier.utils.BackupRestoreManager
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Enhanced Screen enum with new features
enum class Screen {
    Welcome,
    ChooseAccount,
    Selection,
    Manage,
    MyProfile,
    AllNotifications,
    ReadingList,
    AccessibilitySettings,
    BackupRestore,
    LoggedOut
}

class MainActivity : ComponentActivity() {

    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var workManager: WorkManager
    private lateinit var authManager: AuthManager
    private lateinit var themeManager: ThemeManager
    private lateinit var backupRestoreManager: BackupRestoreManager
    private lateinit var readingListManager: ReadingListManager
    private var initialNotificationId: String? = null

    // ActivityResultLauncher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
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
            scheduleSubscriptionWorker()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        subscriptionManager = SubscriptionManager(this)
        workManager = WorkManager.getInstance(this)
        authManager = AuthManager(this)
        themeManager = ThemeManager(this)
        backupRestoreManager = BackupRestoreManager(this)
        readingListManager = ReadingListManager(this)

        // Initialize helpers early
        NotificationHelper.init(this)
        DataFetcher.init(this)

        // Check for notification ID from the intent
        initialNotificationId = intent.getStringExtra(NotificationHelper.NOTIFICATION_ID_EXTRA)

        // Request notification permission if needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scheduleSubscriptionWorker()
            }
        } else {
            scheduleSubscriptionWorker()
        }

        setContent {
            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Use existing NewsNotifierTheme for now
            NewsNotifierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // SnackbarHostState for showing messages across screens
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()

                    // Observe logged-in user state
                    val loggedInUser by authManager.loggedInUserFlow.collectAsState(initial = null)
                    val isLoggedIn = loggedInUser != null

                    // Determine initial screen
                    var currentScreen by remember {
                        mutableStateOf(
                            when {
                                initialNotificationId != null -> Screen.AllNotifications
                                isLoggedIn -> Screen.Selection
                                else -> Screen.Welcome
                            }
                        )
                    }

                    // Navigation after successful sign-in
                    LaunchedEffect(loggedInUser) {
                        if (loggedInUser != null && currentScreen == Screen.Welcome) {
                            currentScreen = Screen.Selection
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Signed in as ${loggedInUser?.displayName ?: loggedInUser?.email}"
                                )
                            }
                        }
                    }

                    // Callback to update subscriptions and schedule worker
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
                                onLogout = { currentScreen = Screen.LoggedOut },
                                onNavigateToAccessibility = { currentScreen = Screen.AccessibilitySettings },
                                onNavigateToBackupRestore = { currentScreen = Screen.BackupRestore },
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
                        Screen.ReadingList -> {
                            ReadingListScreen(
                                onNavigateBack = {
                                    currentScreen = if (initialNotificationId != null) {
                                        Screen.AllNotifications
                                    } else {
                                        Screen.Manage
                                    }
                                },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.AccessibilitySettings -> {
                            AccessibilitySettingsScreen(
                                themeManager = themeManager,
                                onNavigateBack = { currentScreen = Screen.MyProfile },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        Screen.BackupRestore -> {
                            BackupRestoreScreen(
                                backupManager = backupRestoreManager,
                                onNavigateBack = { currentScreen = Screen.MyProfile },
                                snackbarHostState = snackbarHostState
                            )
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
        val workName = "SubscriptionCheckWork"
        val subscriptions = subscriptionManager.getSubscriptions()

        if (subscriptions.isNotEmpty()) {
            val periodicWorkRequest = PeriodicWorkRequestBuilder<SubscriptionWorker>(
                15, TimeUnit.MINUTES
            )
                .addTag(workName)
                .build()

            workManager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )
        } else {
            workManager.cancelUniqueWork(workName)
        }
    }

    // In your NavHost setup (usually in MainActivity or a Navigation file):
    @Composable
    fun NewsNotifierNavigation(
        navController: NavHostController,
        // ... other parameters
    ) {
        NavHost(
            navController = navController,
            startDestination = "notifications"
        ) {
            composable("notifications") {
                AllNotificationsScreen(
                    onNavigateToReadingList = {
                        navController.navigate("reading_list")
                    },
                    // ... other parameters
                )
            }

            composable("subscriptions") {
                ManageSubscriptionsScreen(
                    onNavigateToReadingList = {
                        navController.navigate("reading_list")
                    },
                    // ... other parameters
                )
            }

            composable("reading_list") {
                ReadingListScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    // ... parameters
                )
            }
        }
    }
}