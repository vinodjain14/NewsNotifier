package com.example.pulse.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pulse.MainActivity
import com.example.pulse.R
import com.example.pulse.data.NotificationItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

object NotificationHelper {

    private const val CHANNEL_ID = "pulse_channel"
    private const val CHANNEL_NAME = "Pulse Updates"
    private const val CHANNEL_DESCRIPTION = "Notifications for new news and updates"
    private const val NOTIFICATIONS_PREFS = "notifications_prefs"
    private const val NOTIFICATIONS_KEY = "notifications_list"
    const val NOTIFICATION_ID_EXTRA = "notification_id_extra"

    private lateinit var applicationContext: Context
    private val gson = Gson()

    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationItem>> = _notificationsFlow.asStateFlow()

    fun init(context: Context) {
        if (!::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
            createNotificationChannel(applicationContext)
            _notificationsFlow.value = loadNotificationsFromPrefs()
        }
    }

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

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        sourceName: String,
        isBreaking: Boolean = false,
        isNew: Boolean = false
    ) {
        if (!::applicationContext.isInitialized) {
            init(context.applicationContext)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationItem = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            sourceName = sourceName,
            isBreaking = isBreaking,
            isNew = isNew
        )
        saveNotification(notificationItem)

        // --- FIX: Corrected the Intent to properly reference MainActivity ---
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NOTIFICATION_ID_EXTRA, notificationItem.id)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
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

    private fun saveNotification(notification: NotificationItem) {
        val currentList = _notificationsFlow.value.toMutableList()
        currentList.add(0, notification)
        val maxNotifications = 100
        while (currentList.size > maxNotifications) {
            currentList.removeAt(currentList.size - 1)
        }
        updateNotifications(currentList)
    }

    fun markAsRead(notificationId: String) {
        val currentList = _notificationsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isRead = true)
            updateNotifications(currentList)
        }
    }

    fun deleteNotification(notificationId: String) {
        val currentList = _notificationsFlow.value.toMutableList()
        currentList.removeAll { it.id == notificationId }
        updateNotifications(currentList)
    }

    private fun updateNotifications(newList: List<NotificationItem>) {
        val json = gson.toJson(newList)
        applicationContext.getSharedPreferences(NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(NOTIFICATIONS_KEY, json)
            .apply()
        _notificationsFlow.value = newList
    }

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

    fun clearAllNotifications() {
        if (!::applicationContext.isInitialized) {
            return
        }
        updateNotifications(emptyList())
    }
}