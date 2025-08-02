package com.example.newsnotifier.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

/**
 * This service listens for incoming push notifications from Firebase Cloud Messaging (FCM).
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when a message is received.
     *
     * This is triggered when a notification arrives while the app is in the foreground.
     * If the app is in the background or closed, a standard system notification is shown automatically.
     *
     * @param remoteMessage Object representing the message received from FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        NotificationHelper.init(this)

        // Log the message to see the data in Logcat
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Check if the message contains a notification payload.
        remoteMessage.notification?.let { notification ->
            Log.d("FCM", "Message Notification Body: ${notification.body}")

            // Use your existing NotificationHelper to display the notification.
            // This ensures a consistent look and feel for all notifications.
            NotificationHelper.showNotification(
                context = applicationContext,
                title = notification.title ?: "New Notification",
                message = notification.body ?: "",
                notificationId = Random.nextInt(), // Generate a random ID for the notification
                sourceName = "Cloud Service" // You can enhance this later if needed
            )
        }
    }

    /**
     * Called when a new FCM registration token is generated.
     *
     * This token is the unique address for this app instance. You must send it to your
     * backend (Firestore) so you know where to send notifications.
     *
     * @param token The new token.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")

        // If you had a local server to send this token to, you would do it here.
        // sendRegistrationToServer(token)
    }

    companion object {
        /**
         * Saves the FCM token to the current user's document in Firestore.
         * @param token The FCM registration token for this device.
         */
        // This function is no longer needed as we are not using Firestore.
        /*
        fun sendRegistrationToServer(token: String?) {
            if (token == null) return
        }
        */
    }
}