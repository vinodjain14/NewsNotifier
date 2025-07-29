package com.example.newsnotifier.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsnotifier.utils.DataFetcher // Added import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A WorkManager worker that periodically checks for new content from subscribed sources
 * and sends notifications.
 */
class SubscriptionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            // Initialize DataFetcher if not already done (though MainActivity should do this)
            DataFetcher.init(applicationContext)
            val newContentFound = DataFetcher.fetchAndNotifyNewContent()

            if (newContentFound) {
                Result.success()
            } else {
                Result.success() // Still success, just no new content this time
            }
        }
    }
}
