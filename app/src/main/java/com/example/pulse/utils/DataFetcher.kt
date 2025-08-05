package com.example.pulse.utils

import android.content.Context
import android.util.Log
import com.example.pulse.data.MarketFeed
import com.example.pulse.data.NotificationItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object DataFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private const val LAST_FETCH_PREFS = "last_fetch_prefs"
    private const val LAST_FETCH_TIMESTAMP_KEY_PREFIX = "last_fetch_"
    private const val TAG = "DataFetcher"

    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationItem>> = _notificationsFlow.asStateFlow()

    private lateinit var applicationContext: Context

    fun init(context: Context) {
        if (!::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
            Log.d(TAG, "DataFetcher initialized")
        }
    }

    private fun fetchRssFeed(feedUrl: String): List<NotificationItem> {
        Log.d(TAG, "Fetching RSS feed: $feedUrl")

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

            val articles = parseRssFeed(responseBody, feedUrl)
            val lastFetchTimestamp = getLastFetchTimestamp(feedUrl)
            val newArticles = articles.filter { it.timestamp > lastFetchTimestamp }

            if (newArticles.isNotEmpty()) {
                val latestTimestamp = articles.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                updateLastFetchTimestamp(feedUrl, latestTimestamp)
            }

            newArticles
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RSS feed $feedUrl: ${e.message}", e)
            emptyList()
        }
    }

    fun fetchAllFeeds(): List<NotificationItem> {
        return try {
            val jsonString = applicationContext.assets.open("Finance_Market_RSS_Feeds.json").bufferedReader().use { it.readText() }
            val marketListType = object : TypeToken<List<MarketFeed>>() {}.type
            val marketList: List<MarketFeed> = gson.fromJson(jsonString, marketListType)
            val allNotifications = mutableListOf<NotificationItem>()

            marketList.forEach { feed ->
                allNotifications.addAll(fetchRssFeed(feed.url))
            }
            allNotifications
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all feeds: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseRssFeed(rssXml: String, feedUrl: String): List<NotificationItem> {
        val articles = mutableListOf<NotificationItem>()
        var title: String? = null
        var description: String? = null
        var pubDate: String? = null
        var inItem = false
        var sourceName: String = extractSourceNameFromUrl(feedUrl)

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
                                    val titleLower = title.lowercase()
                                    val isBreaking = titleLower.contains("breaking") || titleLower.contains("urgent") || titleLower.contains("alert")

                                    articles.add(
                                        NotificationItem(
                                            id = UUID.randomUUID().toString(),
                                            title = title,
                                            message = description?.take(200) ?: "",
                                            sourceName = sourceName,
                                            timestamp = timestamp,
                                            isBreaking = isBreaking,
                                            isNew = true,
                                            tag = if (isBreaking) "BREAKING" else "NEW"
                                        )
                                    )
                                }
                            }
                            inItem && tagName.equals("title", ignoreCase = true) -> title = text?.trim()
                            inItem && tagName.equals("description", ignoreCase = true) -> description = text?.let { cleanHtmlTags(it) }?.trim()
                            inItem && tagName.equals("pubDate", ignoreCase = true) -> pubDate = text?.trim()
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS feed: ${e.message}", e)
        }
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
        // RFC 1123 date format for RSS feeds
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        return try {
            format.parse(pubDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            // Fallback for ISO 8601 format
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                isoFormat.parse(pubDate)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                Log.w(TAG, "Could not parse date: $pubDate. Using current time.")
                System.currentTimeMillis()
            }
        }
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
