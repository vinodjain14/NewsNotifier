package com.example.newsnotifier.utils

import android.content.Context
import com.example.newsnotifier.data.NewsItem
import com.example.newsnotifier.data.NotificationItem
import com.example.newsnotifier.data.Subscription
import com.example.newsnotifier.data.SubscriptionType
import com.example.newsnotifier.data.XUserResponse
import com.example.newsnotifier.data.XTweetsResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlin.random.Random // For simulated delays

/**
 * Utility object to fetch data from subscribed sources (X and RSS) and generate notifications.
 * This logic is extracted to be reusable by both the WorkManager worker and direct UI triggers.
 */
object DataFetcher {

    private lateinit var applicationContext: Context
    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    // WARNING: Storing your Bearer Token directly in client-side code is INSECURE.
    // For a production app, use a secure backend to make API calls to X.
    private val X_BEARER_TOKEN = "AAAAAAAAAAAAAAAAAAAAAMkt3QEAAAAATkeB5GRoOD5ghkmnW3cEpd4n1O4%3DpEusbIgM21b7EBT7MOTOTitFtcHBbQL6piDAz7in0wsvUK2QOD" // REPLACE THIS WITH YOUR ACTUAL BEARER TOKEN

    /**
     * Initializes the DataFetcher with application context. Must be called once early.
     */
    fun init(context: Context) {
        if (!::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
        }
    }

    /**
     * Fetches new content for all active subscriptions and generates notifications for new items.
     * @return true if new content was found and notified, false otherwise.
     */
    suspend fun fetchAndNotifyNewContent(): Boolean {
        if (!::applicationContext.isInitialized) {
            println("DataFetcher not initialized. Call init() first.")
            return false
        }

        val subscriptionManager = SubscriptionManager(applicationContext)
        val subscriptions = subscriptionManager.getSubscriptions()
        var newContentFoundOverall = false

        subscriptions.forEach { subscription ->
            // Simulate network delay (can be removed for real network calls)
            kotlinx.coroutines.delay(Random.nextLong(500, 1500))

            try {
                val newItems = fetchContent(subscription)

                // Filter out content that has already been notified
                val unseenItems = newItems.filter { it.id != subscription.lastFetchedContentId }

                if (unseenItems.isNotEmpty()) {
                    newContentFoundOverall = true
                    // Sort by timestamp to get the latest
                    val latestItem = unseenItems.maxByOrNull { it.timestamp }

                    latestItem?.let {
                        val message = "${it.sourceName}: ${it.title}"
                        NotificationHelper.showNotification(
                            applicationContext,
                            "New Update!",
                            message,
                            Random.nextInt() // Use a random ID for each notification
                        )
                        // Update the last fetched content ID for this subscription
                        subscription.lastFetchedContentId = it.id
                        subscriptionManager.updateSubscription(subscription)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error fetching content for ${subscription.name}: ${e.message}")
            }
        }
        return newContentFoundOverall
    }

    /**
     * Fetches content based on subscription type.
     *
     * @param subscription The subscription to fetch content for.
     * @return A list of NewsItems.
     */
    private fun fetchContent(subscription: Subscription): List<NewsItem> {
        return when (subscription.type) {
            SubscriptionType.TWITTER -> {
                fetchXTweetsForUser(subscription.sourceUrl, subscription.name)
            }
            SubscriptionType.RSS_FEED -> {
                fetchRssFeed(subscription.sourceUrl, subscription.name)
            }
        }
    }

    /**
     * Fetches the numeric user ID for a given X username.
     * @param username The X username (e.g., "elonmusk").
     * @return The numeric user ID as a String, or null if not found/error.
     */
    private fun fetchXUserId(username: String): String? {
        val url = "https://api.twitter.com/2/users/by/username/$username"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $X_BEARER_TOKEN")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val type = object : TypeToken<XUserResponse>() {}.type
                val userResponse = gson.fromJson<XUserResponse>(responseBody, type)
                userResponse.data?.id
            } else {
                println("Failed to fetch X user ID for $username: ${response.code} ${response.message} - ${response.body?.string()}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error fetching X user ID for $username: ${e.message}")
            null
        }
    }

    /**
     * Fetches recent tweets for a given X user ID.
     * @param username The X username (for constructing tweet URL and source name).
     * @param sourceName The display name for the news item.
     * @return A list of NewsItems parsed from the X timeline.
     */
    private fun fetchXTweetsForUser(username: String, sourceName: String): List<NewsItem> {
        val newsItems = mutableListOf<NewsItem>()
        val userId = fetchXUserId(username)

        if (userId == null) {
            println("Could not resolve X user ID for username: $username")
            return emptyList()
        }

        val url = "https://api.twitter.com/2/users/$userId/tweets?max_results=5&tweet.fields=created_at"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $X_BEARER_TOKEN")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val type = object : TypeToken<XTweetsResponse>() {}.type
                val tweetsResponse = gson.fromJson<XTweetsResponse>(responseBody, type)

                tweetsResponse.data?.forEach { tweet ->
                    val timestamp = try {
                        iso8601Format.parse(tweet.createdAt)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                    newsItems.add(
                        NewsItem(
                            id = tweet.id,
                            title = tweet.text,
                            url = "https://twitter.com/$username/status/${tweet.id}",
                            sourceName = sourceName,
                            timestamp = timestamp
                        )
                    )
                }
                newsItems
            } else {
                println("Failed to fetch X tweets for user $username (ID: $userId): ${response.code} ${response.message} - ${response.body?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error fetching X tweets for user $username (ID: $userId): ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches and parses an RSS feed from the given URL.
     * @param rssUrl The URL of the RSS feed.
     * @param sourceName The name of the source for the NewsItem.
     * @return A list of NewsItems parsed from the RSS feed.
     */
    private fun fetchRssFeed(rssUrl: String, sourceName: String): List<NewsItem> {
        val newsItems = mutableListOf<NewsItem>()
        var inputStream: InputStream? = null
        try {
            val request = Request.Builder().url(rssUrl).build()
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                inputStream = response.body?.byteStream()
                inputStream?.let {
                    val parserFactory = XmlPullParserFactory.newInstance()
                    parserFactory.isNamespaceAware = false
                    val parser = parserFactory.newPullParser()
                    parser.setInput(it, null)

                    var eventType = parser.eventType
                    var currentItem: NewsItem? = null
                    var text: String? = null

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val tagName = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (tagName.equals("item", ignoreCase = true)) {
                                    currentItem = NewsItem("", "", "", sourceName, 0L)
                                }
                            }
                            XmlPullParser.TEXT -> {
                                text = parser.text
                            }
                            XmlPullParser.END_TAG -> {
                                when {
                                    tagName.equals("item", ignoreCase = true) -> {
                                        currentItem?.let { item ->
                                            if (item.id.isNotBlank() && item.title.isNotBlank() && item.url.isNotBlank()) {
                                                newsItems.add(item)
                                            }
                                        }
                                        currentItem = null
                                    }
                                    tagName.equals("title", ignoreCase = true) -> {
                                        currentItem?.title = text ?: ""
                                    }
                                    tagName.equals("link", ignoreCase = true) -> {
                                        currentItem?.url = text ?: ""
                                    }
                                    tagName.equals("guid", ignoreCase = true) -> {
                                        val guidFromXml = text
                                        val linkFromItem = currentItem?.url

                                        val idToAssign: String = when {
                                            !guidFromXml.isNullOrBlank() -> guidFromXml!! // Assert non-null after check
                                            !linkFromItem.isNullOrBlank() -> linkFromItem!! // Assert non-null after check
                                            else -> UUID.randomUUID().toString()
                                        }
                                        currentItem?.id = idToAssign
                                    }
                                    tagName.equals("pubDate", ignoreCase = true) -> {
                                        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                                        try {
                                            currentItem?.timestamp = dateFormat.parse(text)?.time ?: System.currentTimeMillis()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            currentItem?.timestamp = System.currentTimeMillis()
                                        }
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            } else {
                println("Failed to fetch RSS feed: ${response.code} ${response.message}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error fetching or parsing RSS feed: ${e.message}")
        } finally {
            inputStream?.close()
        }
        return newsItems
    }
}
