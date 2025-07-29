package com.example.newsnotifier.data

/**
 * Data class representing a single notification item to be stored and displayed.
 */
data class NotificationItem(
    val id: String, // Unique ID for the notification (e.g., UUID)
    val title: String, // Title of the notification (e.g., "New Tweet", "News Update")
    val message: String, // Main content of the notification (e.g., tweet text, news headline)
    val timestamp: Long = System.currentTimeMillis() // Timestamp when the notification was created, defaults to current time
)
