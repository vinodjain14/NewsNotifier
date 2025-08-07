package com.example.pulse.data

/**
 * Represents a single news item or tweet.
 * @param id A unique identifier for the content item (e.g., tweet ID, RSS item GUID).
 * @param title The title or main text of the content.
 * @param url The URL to the original content.
 * @param sourceName The name of the source (e.g., "CNN", "Elon Musk").
 * @param timestamp The time the content was published/posted.
 */
data class NewsItem(
    var id: String, // THIS LINE MUST BE 'var'
    var title: String,
    var url: String,
    val sourceName: String,
    var timestamp: Long,
    val imageUrl: String? = null // Add this line
)

// Extension properties for NewsItem to match usage in SubscriptionWorker
val NewsItem.isBreaking: Boolean
    get() = title.contains("BREAKING", ignoreCase = true) ||
            title.contains("URGENT", ignoreCase = true) ||
            title.contains("ALERT", ignoreCase = true)

val NewsItem.isNew: Boolean
    get() = System.currentTimeMillis() - timestamp < 3600000 // Within last hour

val NewsItem.text: String
    get() = title // For compatibility with tweet-like usage