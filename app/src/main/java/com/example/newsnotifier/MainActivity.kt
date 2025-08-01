package com.example.newsnotifier

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
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.newsnotifier.ui.theme.AppTypography
import com.example.newsnotifier.ui.theme.NewsNotifierTheme
import com.example.newsnotifier.utils.*
import com.example.newsnotifier.workers.SubscriptionWorker
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Define Screen enum at the top level of the file, outside the MainActivity class.
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
    private lateinit var readingListManager: ReadingListManager
    private var initialNotificationId: String? = null

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
            scheduleSubscriptionWorker()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        subscriptionManager = SubscriptionManager(this)
        workManager = WorkManager.getInstance(this)
        authManager = AuthManager(this)
        themeManager = ThemeManager(this)
        readingListManager = ReadingListManager(this)

        NotificationHelper.init(this)
        DataFetcher.init(this)

        initialNotificationId = intent.getStringExtra(NotificationHelper.NOTIFICATION_ID_EXTRA)

        // Get the current FCM token and save it if a user is logged in
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", "Current FCM Token: $token")
            MyFirebaseMessagingService.sendRegistrationToServer(token)
        }


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
            // Explicitly define the types for the state collectors to help the compiler.
            val themeMode: ThemeMode by themeManager.themeModeFlow.collectAsState()
            val fontSize: FontSize by themeManager.fontSizeFlow.collectAsState()
            val typography = AppTypography(fontSize = fontSize)

            NewsNotifierTheme(
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
                    val isLoggedIn = loggedInUser != null

                    var currentScreen: Screen by remember {
                        mutableStateOf(
                            when {
                                initialNotificationId != null -> Screen.AllNotifications
                                isLoggedIn -> Screen.Selection
                                else -> Screen.Welcome
                            }
                        )
                    }

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

                    val updateSubscriptionsAndScheduleWorker: () -> Unit = {
                        scheduleSubscriptionWorker()
                    }

                    when (currentScreen) {
                        Screen.Welcome -> WelcomeScreen(
                            onSignInWithGoogle = {
                                val signInIntent = authManager.getGoogleSignInIntent()
                                googleSignInLauncher.launch(signInIntent)
                            },
                            onNavigateToChooseAccount = { currentScreen = Screen.ChooseAccount },
                            onNavigateToSelection = { currentScreen = Screen.Selection },
                            snackbarHostState = snackbarHostState
                        )
                        Screen.ChooseAccount -> ChooseAccountScreen(
                            authManager = authManager,
                            onNavigateToSelection = { currentScreen = Screen.Selection },
                            onNavigateBack = { currentScreen = Screen.Welcome },
                            snackbarHostState = snackbarHostState
                        )
                        Screen.Selection -> SubscriptionSelectionScreen(
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
                        Screen.Manage -> ManageSubscriptionsScreen(
                            subscriptionManager = subscriptionManager,
                            onSubscriptionsChanged = updateSubscriptionsAndScheduleWorker,
                            onNavigateToSelection = { currentScreen = Screen.Selection },
                            onNavigateToAllNotifications = { currentScreen = Screen.AllNotifications },
                            onNavigateToReadingList = { currentScreen = Screen.ReadingList },
                            snackbarHostState = snackbarHostState
                        )
                        Screen.MyProfile -> MyProfileScreen(
                            authManager = authManager,
                            onNavigateToSelection = { currentScreen = Screen.Selection },
                            onNavigateBack = { currentScreen = Screen.Selection },
                            onLogout = { currentScreen = Screen.LoggedOut },
                            onNavigateToAccessibility = { currentScreen = Screen.AccessibilitySettings },
                            onNavigateToBackupRestore = { currentScreen = Screen.BackupRestore },
                            snackbarHostState = snackbarHostState
                        )
                        Screen.AllNotifications -> {
                            AllNotificationsScreen(
                                onNavigateBack = { currentScreen = Screen.Manage },
                                snackbarHostState = snackbarHostState,
                                notificationIdToFocus = initialNotificationId,
                                onNavigateToReadingList = { currentScreen = Screen.ReadingList }
                            )
                            DisposableEffect(Unit) {
                                onDispose { initialNotificationId = null }
                            }
                        }
                        Screen.ReadingList -> ReadingListScreen(
                            readingListManager = readingListManager,
                            onNavigateBack = {
                                currentScreen = if (initialNotificationId != null) {
                                    Screen.AllNotifications
                                } else {
                                    Screen.Manage
                                }
                            },
                            snackbarHostState = snackbarHostState
                        )
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
                    }
                }
            }
        }
    }

    private fun scheduleSubscriptionWorker() {
        // This function is now simplified. We no longer need to check the number of subscriptions
        // here because the background worker is designed to handle the case of zero subscriptions gracefully.
        // We just need to ensure it's scheduled.
        val workName = "SubscriptionCheckWork"

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SubscriptionWorker>(15, TimeUnit.MINUTES)
            .addTag(workName)
            .build()

        workManager.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }
}
