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
import kotlinx.coroutines.flow.update

object NotificationHelper {

    private const val CHANNEL_ID = "pulse_notifications_channel"
    private const val CHANNEL_NAME = "Pulse Notifications"
    const val NOTIFICATION_ID_EXTRA = "notification_id_extra"
    private const val TAG = "NotificationHelper"

    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationItem>> = _notificationsFlow.asStateFlow()

    private lateinit var applicationContext: Context

    fun init(context: Context) {
        if (!::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
            createNotificationChannel()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for Pulse news and updates"
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        sourceName: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NOTIFICATION_ID_EXTRA, notificationId.toString())
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.pulse_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun addNotifications(newNotifications: List<NotificationItem>) {
        _notificationsFlow.update { currentNotifications ->
            val existingIds = currentNotifications.map { it.id }.toSet()
            val uniqueNewNotifications = newNotifications.filterNot { existingIds.contains(it.id) }
            (uniqueNewNotifications + currentNotifications).sortedByDescending { it.timestamp }
        }
        Log.d(TAG, "Added ${newNotifications.size} notifications.")
    }

    fun markAsRead(notificationId: String) {
        _notificationsFlow.update { notifications ->
            notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        _notificationsFlow.update { notifications ->
            notifications.filterNot { it.id == notificationId }
        }
    }

    fun clearAllNotifications() {
        _notificationsFlow.value = emptyList()
    }
}
