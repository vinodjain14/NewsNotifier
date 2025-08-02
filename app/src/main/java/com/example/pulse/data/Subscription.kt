package com.example.pulse.data

/**
 * Represents a user's subscription to a news source or X (Twitter) personality.
 * NOTE: Default values have been added to each property to ensure compatibility
 * with Firestore's automatic data mapping, which requires a no-argument constructor.
 */
data class Subscription(
    val id: String = "",
    val name: String = "",
    val type: SubscriptionType = SubscriptionType.RSS_FEED,
    val sourceUrl: String = ""
)

enum class SubscriptionType {
    RSS_FEED,
    TWITTER
}
