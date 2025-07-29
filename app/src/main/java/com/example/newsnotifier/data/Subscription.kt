package com.example.newsnotifier.data

/**
 * Represents a user's subscription to a news source or X (Twitter) personality.
 * @param id A unique identifier for the subscription.
 * @param name A user-friendly name for the subscription (e.g., "TechCrunch", "Elon Musk").
 * @param type The type of subscription (e.g., RSS_FEED, TWITTER).
 * @param sourceUrl The URL for RSS feeds or the username for Twitter.
 */
data class Subscription(
    val id: String,
    val name: String,
    val type: SubscriptionType,
    val sourceUrl: String
)

enum class SubscriptionType {
    RSS_FEED,
    TWITTER
}
