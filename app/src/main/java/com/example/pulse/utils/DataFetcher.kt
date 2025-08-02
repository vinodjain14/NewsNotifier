package com.example.pulse.utils

import android.content.Context
import android.util.Log
import com.example.pulse.data.NotificationItem
import com.example.pulse.data.XTweet
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Object to handle fetching data from RSS feeds and X (Twitter) API.
 * This class also manages the last fetched timestamps for each source.
 */
object DataFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // In a real application, you would use a secure way to store and retrieve API keys,
    // e.g., BuildConfig fields, encrypted secrets, or a backend service.
    // For this demonstration, it's hardcoded.
    private const val X_BEARER_TOKEN = "AAAAAAAAAAAAAAAAAAAAAMkt3QEAAAAATkeB5GRoOD5ghkmnW3cEpd4n1O4%3DpEusbIgM21b7EBT7MOTOTitFtcHBbQL6piDAz7in0wsvUK2QOD" // Replace with your actual X API Bearer Token

    private const val LAST_FETCH_PREFS = "last_fetch_prefs"
    private const val LAST_FETCH_TIMESTAMP_KEY_PREFIX = "last_fetch_"
    private const val LAST_TWEET_ID_KEY_PREFIX = "last_tweet_id_"

    private const val TAG = "DataFetcher"

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
            Log.d(TAG, "DataFetcher initialized")
        }
    }

    /**
     * Fetches RSS feed and returns new articles since the last fetch.
     * @param feedUrl The URL of the RSS feed.
     * @return A list of new articles (NotificationItem).
     */
    fun fetchRssFeed(feedUrl: String): List<NotificationItem> {
        Log.d(TAG, "Fetching RSS feed: $feedUrl")

        return try {
            val request = Request.Builder()
                .url(feedUrl)
                .addHeader("User-Agent", "NOTT/1.0 (News Aggregator)")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch RSS feed: $feedUrl, Code: ${response.code}")
                return emptyList()
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Empty response body for RSS feed: $feedUrl")
                return emptyList()
            }

            Log.d(TAG, "RSS response received, length: ${responseBody.length}")
            val articles = parseRssFeed(responseBody, feedUrl)
            Log.d(TAG, "Parsed ${articles.size} articles from RSS feed")

            val lastFetchTimestamp = getLastFetchTimestamp(feedUrl)
            val newArticles = articles.filter { it.timestamp > lastFetchTimestamp }
            Log.d(TAG, "Found ${newArticles.size} new articles since last fetch")

            if (newArticles.isNotEmpty()) {
                // Update last fetch timestamp to the latest article's timestamp or current time
                val latestTimestamp = articles.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                updateLastFetchTimestamp(feedUrl, latestTimestamp)
                Log.d(TAG, "Updated last fetch timestamp for $feedUrl")
            }

            newArticles
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RSS feed $feedUrl: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches tweets for a given username and returns new tweets since the last fetch.
     * @param username The X (Twitter) username.
     * @return A list of new tweets (XTweet).
     */
    fun fetchTweets(username: String): List<XTweet> {
        Log.d(TAG, "Fetching tweets for: @$username")

        if (X_BEARER_TOKEN == "YOUR_X_BEARER_TOKEN") {
            Log.w(TAG, "X_BEARER_TOKEN is not set. Cannot fetch tweets for @$username")
            return emptyList()
        }

        return try {
            // 1. Get User ID from username
            val userId = getUserIdFromUsername(username)
            if (userId == null) {
                Log.e(TAG, "Could not get user ID for @$username")
                return emptyList()
            }

            // 2. Fetch user's tweets
            val tweets = fetchUserTweetsTimeline(userId)
            Log.d(TAG, "Fetched ${tweets.size} tweets for @$username")

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

            Log.d(TAG, "Found ${newTweets.size} new tweets for @$username")
            newTweets
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tweets for @$username: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Helper to get X User ID from username.
     */
    private fun getUserIdFromUsername(username: String): String? {
        return try {
            val request = Request.Builder()
                .url("https://api.twitter.com/2/users/by/username/$username")
                .header("Authorization", "Bearer $X_BEARER_TOKEN")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch user ID for @$username. Code: ${response.code}")
                return null
            }

            val responseBody = response.body?.string() ?: return null
            val userResponse = gson.fromJson(responseBody, com.example.pulse.data.XUserResponse::class.java)
            userResponse.data?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID for @$username: ${e.message}", e)
            null
        }
    }

    /**
     * Helper to fetch a user's tweets timeline.
     * Includes 'tweet.fields=created_at' to get timestamp.
     */
    private fun fetchUserTweetsTimeline(userId: String): List<XTweet> {
        return try {
            val request = Request.Builder()
                .url("https://api.twitter.com/2/users/$userId/tweets?tweet.fields=created_at&max_results=10")
                .header("Authorization", "Bearer $X_BEARER_TOKEN")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch tweets for user ID $userId. Code: ${response.code}")
                return emptyList()
            }

            val responseBody = response.body?.string() ?: return emptyList()
            val tweetsResponse = gson.fromJson(responseBody, com.example.pulse.data.XTweetsResponse::class.java)
            tweetsResponse.data ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tweets timeline for user $userId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Parses an RSS feed XML string into a list of NotificationItem.
     */
    private fun parseRssFeed(rssXml: String, feedUrl: String): List<NotificationItem> {
        Log.d(TAG, "Parsing RSS feed XML")

        val articles = mutableListOf<NotificationItem>()
        var title: String? = null
        var description: String? = null
        var pubDate: String? = null
        var link: String? = null
        var inItem = false
        var sourceName: String = extractSourceNameFromUrl(feedUrl)
        var isBreaking: Boolean = false
        var isNew: Boolean = true

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(rssXml))

            var eventType = parser.eventType
            var text: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when {
                            tagName.equals("channel", ignoreCase = true) -> {
                                // Extract source name from channel title if possible
                                sourceName = extractChannelTitle(parser) ?: sourceName
                            }
                            tagName.equals("item", ignoreCase = true) -> {
                                inItem = true
                                title = null
                                description = null
                                pubDate = null
                                link = null
                                isBreaking = false
                                isNew = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        text = parser.text
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            tagName.equals("item", ignoreCase = true) -> {
                                inItem = false
                                if (title != null && !title.isBlank()) {
                                    val timestamp = if (pubDate != null) parseRssPubDate(pubDate!!) else System.currentTimeMillis()

                                    // Check for breaking news keywords
                                    val titleLower = title!!.lowercase()
                                    isBreaking = titleLower.contains("breaking") ||
                                            titleLower.contains("urgent") ||
                                            titleLower.contains("alert")

                                    articles.add(
                                        NotificationItem(
                                            id = UUID.randomUUID().toString(),
                                            title = title!!,
                                            message = description?.take(200) ?: "",
                                            sourceName = sourceName,
                                            timestamp = timestamp,
                                            isBreaking = isBreaking,
                                            isNew = isNew,
                                            tag = if (isBreaking) "BREAKING" else if (isNew) "NEW" else null
                                        )
                                    )
                                }
                            }
                            inItem && tagName.equals("title", ignoreCase = true) -> {
                                title = text?.trim()
                            }
                            inItem && tagName.equals("description", ignoreCase = true) -> {
                                description = text?.let { cleanHtmlTags(it) }?.trim()
                            }
                            inItem && tagName.equals("pubDate", ignoreCase = true) -> {
                                pubDate = text?.trim()
                            }
                            inItem && tagName.equals("link", ignoreCase = true) -> {
                                link = text?.trim()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS feed: ${e.message}", e)
        }

        Log.d(TAG, "Successfully parsed ${articles.size} articles")
        return articles
    }

    /**
     * Extracts channel title from RSS XML
     */
    private fun extractChannelTitle(parser: XmlPullParser): String? {
        return try {
            var depth = 0
            var eventType = parser.next()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        depth++
                        if (parser.name.equals("title", ignoreCase = true) && depth == 1) {
                            parser.next() // Move to text
                            return parser.text?.trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        depth--
                        if (depth < 0) break
                    }
                }
                eventType = parser.next()
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting channel title: ${e.message}", e)
            null
        }
    }

    /**
     * Extracts source name from feed URL
     */
    private fun extractSourceNameFromUrl(url: String): String {
        return try {
            val domain = url.substringAfter("://").substringBefore("/")
            when {
                domain.contains("reuters") -> "Reuters"
                domain.contains("bbc") -> "BBC News"
                domain.contains("cnn") -> "CNN"
                domain.contains("nytimes") -> "New York Times"
                domain.contains("guardian") -> "The Guardian"
                domain.contains("aljazeera") -> "Al Jazeera"
                domain.contains("ndtv") -> "NDTV"
                domain.contains("thehindu") -> "The Hindu"
                domain.contains("economictimes") -> "Economic Times"
                domain.contains("livemint") -> "LiveMint"
                domain.contains("moneycontrol") -> "MoneyControl"
                else -> domain.substringBefore(".").replaceFirstChar { it.uppercase() }
            }
        } catch (e: Exception) {
            "RSS Feed"
        }
    }

    /**
     * Cleans HTML tags from text
     */
    private fun cleanHtmlTags(text: String): String {
        return text.replace(Regex("<.*?>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
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
                Log.w(TAG, "Could not parse date: $pubDate. Using current time.")
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