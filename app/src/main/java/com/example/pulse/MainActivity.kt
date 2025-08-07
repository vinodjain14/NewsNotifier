package com.example.pulse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pulse.data.NotificationItem
import com.example.pulse.ui.theme.AppTypography
import com.example.pulse.ui.theme.PulseTheme
import com.example.pulse.utils.*
import com.example.pulse.workers.SelfRepeatingWorkerManager
import com.example.pulse.workers.SubscriptionWorker
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class Screen {
    Welcome,
    Onboarding,
    ChooseAccount,
    PulseMarket,
    Manage,
    MyProfile,
    AllNotifications,
    ReadingList,
    NotificationDetail,
    AccessibilitySettings,
    BackupRestore,
    LoggedOut,
    Search
}

class MainActivity : ComponentActivity() {

    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var workManager: WorkManager
    private lateinit var authManager: AuthManager
    private lateinit var themeManager: ThemeManager
    private lateinit var readingListManager: ReadingListManager
    private var initialNotificationId: String? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val PREF_USE_FAST_FETCH = "use_fast_fetch"
        private const val FAST_FETCH_INTERVAL = 5L // 5 minutes
        private const val STANDARD_FETCH_INTERVAL = 15L // 15 minutes
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            lifecycleScope.launch {
                authManager.firebaseAuthWithGoogle(data)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permissions", "POST_NOTIFICATIONS permission granted.")
        } else {
            Log.w("Permissions", "POST_NOTIFICATIONS permission denied.")
        }
    }

    private fun isFirstTimeUser(): Boolean {
        val prefs = getSharedPreferences("pulse_prefs", MODE_PRIVATE)
        return prefs.getBoolean("is_first_time", true)
    }

    private fun setFirstTimeUser(isFirstTime: Boolean) {
        val prefs = getSharedPreferences("pulse_prefs", MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("is_first_time", isFirstTime)
            apply()
        }
    }

    private fun useFastFetch(): Boolean {
        val prefs = getSharedPreferences("pulse_prefs", MODE_PRIVATE)
        return prefs.getBoolean(PREF_USE_FAST_FETCH, true) // Default to fast fetch
    }

    private fun setUseFastFetch(enabled: Boolean) {
        val prefs = getSharedPreferences("pulse_prefs", MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean(PREF_USE_FAST_FETCH, enabled)
            apply()
        }
        Log.d(TAG, "Fast fetch ${if (enabled) "enabled" else "disabled"}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        subscriptionManager = SubscriptionManager(this)
        workManager = WorkManager.getInstance(this)
        authManager = AuthManager(this)
        themeManager = ThemeManager(this)
        readingListManager = ReadingListManager(this)
        NotificationHelper.init(this)
        DataFetcher.init(this, subscriptionManager)

        initialNotificationId = intent.getStringExtra(NotificationHelper.NOTIFICATION_ID_EXTRA)
        val isFirstTime = isFirstTimeUser()

        askNotificationPermission()

        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "Current FCM Token: $token")
        }

        setContent {
            val themeMode: ThemeMode by themeManager.themeModeFlow.collectAsState()
            val fontSize: FontSize by themeManager.fontSizeFlow.collectAsState()
            val typography = AppTypography(fontSize = fontSize)

            PulseTheme(
                themeMode = themeMode,
                typography = typography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()
                    val loggedInUser: FirebaseUser? by authManager.loggedInUserFlow.collectAsState()

                    var currentScreen: Screen by remember {
                        mutableStateOf(
                            when {
                                initialNotificationId != null -> Screen.AllNotifications
                                loggedInUser != null -> if (isFirstTime) Screen.Onboarding else Screen.AllNotifications
                                else -> Screen.Welcome
                            }
                        )
                    }

                    // State to hold the selected notification for detail screen
                    var selectedNotification by remember { mutableStateOf<NotificationItem?>(null) }

                    LaunchedEffect(loggedInUser) {
                        val user = loggedInUser
                        if (user != null) {
                            scope.launch(Dispatchers.IO) {
                                val notifications = DataFetcher.fetchAllFeeds()
                                NotificationHelper.addNotifications(notifications)
                            }
                            if (currentScreen == Screen.Welcome) {
                                currentScreen = if (isFirstTime) Screen.Onboarding else Screen.AllNotifications
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Signed in as ${user.displayName ?: user.email}"
                                    )
                                }
                            }
                        }
                    }

                    val updateSubscriptionsAndScheduleWorker: () -> Unit = {
                        configureDataFetching()
                    }

                    // Configure data fetching on app start
                    LaunchedEffect(Unit) {
                        configureDataFetching()
                    }

                    when (currentScreen) {
                        Screen.Welcome -> WelcomeScreen(
                            onSignInWithGoogle = {
                                val signInIntent = authManager.getGoogleSignInIntent()
                                googleSignInLauncher.launch(signInIntent)
                            },
                            onNavigateToChooseAccount = { currentScreen = Screen.ChooseAccount },
                            onNavigateToSelection = {
                                currentScreen = if (isFirstTime) Screen.Onboarding else Screen.AllNotifications
                            },
                            snackbarHostState = snackbarHostState
                        )

                        Screen.Onboarding -> UnifiedSubscriptionScreen(
                            mode = SubscriptionScreenMode.ONBOARDING,
                            subscriptionManager = subscriptionManager,
                            onComplete = {
                                setFirstTimeUser(false)
                                updateSubscriptionsAndScheduleWorker()
                                currentScreen = Screen.AllNotifications
                            }
                        )

                        Screen.ChooseAccount -> ChooseAccountScreen(
                            authManager = authManager,
                            onNavigateToSelection = { currentScreen = Screen.AllNotifications },
                            onNavigateBack = { currentScreen = Screen.Welcome },
                            snackbarHostState = snackbarHostState
                        )

                        Screen.PulseMarket -> PulseMarketScreen(
                            onNavigateToAllNotifications = { currentScreen = Screen.AllNotifications },
                            onNavigateToManageSubscriptions = { currentScreen = Screen.Manage },
                            onNavigateToMyProfile = { currentScreen = Screen.MyProfile }
                        )

                        Screen.Manage -> UnifiedSubscriptionScreen(
                            mode = SubscriptionScreenMode.MANAGEMENT,
                            subscriptionManager = subscriptionManager,
                            onComplete = {
                                updateSubscriptionsAndScheduleWorker()
                                currentScreen = Screen.AllNotifications
                            },
                            onNavigateBack = { currentScreen = Screen.AllNotifications },
                            onNavigateToAllNotifications = { currentScreen = Screen.AllNotifications },
                            onNavigateToReadingList = { currentScreen = Screen.ReadingList },
                            snackbarHostState = snackbarHostState
                        )

                        Screen.MyProfile -> MyProfileScreen(
                            authManager = authManager,
                            onNavigateToSelection = { currentScreen = Screen.AllNotifications },
                            onNavigateBack = { currentScreen = Screen.AllNotifications },
                            onLogout = { currentScreen = Screen.LoggedOut },
                            onNavigateToAccessibility = { currentScreen = Screen.AccessibilitySettings },
                            onNavigateToBackupRestore = { currentScreen = Screen.BackupRestore },
                            snackbarHostState = snackbarHostState
                        )

                        Screen.AllNotifications -> {
                            AllNotificationsScreen(
                                onNavigateToManageSubscriptions = { currentScreen = Screen.Manage },
                                onNavigateToMyProfile = { currentScreen = Screen.MyProfile },
                                onNavigateToReadingList = { currentScreen = Screen.ReadingList },
                                onNavigateToNotificationDetail = { notification ->
                                    selectedNotification = notification
                                    currentScreen = Screen.NotificationDetail
                                },
                                snackbarHostState = snackbarHostState,
                                notificationIdToFocus = initialNotificationId,
                                readingListManager = readingListManager,
                                subscriptionManager = subscriptionManager
                            )
                            DisposableEffect(Unit) {
                                onDispose { initialNotificationId = null }
                            }
                        }

                        Screen.ReadingList -> ReadingListScreen(
                            readingListManager = readingListManager,
                            onNavigateBack = {
                                currentScreen = Screen.AllNotifications
                            },
                            onNavigateToNotificationDetail = { notification ->
                                selectedNotification = notification
                                currentScreen = Screen.NotificationDetail
                            },
                            snackbarHostState = snackbarHostState
                        )

                        Screen.NotificationDetail -> {
                            selectedNotification?.let { notification ->
                                NotificationDetailScreen(
                                    notification = notification,
                                    readingListManager = readingListManager,
                                    onNavigateBack = {
                                        selectedNotification = null
                                        currentScreen = Screen.AllNotifications
                                    },
                                    snackbarHostState = snackbarHostState,
                                    sourceUrl = notification.sourceUrl
                                )
                            }
                        }

                        Screen.AccessibilitySettings -> AccessibilitySettingsScreen(
                            themeManager = themeManager,
                            onNavigateBack = { currentScreen = Screen.MyProfile },
                            snackbarHostState = snackbarHostState
                        )

                        Screen.BackupRestore -> BackupRestoreScreen(
                            subscriptionManager = subscriptionManager,
                            onNavigateBack = { currentScreen = Screen.MyProfile },
                            snackbarHostState = snackbarHostState
                        )

                        Screen.LoggedOut -> LoggedOutScreen(
                            onNavigateToWelcome = { currentScreen = Screen.Welcome }
                        )

                        Screen.Search -> SearchScreen(
                            onNavigateBack = { currentScreen = Screen.AllNotifications },
                            onAddSubscription = { subscription ->
                                subscriptionManager.addSubscription(subscription)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Added ${subscription.name}")
                                }
                                currentScreen = Screen.AllNotifications
                            }
                        )
                    }
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Configure data fetching based on preferences
     */
    private fun configureDataFetching() {
        Log.d(TAG, "Configuring data fetching...")

        // Stop existing workers first
        SelfRepeatingWorkerManager.stopSelfRepeatingWork(this)
        workManager.cancelUniqueWork("SubscriptionCheckWork")

        if (useFastFetch()) {
            // Use self-repeating worker for fast updates
            Log.d(TAG, "Starting fast fetch mode: ${FAST_FETCH_INTERVAL}-minute intervals")
            SelfRepeatingWorkerManager.startSelfRepeatingWork(this, FAST_FETCH_INTERVAL)
        } else {
            // Use standard periodic worker
            Log.d(TAG, "Starting standard fetch mode: ${STANDARD_FETCH_INTERVAL}-minute intervals")
            scheduleStandardSubscriptionWorker()
        }
    }

    /**
     * Schedule standard periodic worker (15+ minute intervals)
     */
    private fun scheduleStandardSubscriptionWorker() {
        val workName = "SubscriptionCheckWork"
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SubscriptionWorker>(
            STANDARD_FETCH_INTERVAL, TimeUnit.MINUTES
        ).addTag(workName).build()

        workManager.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )

        Log.d(TAG, "Scheduled periodic worker for $STANDARD_FETCH_INTERVAL minutes")
    }
}