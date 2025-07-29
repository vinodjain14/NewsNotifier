package com.example.newsnotifier.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.newsnotifier.R
import com.example.newsnotifier.data.NotificationItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import android.content.Intent
import android.app.PendingIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper class for creating and managing notifications.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "news_notifier_channel"
    private const val CHANNEL_NAME = "News Notifier Updates"
    private const val CHANNEL_DESCRIPTION = "Notifications for new news and tweets"
    private const val NOTIFICATIONS_PREFS = "notifications_prefs"
    private const val NOTIFICATIONS_KEY = "notifications_list"
    const val NOTIFICATION_ID_EXTRA = "notification_id_extra"

    private lateinit var applicationContext: Context
    private val gson = Gson()

    // MutableStateFlow to hold the current list of notifications and emit updates
    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationItem>> = _notificationsFlow.asStateFlow()

    /**
     * Initializes the NotificationHelper with application context.
     * This should be called once early in the application lifecycle (e.g., in MainActivity's onCreate).
     */
    fun init(context: Context) {
        if (!::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
            createNotificationChannel(applicationContext)
            // Load initial notifications into the flow
            _notificationsFlow.value = loadNotificationsFromPrefs()
        }
    }

    /**
     * Creates a notification channel for Android O (API 26) and above.
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a notification to the user and saves it to SharedPreferences.
     * @param context The application context.
     * @param title The title of the notification.
     * @param message The main text of the notification.
     * @param notificationId A unique ID for this notification.
     */
    fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        // Ensure init has been called
        if (!::applicationContext.isInitialized) {
            init(context.applicationContext)
        }

        // Check for POST_NOTIFICATIONS permission on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, cannot show notification.
            return
        }

        // Create a NotificationItem to store
        val notificationItem = NotificationItem(UUID.randomUUID().toString(), title, message)
        saveNotification(notificationItem) // Save and update flow before building the notification

        // Create an Intent to open MainActivity when the notification is tapped
        val intent = Intent(context, com.example.newsnotifier.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NOTIFICATION_ID_EXTRA, notificationItem.id) // Pass the ID of the saved notification
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // Use the notificationId as request code for unique pending intents
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    /**
     * Saves a single notification to SharedPreferences and updates the flow.
     */
    private fun saveNotification(notification: NotificationItem) {
        val currentList = _notificationsFlow.value.toMutableList()
        currentList.add(0, notification) // Add to the beginning for newest first
        // Optional: Limit the number of stored notifications to prevent excessive storage
        val maxNotifications = 50
        while (currentList.size > maxNotifications) {
            currentList.removeAt(currentList.size - 1)
        }
        val json = gson.toJson(currentList)
        applicationContext.getSharedPreferences(NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(NOTIFICATIONS_KEY, json)
            .apply()
        _notificationsFlow.value = currentList // Update the flow
    }

    /**
     * Loads all stored notifications from SharedPreferences (internal helper).
     */
    private fun loadNotificationsFromPrefs(): List<NotificationItem> {
        val json = applicationContext.getSharedPreferences(NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
            .getString(NOTIFICATIONS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<NotificationItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    /**
     * Clears all stored notifications from SharedPreferences and updates the flow.
     */
    fun clearAllNotifications() {
        if (!::applicationContext.isInitialized) {
            return
        }
        applicationContext.getSharedPreferences(NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(NOTIFICATIONS_KEY)
            .apply()
        _notificationsFlow.value = emptyList() // Clear the flow
    }
}
