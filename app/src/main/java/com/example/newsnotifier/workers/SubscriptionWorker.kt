package com.example.newsnotifier.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsnotifier.data.SubscriptionType // Added import for SubscriptionType
import com.example.newsnotifier.utils.DataFetcher
import com.example.newsnotifier.utils.NotificationHelper
import com.example.newsnotifier.utils.SubscriptionManager
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
                val subscriptions = subscriptionManager.getSubscriptions()
                if (subscriptions.isEmpty()) {
                    // No subscriptions, so nothing to do. Success.
                    return@withContext Result.success()
                }

                subscriptions.forEach { subscription ->
                    when (subscription.type) {
                        SubscriptionType.RSS_FEED -> { // Explicitly use SubscriptionType.RSS_FEED
                            val newArticles = dataFetcher.fetchRssFeed(subscription.sourceUrl)
                            newArticles.forEach { article ->
                                // Show notification for each new article
                                NotificationHelper.showNotification(
                                    applicationContext,
                                    "New Article from ${subscription.name}",
                                    article.title, // Assuming article.title is the message
                                    notificationIdCounter.incrementAndGet(),
                                    sourceName = subscription.name,
                                    isBreaking = article.isBreaking, // Pass breaking status from fetched article
                                    isNew = article.isNew // Pass new status from fetched article
                                )
                            }
                        }
                        SubscriptionType.TWITTER -> { // Explicitly use SubscriptionType.TWITTER
                            // Assuming sourceUrl for Twitter is the username
                            val newTweets = dataFetcher.fetchTweets(subscription.sourceUrl)
                            newTweets.forEach { tweet ->
                                // Show notification for each new tweet
                                NotificationHelper.showNotification(
                                    applicationContext,
                                    "New Tweet from @${subscription.name}", // Use subscription.name for display
                                    tweet.text,
                                    notificationIdCounter.incrementAndGet(),
                                    sourceName = subscription.name,
                                    // For simplicity, assuming tweets are not "breaking" or "new" unless specified by API
                                    isBreaking = false,
                                    isNew = true // Consider all fetched tweets as new for now
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
