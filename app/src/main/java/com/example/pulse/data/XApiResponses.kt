package com.example.pulse.data

import com.google.gson.annotations.SerializedName

// Data class for the user lookup response (e.g., from users/by/username/:username)
data class XUserResponse(
    val data: XUserData?
)

data class XUserData(
    val id: String,
    val name: String,
    val username: String
)

// Data class for the user's tweets timeline response (e.g., from users/:id/tweets)
data class XTweetsResponse(
    val data: List<XTweet>?, // This is nullable, meaning 'data' itself can be null
    val meta: XMeta?
)

data class XTweet(
    val id: String, // These properties are non-nullable within XTweet
    val text: String,
    @SerializedName("created_at")
    val createdAt: String // ISO 8601 format string
)

data class XMeta(
    @SerializedName("newest_id")
    val newestId: String?,
    @SerializedName("oldest_id")
    val oldestId: String?,
    @SerializedName("result_count")
    val resultCount: Int?,
    @SerializedName("next_token")
    val nextToken: String?
)
