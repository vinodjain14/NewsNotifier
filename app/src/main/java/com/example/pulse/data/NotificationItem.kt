package com.example.pulse.data

/**
 * Data class representing a single notification item to be displayed in the app.
 * @param id A unique ID for the notification.
 * @param title The title of the notification.
 * @param message The main content/message of the notification.
 * @param sourceName The name of the source (e.g., "Reuters", "Elon Musk").
 * @param timestamp The time the notification was created (defaults to current time).
 * @param isRead True if the user has read this notification.
 * @param isSaved True if the user has saved this notification.
 * @param isBreaking True if this is a breaking news notification.
 * @param isNew True if this is a new notification (distinct from breaking).
 * @param tag Optional tag for display (e.g., "NEW", "BREAKING").
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val sourceName: String, // Added for UI
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false, // Added for filtering
    val isSaved: Boolean = false, // Added for filtering
    val isBreaking: Boolean = false, // Added for UI tag
    val isNew: Boolean = false, // Added for UI tag
    val tag: String? = null // For displaying "NEW" or "BREAKING" in UI
)
