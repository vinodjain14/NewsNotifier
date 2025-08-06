package com.example.pulse.data

import java.util.UUID

/**
 * Represents a user's subscription to a news source or X (Twitter) personality.
 * NOTE: Default values have been added to each property to ensure compatibility
 * with Firestore's automatic data mapping, which requires a no-argument constructor.
 */
data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: SubscriptionType,
    val sourceUrl: String,
    val market: String,
    val category: String // Added for categorization
)

enum class SubscriptionType {
    RSS_FEED,
    TWITTER
}
