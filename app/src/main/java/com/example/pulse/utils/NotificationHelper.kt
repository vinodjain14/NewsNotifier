package com.example.pulse.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pulse.MainActivity
import com.example.pulse.R
import com.example.pulse.data.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationHelper {

    const val CHANNEL_ID = "pulse_notifications"
    const val NOTIFICATION_ID_EXTRA = "notification_id"
    private const val TAG = "NotificationHelper"

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationItem>> = _notificationsFlow.asStateFlow()

    fun init(appContext: Context) {
        context = appContext
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pulse Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "News and market updates from Pulse"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun addNotifications(notifications: List<NotificationItem>) {
        val currentNotifications = _notificationsFlow.value.toMutableList()

        // Add only new notifications (avoid duplicates)
        val newNotifications = notifications.filter { newNotif ->
            currentNotifications.none { existingNotif -> existingNotif.id == newNotif.id }
        }

        currentNotifications.addAll(0, newNotifications) // Add to beginning
        _notificationsFlow.value = currentNotifications.sortedByDescending { it.timestamp }

        Log.d(TAG, "Added ${newNotifications.size} new notifications. Total: ${currentNotifications.size}")
    }

    fun markNotificationAsRead(notificationId: String) {
        val currentNotifications = _notificationsFlow.value.toMutableList()
        val index = currentNotifications.indexOfFirst { it.id == notificationId }

        if (index != -1) {
            currentNotifications[index] = currentNotifications[index].markAsRead()
            _notificationsFlow.value = currentNotifications
            Log.d(TAG, "Marked notification as read: $notificationId")
        }
    }

    fun toggleNotificationSaved(notificationId: String) {
        val currentNotifications = _notificationsFlow.value.toMutableList()
        val index = currentNotifications.indexOfFirst { it.id == notificationId }

        if (index != -1) {
            currentNotifications[index] = currentNotifications[index].toggleSaved()
            _notificationsFlow.value = currentNotifications
            Log.d(TAG, "Toggled saved status for notification: $notificationId")
        }
    }

    fun clearAllNotifications() {
        _notificationsFlow.value = emptyList()
        Log.d(TAG, "Cleared all notifications")
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        sourceName: String
    ) {
        val notificationItem = NotificationItem(
            id = notificationId.toString(),
            title = title,
            message = message,
            sourceName = sourceName,
            market = "Cloud",
            category = "Push Notifications",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isSaved = false,
            isBreaking = false,
            isNew = true
        )

        // Add to internal list
        addNotifications(listOf(notificationItem))

        // Show system notification
        showSystemNotification(notificationItem)
    }

    fun showSystemNotification(notificationItem: NotificationItem) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(NOTIFICATION_ID_EXTRA, notificationItem.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationItem.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(notificationItem.title)
            .setContentText(notificationItem.message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(if (notificationItem.isBreaking) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationItem.id.hashCode(), notification)
    }
}