package com.example.pulse.utils

import android.text.format.DateUtils
import java.util.concurrent.TimeUnit

object TimeUtils {
    /**
     * Formats a given timestamp into a relative string (e.g., "5m ago", "2h ago").
     * @param timestamp The timestamp in milliseconds.
     * @return A formatted, relative time string.
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val difference = now - timestamp

        return when {
            difference < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            difference < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(difference)}m ago"
            difference < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(difference)}h ago"
            else -> DateUtils.getRelativeTimeSpanString(
                timestamp,
                now,
                DateUtils.DAY_IN_MILLIS
            ).toString()
        }
    }
}
