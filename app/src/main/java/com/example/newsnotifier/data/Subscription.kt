package com.example.newsnotifier.data

/**
 * Represents a single subscription item.
 * @param id A unique identifier for the subscription.
 * @param name The display name of the subscription (e.g., "Elon Musk", "CNN").
 * @param type The type of subscription (e.g., "TWITTER", "RSS_FEED").
 * @param sourceUrl The URL or identifier for the source (e.g., Twitter handle, RSS feed URL).
 * @param lastFetchedContentId A unique ID of the last content item fetched, to avoid re-notifying.
 */
data class Subscription(
    val id: String,
    val name: String,
    val type: SubscriptionType,
    val sourceUrl: String,
    var lastFetchedContentId: String? = null // To track the last seen content
)

enum class SubscriptionType {
    TWITTER,
    RSS_FEED
}