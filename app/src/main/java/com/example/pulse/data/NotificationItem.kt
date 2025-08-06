package com.example.pulse.data

/**
 * Data class representing a single notification item to be displayed in the app.
 * @param id A unique ID for the notification.
 * @param title The title of the notification.
 * @param message The main content/message of the notification.
 * @param sourceName The name of the source (e.g., "Reuters", "Elon Musk").
 * @param market The market category of the notification (e.g., "USA", "UK/Europe").
 * @param category The specific category of the notification (e.g., "Finance").
 * @param timestamp The time the notification was created (defaults to current time).
 * @param isRead True if the user has read this notification.
 * @param isSaved True if the user has saved this notification.
 * @param isBreaking True if this is a breaking news notification.
 * @param isNew True if this is a new notification (distinct from breaking).
 * @param tag Optional tag for display (e.g., "NEW", "BREAKING").
 * @param sourceUrl The URL to the original article/source (for in-app browser).
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val sourceName: String,
    val market: String,
    val category: String, // Added for categorization
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isSaved: Boolean = false,
    val isBreaking: Boolean = false,
    val isNew: Boolean = false,
    val tag: String? = null,
    val sourceUrl: String? = null // Added for in-app browser
) {
    // Helper method to create a copy with read status changed
    fun markAsRead(): NotificationItem = this.copy(isRead = true)

    // Helper method to create a copy with saved status changed
    fun toggleSaved(): NotificationItem = this.copy(isSaved = !isSaved)
}