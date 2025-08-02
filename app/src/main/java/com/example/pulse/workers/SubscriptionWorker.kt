package com.example.pulse.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pulse.data.Subscription
import com.example.pulse.data.SubscriptionType
import com.example.pulse.utils.DataFetcher
import com.example.pulse.utils.NotificationHelper
import com.example.pulse.utils.SubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * A WorkManager worker that periodically fetches data for active subscriptions
 * and shows notifications for new content.
 */
class SubscriptionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val subscriptionManager = SubscriptionManager(appContext)
    private val dataFetcher = DataFetcher // DataFetcher is an object, no need to instantiate
    private val notificationIdCounter = AtomicInteger(0) // Simple counter for unique notification IDs

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // *** FIX: Get subscriptions from the flow's current value ***
                val subscriptions = subscriptionManager.subscriptionsFlow.value

                if (subscriptions.isEmpty()) {
                    // No subscriptions, so nothing to do. Success.
                    return@withContext Result.success()
                }

                subscriptions.forEach { subscription: Subscription ->
                    when (subscription.type) {
                        SubscriptionType.RSS_FEED -> {
                            val newArticles = dataFetcher.fetchRssFeed(subscription.sourceUrl)
                            newArticles.forEach { article ->
                                // Show notification for each new article
                                NotificationHelper.showNotification(
                                    applicationContext,
                                    "New Article from ${subscription.name}",
                                    article.title,
                                    notificationIdCounter.incrementAndGet(),
                                    sourceName = subscription.name,
                                    isBreaking = article.isBreaking,
                                    isNew = article.isNew
                                )
                            }
                        }
                        SubscriptionType.TWITTER -> {
                            val newTweets = dataFetcher.fetchTweets(subscription.sourceUrl)
                            newTweets.forEach { tweet ->
                                // Show notification for each new tweet
                                NotificationHelper.showNotification(
                                    applicationContext,
                                    "New Tweet from @${subscription.name}",
                                    tweet.text,
                                    notificationIdCounter.incrementAndGet(),
                                    sourceName = subscription.name,
                                    isBreaking = false,
                                    isNew = true
                                )
                            }
                        }
                    }
                }
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry() // Retry if there's an error
            }
        }
    }
}
