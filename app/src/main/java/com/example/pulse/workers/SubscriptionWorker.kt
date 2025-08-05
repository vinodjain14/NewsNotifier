package com.example.pulse.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pulse.utils.DataFetcher
import com.example.pulse.utils.NotificationHelper
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SubscriptionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SubscriptionWorker", "Worker started.")
        try {
            val allNewNotifications = DataFetcher.fetchAllFeeds()

            if (allNewNotifications.isNotEmpty()) {
                Log.d("SubscriptionWorker", "Found ${allNewNotifications.size} new notifications.")
                // Add all new notifications to the central helper so they appear in the app's list
                NotificationHelper.addNotifications(allNewNotifications)

                // --- FILTER FOR PUSH NOTIFICATIONS ---
                val oneHourInMillis = TimeUnit.HOURS.toMillis(1)
                val currentTime = System.currentTimeMillis()

                val recentNotifications = allNewNotifications.filter {
                    (currentTime - it.timestamp) < oneHourInMillis
                }
                // --- END FILTER ---

                // Only show push notifications for items published in the last hour
                if (recentNotifications.isNotEmpty()) {
                    Log.d("SubscriptionWorker", "Found ${recentNotifications.size} recent notifications to notify about.")
                    // Show a notification for the most recent item
                    val notificationToShow = recentNotifications.first()
                    NotificationHelper.showNotification(
                        context = applicationContext,
                        title = notificationToShow.title,
                        message = notificationToShow.message,
                        notificationId = Random.nextInt(),
                        sourceName = notificationToShow.sourceName
                    )
                } else {
                    Log.d("SubscriptionWorker", "No recent (last 1 hour) notifications found to send a push notification for.")
                }
            } else {
                Log.d("SubscriptionWorker", "No new notifications found.")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("SubscriptionWorker", "Error in worker: ${e.message}", e)
            return Result.failure()
        }
    }
}
