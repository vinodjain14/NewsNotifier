package com.example.newsnotifier.utils

import android.content.Context
import com.example.newsnotifier.data.NotificationItem
import com.example.newsnotifier.data.XTweet
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.gson.reflect.TypeToken // Added import for TypeToken
import java.util.UUID // Added import for UUID

/**
 * Object to handle fetching data from RSS feeds and X (Twitter) API.
 * This class also manages the last fetched timestamps for each source.
 */
object DataFetcher {

    private val client = OkHttpClient()
    private val gson = Gson()

    // In a real application, you would use a secure way to store and retrieve API keys,
    // e.g., BuildConfig fields, encrypted secrets, or a backend service.
    // For this demonstration, it's hardcoded.
    private const val X_BEARER_TOKEN = "YOUR_X_BEARER_TOKEN" // Replace with your actual X API Bearer Token

    private const val LAST_FETCH_PREFS = "last_fetch_prefs"
    private const val LAST_FETCH_TIMESTAMP_KEY_PREFIX = "last_fetch_" // Prefix for individual source timestamps
    private const val LAST_TWEET_ID_KEY_PREFIX = "last_tweet_id_" // Prefix for last tweet ID for Twitter sources

    // MutableStateFlow to hold the current list of notifications and emit updates
    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationItem>> = _notificationsFlow.asStateFlow()

    private lateinit var applicationContext: Context

    /**
     * Initializes the DataFetcher with application context.
     * This should be called once early in the application lifecycle (e.g., in MainActivity's onCreate).
     */
    fun init(context: Context) {
        if (!::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
            // Load initial notifications into the flow if needed, or manage separately
            // For now, notifications are managed by NotificationHelper, but if DataFetcher
            // were to directly expose a notifications list, it's would be initialized here.
        }
    }

    /**
     * Fetches RSS feed and returns new articles since the last fetch.
     * @param feedUrl The URL of the RSS feed.
     * @return A list of new articles (NotificationItem).
     */
    fun fetchRssFeed(feedUrl: String): List<NotificationItem> {
        val request = Request.Builder().url(feedUrl).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            println("Failed to fetch RSS feed: $feedUrl, Code: ${response.code}")
            return emptyList()
        }

        val responseBody = response.body?.string() ?: return emptyList()
        val articles = parseRssFeed(responseBody)

        val lastFetchTimestamp = getLastFetchTimestamp(feedUrl)
        val newArticles = articles.filter { it.timestamp > lastFetchTimestamp }

        if (newArticles.isNotEmpty()) {
            // Update last fetch timestamp to the latest article's timestamp or current time
            val latestTimestamp = articles.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
            updateLastFetchTimestamp(feedUrl, latestTimestamp)
        }

        return newArticles
    }

    /**
     * Fetches tweets for a given username and returns new tweets since the last fetch.
     * @param username The X (Twitter) username.
     * @return A list of new tweets (XTweet).
     */
    fun fetchTweets(username: String): List<XTweet> {
        if (X_BEARER_TOKEN == "YOUR_X_BEARER_TOKEN") {
            println("X_BEARER_TOKEN is not set. Cannot fetch tweets.")
            return emptyList()
        }

        // 1. Get User ID from username
        val userId = getUserIdFromUsername(username) ?: run {
            println("Could not get user ID for @$username")
            return emptyList()
        }

        // 2. Fetch user's tweets
        val tweets = fetchUserTweetsTimeline(userId)

        val lastTweetId = getLastTweetId(username)
        val newTweets = if (lastTweetId == null) {
            // If no last tweet ID, consider all fetched tweets as new (or just the latest few)
            tweets.take(5) // Take a few recent ones on first fetch
        } else {
            // Filter tweets that are newer than the last stored tweet ID
            tweets.filter { it.id > lastTweetId }
        }

        if (newTweets.isNotEmpty()) {
            // Update last tweet ID to the newest tweet's ID
            val newestTweetId = tweets.maxOfOrNull { it.id }
            if (newestTweetId != null) {
                updateLastTweetId(username, newestTweetId)
            }
        }
        return newTweets
    }

    /**
     * Helper to get X User ID from username.
     */
    private fun getUserIdFromUsername(username: String): String? {
        val request = Request.Builder()
            .url("https://api.twitter.com/2/users/by/username/$username")
            .header("Authorization", "Bearer $X_BEARER_TOKEN")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            println("Failed to fetch user ID for @$username. Code: ${response.code}, Message: ${response.message}")
            println("Response Body: ${response.body?.string()}")
            return null
        }

        val responseBody = response.body?.string() ?: return null
        val userResponse = gson.fromJson(responseBody, com.example.newsnotifier.data.XUserResponse::class.java)
        return userResponse.data?.id
    }

    /**
     * Helper to fetch a user's tweets timeline.
     * Includes 'tweet.fields=created_at' to get timestamp.
     */
    private fun fetchUserTweetsTimeline(userId: String): List<XTweet> {
        val request = Request.Builder()
            .url("https://api.twitter.com/2/users/$userId/tweets?tweet.fields=created_at&max_results=10") // Fetch up to 10 recent tweets
            .header("Authorization", "Bearer $X_BEARER_TOKEN")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            println("Failed to fetch tweets for user ID $userId. Code: ${response.code}, Message: ${response.message}")
            println("Response Body: ${response.body?.string()}")
            return emptyList()
        }

        val responseBody = response.body?.string() ?: return emptyList()
        val tweetsResponse = gson.fromJson(responseBody, com.example.newsnotifier.data.XTweetsResponse::class.java)
        return tweetsResponse.data ?: emptyList()
    }

    /**
     * Parses an RSS feed XML string into a list of NotificationItem.
     */
    private fun parseRssFeed(rssXml: String): List<NotificationItem> {
        val articles = mutableListOf<NotificationItem>()
        var text: String? = null
        var title: String? = null
        var pubDate: String? = null
        var inItem = false
        var sourceName: String = "Unknown Source" // Default source name
        var isBreaking: Boolean = false // Default breaking status
        var isNew: Boolean = false // Default new status
        var tag: String? = null

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(rssXml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("channel", ignoreCase = true)) {
                            // Try to get source name from channel title
                            var channelTitle: String? = null
                            var nextEventType = parser.next()
                            while (nextEventType != XmlPullParser.END_TAG || !parser.name.equals("channel", ignoreCase = true)) {
                                if (nextEventType == XmlPullParser.START_TAG && parser.name.equals("title", ignoreCase = true)) {
                                    parser.next() // Move to text
                                    channelTitle = parser.text
                                }
                                nextEventType = parser.next()
                            }
                            channelTitle?.let { sourceName = it }
                        } else if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                            title = null
                            pubDate = null
                            text = null
                            isBreaking = false // Reset for each item
                            isNew = true // Assume new if just fetched
                            tag = null
                        }
                    }
                    XmlPullParser.TEXT -> {
                        text = parser.text
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            tagName.equals("item", ignoreCase = true) -> {
                                inItem = false
                                if (title != null && pubDate != null) {
                                    val timestamp = parseRssPubDate(pubDate)
                                    articles.add(
                                        NotificationItem(
                                            id = UUID.randomUUID().toString(),
                                            title = title!!,
                                            message = text ?: "",
                                            sourceName = sourceName,
                                            timestamp = timestamp,
                                            isBreaking = isBreaking,
                                            isNew = isNew,
                                            tag = tag
                                        )
                                    )
                                }
                            }
                            inItem && tagName.equals("title", ignoreCase = true) -> {
                                title = text
                            }
                            inItem && tagName.equals("description", ignoreCase = true) -> {
                                // Corrected: Directly assign to local 'text' variable
                                if (text != null && text!!.isNotBlank()) {
                                    if (text!!.contains("<") && text!!.contains(">")) {
                                        text = text!!.replace(Regex("<.*?>"), "").trim()
                                    } else {
                                        text = text
                                    }
                                }
                            }
                            inItem && tagName.equals("pubDate", ignoreCase = true) -> {
                                pubDate = text
                            }
                            // Example: Custom tags in RSS for breaking news (not standard, but for demonstration)
                            inItem && tagName.equals("breaking", ignoreCase = true) && text.toBoolean() -> {
                                isBreaking = true
                                tag = "BREAKING"
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error parsing RSS feed: ${e.message}")
        }
        return articles
    }

    /**
     * Parses various RSS date formats into a Unix timestamp (milliseconds).
     * Handles RFC 822 (e.g., "Thu, 01 Jan 2023 12:00:00 GMT") and ISO 8601.
     */
    private fun parseRssPubDate(pubDate: String): Long {
        return try {
            // Try parsing as RFC 1123 / RFC 822
            val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
            val zonedDateTime = java.time.ZonedDateTime.parse(pubDate, formatter)
            zonedDateTime.toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                // Try parsing as ISO 8601 (e.g., from some feeds or X API)
                val instant = Instant.parse(pubDate)
                instant.toEpochMilli()
            } catch (e2: Exception) {
                println("Could not parse date: $pubDate. Error: ${e2.message}. Using current time.")
                System.currentTimeMillis() // Fallback to current time
            }
        }
    }

    /**
     * Retrieves the last fetch timestamp for a given source URL from SharedPreferences.
     */
    private fun getLastFetchTimestamp(sourceUrl: String): Long {
        val prefs = applicationContext.getSharedPreferences(LAST_FETCH_PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(LAST_FETCH_TIMESTAMP_KEY_PREFIX + sourceUrl, 0L)
    }

    /**
     * Updates the last fetch timestamp for a given source URL in SharedPreferences.
     */
    private fun updateLastFetchTimestamp(sourceUrl: String, timestamp: Long) {
        val prefs = applicationContext.getSharedPreferences(LAST_FETCH_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong(LAST_FETCH_TIMESTAMP_KEY_PREFIX + sourceUrl, timestamp).apply()
    }

    /**
     * Retrieves the last tweet ID for a given username from SharedPreferences.
     */
    private fun getLastTweetId(username: String): String? {
        val prefs = applicationContext.getSharedPreferences(LAST_FETCH_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(LAST_TWEET_ID_KEY_PREFIX + username, null)
    }

    /**
     * Updates the last tweet ID for a given username in SharedPreferences.
     */
    private fun updateLastTweetId(username: String, tweetId: String) {
        val prefs = applicationContext.getSharedPreferences(LAST_FETCH_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(LAST_TWEET_ID_KEY_PREFIX + username, tweetId).apply()
    }
}
