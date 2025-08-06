package com.example.pulse.utils

import android.content.Context
import android.util.Log
import com.example.pulse.data.NotificationItem
import com.example.pulse.data.Subscription
import com.example.pulse.data.SubscriptionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DataFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val LAST_FETCH_PREFS = "last_fetch_prefs"
    private const val LAST_FETCH_TIMESTAMP_KEY_PREFIX = "last_fetch_"
    private const val TAG = "DataFetcher"

    // 24 hours in milliseconds
    private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L

    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationItem>> = _notificationsFlow.asStateFlow()

    private lateinit var applicationContext: Context
    private lateinit var subscriptionManager: SubscriptionManager

    fun init(context: Context, subManager: SubscriptionManager) {
        if (!::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
            subscriptionManager = subManager
            Log.d(TAG, "DataFetcher initialized")
        }
    }

    fun fetchAllFeeds(): List<NotificationItem> {
        val subscriptions: List<Subscription> = subscriptionManager.getSubscriptions()
        if (subscriptions.isEmpty()) {
            Log.d(TAG, "No subscriptions found. Skipping fetch.")
            return emptyList()
        }

        Log.d(TAG, "Fetching feeds for ${subscriptions.size} subscriptions.")
        val allNotifications = mutableListOf<NotificationItem>()
        val globalSeenTitles = mutableSetOf<String>() // Global deduplication across all feeds
        val globalSeenLinks = mutableSetOf<String>()

        subscriptions.forEach { subscription ->
            if (subscription.type == SubscriptionType.RSS_FEED) {
                // Pass category along with market and URL
                val feedNotifications = fetchRssFeed(subscription.sourceUrl, subscription.market, subscription.category)

                // Apply global deduplication across all feeds
                feedNotifications.forEach { notification ->
                    val normalizedTitle = normalizeTitle(notification.title)
                    val normalizedLink = notification.sourceUrl?.trim()

                    val isDuplicateTitle = globalSeenTitles.contains(normalizedTitle)
                    val isDuplicateLink = normalizedLink != null && globalSeenLinks.contains(normalizedLink)

                    if (!isDuplicateTitle && !isDuplicateLink) {
                        allNotifications.add(notification)
                        globalSeenTitles.add(normalizedTitle)
                        if (normalizedLink != null) {
                            globalSeenLinks.add(normalizedLink)
                        }
                    } else {
                        val reason = when {
                            isDuplicateTitle && isDuplicateLink -> "duplicate title and link across feeds"
                            isDuplicateTitle -> "duplicate title across feeds"
                            isDuplicateLink -> "duplicate link across feeds"
                            else -> "unknown"
                        }
                        Log.d(TAG, "Skipped global duplicate: ${notification.title} from ${notification.sourceName} ($reason)")
                    }
                }
            }
        }

        Log.d(TAG, "Total unique notifications after global deduplication: ${allNotifications.size}")
        return allNotifications.sortedByDescending { it.timestamp } // Sort by newest first
    }

    private fun fetchRssFeed(feedUrl: String, market: String, category: String): List<NotificationItem> {
        Log.d(TAG, "Fetching RSS feed: $feedUrl for market: $market, category: $category")

        return try {
            val request = Request.Builder()
                .url(feedUrl)
                .addHeader("User-Agent", "Pulse/1.0 (News Aggregator)")
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

            // Pass category to the parser
            parseRssFeed(responseBody, feedUrl, market, category)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RSS feed $feedUrl: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseRssFeed(rssXml: String, feedUrl: String, market: String, category: String): List<NotificationItem> {
        val articles = mutableListOf<NotificationItem>()
        val seenTitles = mutableSetOf<String>() // Track titles to avoid duplicates
        val seenLinks = mutableSetOf<String>() // Track URLs to avoid duplicates
        var title: String? = null
        var description: String? = null
        var pubDate: String? = null
        var link: String? = null // Add link parsing
        var inItem = false
        val sourceName: String = extractSourceNameFromUrl(feedUrl)
        val currentTime = System.currentTimeMillis()

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
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                            title = null
                            description = null
                            pubDate = null
                            link = null // Reset link
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

                                    // Check if the article is within the last 24 hours
                                    val ageInMs = currentTime - timestamp
                                    if (ageInMs <= TWENTY_FOUR_HOURS_MS) {
                                        val normalizedTitle = normalizeTitle(title!!)
                                        val normalizedLink = link?.trim()

                                        // Check for duplicates based on normalized title and link
                                        val isDuplicateTitle = seenTitles.contains(normalizedTitle)
                                        val isDuplicateLink = normalizedLink != null && seenLinks.contains(normalizedLink)

                                        if (!isDuplicateTitle && !isDuplicateLink) {
                                            val titleLower = title!!.lowercase()
                                            val isBreaking = titleLower.contains("breaking") || titleLower.contains("urgent") || titleLower.contains("alert")

                                            // Generate unique ID based on content
                                            val uniqueId = generateUniqueId(title!!, normalizedLink, timestamp)

                                            articles.add(
                                                NotificationItem(
                                                    id = uniqueId,
                                                    title = title!!,
                                                    message = description?.take(200) ?: "",
                                                    sourceName = sourceName,
                                                    market = market,
                                                    category = category,
                                                    timestamp = timestamp,
                                                    isBreaking = isBreaking,
                                                    isNew = true,
                                                    tag = if (isBreaking) "BREAKING" else "NEW",
                                                    sourceUrl = normalizedLink // Add source URL
                                                )
                                            )

                                            // Add to seen sets
                                            seenTitles.add(normalizedTitle)
                                            if (normalizedLink != null) {
                                                seenLinks.add(normalizedLink)
                                            }

                                            Log.d(TAG, "Added unique notification: $title (${formatAgeForLog(ageInMs)} old)")
                                        } else {
                                            val reason = when {
                                                isDuplicateTitle && isDuplicateLink -> "duplicate title and link"
                                                isDuplicateTitle -> "duplicate title"
                                                isDuplicateLink -> "duplicate link"
                                                else -> "unknown"
                                            }
                                            Log.d(TAG, "Skipped duplicate notification: $title ($reason)")
                                        }
                                    } else {
                                        Log.d(TAG, "Skipped old notification: $title (${formatAgeForLog(ageInMs)} old)")
                                    }
                                }
                            }
                            inItem && tagName.equals("title", ignoreCase = true) -> title = text?.trim()
                            inItem && tagName.equals("description", ignoreCase = true) -> description = text?.let { cleanHtmlTags(it) }?.trim()
                            inItem && tagName.equals("pubDate", ignoreCase = true) -> pubDate = text?.trim()
                            inItem && tagName.equals("link", ignoreCase = true) -> link = text?.trim() // Parse link
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS feed: ${e.message}", e)
        }

        Log.d(TAG, "Parsed ${articles.size} unique notifications from $feedUrl (filtered for last 24 hours)")
        return articles
    }

    private fun extractSourceNameFromUrl(url: String): String {
        return try {
            val domain = url.substringAfter("://").substringBefore("/")
            when {
                domain.contains("reuters") -> "Reuters"
                domain.contains("bloomberg") -> "Bloomberg"
                domain.contains("ft.com") -> "Financial Times"
                domain.contains("economist") -> "The Economist"
                domain.contains("bbci.co.uk") -> "BBC News"
                domain.contains("nikkei.com") -> "Nikkei Asia"
                domain.contains("caixinglobal") -> "Caixin Global"
                domain.contains("scmp.com") -> "SCMP"
                else -> domain.substringBefore(".").replaceFirstChar { it.uppercase() }
            }
        } catch (e: Exception) {
            "RSS Feed"
        }
    }

    private fun cleanHtmlTags(text: String): String {
        return text.replace(Regex("<.*?>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
    }

    private fun parseRssPubDate(pubDate: String): Long {
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        return try {
            format.parse(pubDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                isoFormat.parse(pubDate)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                Log.w(TAG, "Could not parse date: $pubDate. Using current time.")
                System.currentTimeMillis()
            }
        }
    }

    private fun formatAgeForLog(ageInMs: Long): String {
        val hours = ageInMs / (60 * 60 * 1000)
        val minutes = (ageInMs % (60 * 60 * 1000)) / (60 * 1000)
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    /**
     * Normalizes a title for duplicate detection by:
     * - Converting to lowercase
     * - Removing extra whitespace
     * - Removing common punctuation that might vary
     * - Removing common prefixes that might be added/removed
     */
    private fun normalizeTitle(title: String): String {
        var normalized = title.trim().lowercase()

        // Multiple spaces to single space
        normalized = normalized.replace(Regex("\\s+"), " ")

        // Normalize various quote types to standard quotes
        normalized = normalized.replace("'", "'")
            .replace("'", "'")
            .replace(""", "\"")
            .replace(""", "\"")
            .replace("`", "'")

        // Normalize various dash types to standard hyphen
        normalized = normalized.replace("–", "-")
            .replace("—", "-")
            .replace("−", "-")

        // Remove breaking news prefixes (case insensitive)
        normalized = normalized.replace(Regex("^breaking:?\\s*"), "")
            .replace(Regex("^urgent:?\\s*"), "")
            .replace(Regex("^alert:?\\s*"), "")

        // Remove source suffixes like "- CNN" or "| Reuters"
        normalized = normalized.replace(Regex("\\s*[|\\-]\\s*[^|\\-]*$"), "")

        return normalized.trim()
    }

    /**
     * Generates a unique ID based on content rather than random UUID
     * This helps maintain consistency across app restarts
     */
    private fun generateUniqueId(title: String, link: String?, timestamp: Long): String {
        val contentForId = "${normalizeTitle(title)}${link ?: ""}$timestamp"
        return contentForId.hashCode().toString()
    }

    private fun getLastFetchTimestamp(sourceUrl: String): Long {
        val prefs = applicationContext.getSharedPreferences(LAST_FETCH_PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(LAST_FETCH_TIMESTAMP_KEY_PREFIX + sourceUrl, 0L)
    }

    private fun updateLastFetchTimestamp(sourceUrl: String, timestamp: Long) {
        val prefs = applicationContext.getSharedPreferences(LAST_FETCH_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong(LAST_FETCH_TIMESTAMP_KEY_PREFIX + sourceUrl, timestamp).apply()
    }
}