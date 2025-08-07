package com.example.pulse.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.pulse.utils.DataFetcher
import com.example.pulse.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Self-repeating worker that bypasses the 15-minute PeriodicWorkRequest limitation
 * by scheduling OneTimeWorkRequests in a chain with custom intervals
 */
class SelfRepeatingSubscriptionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SelfRepeatingWorker"
        const val WORK_NAME = "SelfRepeatingSubscriptionWork"
        const val RETRY_COUNT_KEY = "retry_count"
        const val INTERVAL_MINUTES_KEY = "interval_minutes"
        const val MAX_RETRY_ATTEMPTS = 3
        const val DEFAULT_INTERVAL_MINUTES = 5L

        // Work tags for easy identification and cancellation
        const val SELF_REPEATING_TAG = "self_repeating_subscription"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SelfRepeatingSubscriptionWorker started")

        return try {
            // Get configuration from input data
            val intervalMinutes = inputData.getLong(INTERVAL_MINUTES_KEY, DEFAULT_INTERVAL_MINUTES)
            val currentRetryCount = inputData.getInt(RETRY_COUNT_KEY, 0)

            // Perform the actual work
            val workResult = performSubscriptionFetch()

            when (workResult) {
                is WorkResult.Success -> {
                    Log.d(TAG, "Successfully fetched ${workResult.notificationCount} notifications")

                    // Schedule the next execution with original interval
                    scheduleNextExecution(intervalMinutes, 0) // Reset retry count on success
                    Result.success()
                }
                is WorkResult.Failure -> {
                    Log.w(TAG, "Failed to fetch subscriptions: ${workResult.error}")

                    if (currentRetryCount < MAX_RETRY_ATTEMPTS) {
                        // Retry with exponential backoff
                        val retryDelay = calculateRetryDelay(currentRetryCount)
                        Log.d(TAG, "Scheduling retry attempt ${currentRetryCount + 1} in $retryDelay minutes")

                        scheduleNextExecution(retryDelay, currentRetryCount + 1, intervalMinutes)
                        Result.success() // Return success to avoid WorkManager's built-in retry
                    } else {
                        Log.e(TAG, "Max retry attempts reached. Scheduling next normal execution.")
                        // Schedule next normal execution despite failure
                        scheduleNextExecution(intervalMinutes, 0)
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in SelfRepeatingSubscriptionWorker", e)

            // Schedule next execution even on unexpected errors to maintain the cycle
            val intervalMinutes = inputData.getLong(INTERVAL_MINUTES_KEY, DEFAULT_INTERVAL_MINUTES)
            scheduleNextExecution(intervalMinutes, 0)

            Result.failure()
        }
    }

    /**
     * Performs the actual subscription fetching work
     */
    private suspend fun performSubscriptionFetch(): WorkResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting subscription fetch...")

                // Check if DataFetcher is properly initialized
                if (!DataFetcher.isInitialized()) {
                    return@withContext WorkResult.Failure("DataFetcher not initialized")
                }

                // Fetch all feeds
                val notifications = DataFetcher.fetchAllFeeds()

                if (notifications.isEmpty()) {
                    Log.d(TAG, "No new notifications found")
                    return@withContext WorkResult.Success(0)
                }

                // Add notifications to the system
                NotificationHelper.addNotifications(notifications)

                Log.d(TAG, "Successfully processed ${notifications.size} notifications")
                WorkResult.Success(notifications.size)

            } catch (e: Exception) {
                Log.e(TAG, "Error during subscription fetch", e)
                WorkResult.Failure("Network error: ${e.message}")
            }
        }
    }

    /**
     * Schedules the next execution of this worker
     */
    private fun scheduleNextExecution(
        delayMinutes: Long,
        retryCount: Int,
        originalIntervalMinutes: Long = DEFAULT_INTERVAL_MINUTES
    ) {
        val inputData = Data.Builder()
            .putInt(RETRY_COUNT_KEY, retryCount)
            .putLong(INTERVAL_MINUTES_KEY, originalIntervalMinutes)
            .build()

        val nextWorkRequest = OneTimeWorkRequestBuilder<SelfRepeatingSubscriptionWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .addTag(SELF_REPEATING_TAG)
            .setConstraints(createWorkConstraints())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE, // Replace any existing work
                nextWorkRequest
            )

        Log.d(TAG, "Scheduled next execution in $delayMinutes minutes (retry count: $retryCount)")
    }

    /**
     * Calculate exponential backoff delay for retries
     */
    private fun calculateRetryDelay(retryCount: Int): Long {
        return when (retryCount) {
            0 -> 1L // First retry after 1 minute
            1 -> 2L // Second retry after 2 minutes
            2 -> 5L // Third retry after 5 minutes
            else -> DEFAULT_INTERVAL_MINUTES // Fall back to normal interval
        }
    }

    /**
     * Create work constraints for network and battery optimization
     */
    private fun createWorkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false) // Allow when battery is low for important news
            .setRequiresStorageNotLow(true)
            .build()
    }

    /**
     * Sealed class to represent work results
     */
    private sealed class WorkResult {
        data class Success(val notificationCount: Int) : WorkResult()
        data class Failure(val error: String) : WorkResult()
    }
}

/**
 * Manager class to control the self-repeating worker
 */
object SelfRepeatingWorkerManager {
    private const val TAG = "SelfRepeatingManager"

    /**
     * Starts the self-repeating subscription worker with custom interval
     */
    fun startSelfRepeatingWork(context: Context, intervalMinutes: Long = 5L) {
        Log.d(TAG, "Starting self-repeating work with $intervalMinutes minute interval")

        val inputData = Data.Builder()
            .putLong(SelfRepeatingSubscriptionWorker.INTERVAL_MINUTES_KEY, intervalMinutes)
            .putInt(SelfRepeatingSubscriptionWorker.RETRY_COUNT_KEY, 0)
            .build()

        val initialWorkRequest = OneTimeWorkRequestBuilder<SelfRepeatingSubscriptionWorker>()
            .setInputData(inputData)
            .addTag(SelfRepeatingSubscriptionWorker.SELF_REPEATING_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SelfRepeatingSubscriptionWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                initialWorkRequest
            )

        Log.d(TAG, "Self-repeating work started successfully")
    }

    /**
     * Stops the self-repeating worker completely
     */
    fun stopSelfRepeatingWork(context: Context) {
        Log.d(TAG, "Stopping self-repeating work")

        WorkManager.getInstance(context).apply {
            // Cancel by unique work name
            cancelUniqueWork(SelfRepeatingSubscriptionWorker.WORK_NAME)

            // Cancel by tag as backup
            cancelAllWorkByTag(SelfRepeatingSubscriptionWorker.SELF_REPEATING_TAG)
        }

        Log.d(TAG, "Self-repeating work stopped")
    }

    /**
     * Checks if the self-repeating worker is currently active
     */
    fun isWorkActive(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SelfRepeatingSubscriptionWorker.WORK_NAME)
            .get()

        return workInfos.any { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        }
    }

    /**
     * Gets the current status of the self-repeating worker
     */
    fun getWorkStatus(context: Context, callback: (WorkInfo?) -> Unit) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(SelfRepeatingSubscriptionWorker.WORK_NAME)
            .observeForever { workInfos ->
                callback(workInfos?.firstOrNull())
            }
    }

    /**
     * Triggers an immediate execution while maintaining the schedule
     */
    fun triggerImmediateExecution(context: Context) {
        Log.d(TAG, "Triggering immediate execution")

        val immediateWorkRequest = OneTimeWorkRequestBuilder<SelfRepeatingSubscriptionWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(SelfRepeatingSubscriptionWorker.INTERVAL_MINUTES_KEY, 5L)
                    .putInt(SelfRepeatingSubscriptionWorker.RETRY_COUNT_KEY, 0)
                    .build()
            )
            .addTag("immediate_execution")
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
    }
}