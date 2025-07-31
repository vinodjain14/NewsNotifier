package com.example.newsnotifier.utils

import com.example.newsnotifier.data.NotificationItem
import com.example.newsnotifier.data.SubscriptionType

data class NotificationCategory(
    val name: String,
    val icon: String,
    val notifications: List<NotificationItem>
)

object NotificationCategoryHelper {

    fun categorizeNotifications(notifications: List<NotificationItem>): List<NotificationCategory> {
        val categories = mutableListOf<NotificationCategory>()

        // Group by Breaking News
        val breakingNews = notifications.filter { it.isBreaking }
        if (breakingNews.isNotEmpty()) {
            categories.add(
                NotificationCategory(
                    name = "Breaking News",
                    icon = "🚨",
                    notifications = breakingNews.sortedByDescending { it.timestamp }
                )
            )
        }

        // Group by News Sources
        val newsSources = notifications.filter {
            !it.isBreaking && isNewsSource(it.sourceName)
        }
        if (newsSources.isNotEmpty()) {
            categories.add(
                NotificationCategory(
                    name = "News Sources",
                    icon = "📰",
                    notifications = newsSources.sortedByDescending { it.timestamp }
                )
            )
        }

        // Group by Social Media
        val socialMedia = notifications.filter {
            !it.isBreaking && isSocialMedia(it.sourceName)
        }
        if (socialMedia.isNotEmpty()) {
            categories.add(
                NotificationCategory(
                    name = "Social Media",
                    icon = "🐦",
                    notifications = socialMedia.sortedByDescending { it.timestamp }
                )
            )
        }

        // Financial News (special category)
        val financialNews = notifications.filter {
            !it.isBreaking && isFinancialSource(it.sourceName)
        }
        if (financialNews.isNotEmpty()) {
            categories.add(
                NotificationCategory(
                    name = "Financial News",
                    icon = "💹",
                    notifications = financialNews.sortedByDescending { it.timestamp }
                )
            )
        }

        // Saved for Later (if viewing saved items)
        val saved = notifications.filter { it.isSaved }
        if (saved.isNotEmpty()) {
            categories.add(
                NotificationCategory(
                    name = "Saved Articles",
                    icon = "🔖",
                    notifications = saved.sortedByDescending { it.timestamp }
                )
            )
        }

        return categories
    }

    fun isNewsSource(sourceName: String): Boolean {
        val newsKeywords = listOf(
            "Reuters", "BBC", "CNN", "Times", "Guardian",
            "NDTV", "Hindu", "Al Jazeera", "News"
        )
        return newsKeywords.any { sourceName.contains(it, ignoreCase = true) }
    }

    fun isSocialMedia(sourceName: String): Boolean {
        // Check for known X/Twitter personalities or social media indicators
        val socialKeywords = listOf(
            "Trump", "Musk", "Modi", "@"
        )
        return socialKeywords.any { sourceName.contains(it, ignoreCase = true) } ||
                (!isNewsSource(sourceName) && !isFinancialSource(sourceName))
    }

    fun isFinancialSource(sourceName: String): Boolean {
        val financialKeywords = listOf(
            "Economic", "Financial", "WSJ", "MarketWatch",
            "MoneyControl", "LiveMint", "Yahoo Finance",
            "Jim Cramer", "Market", "Finance"
        )
        return financialKeywords.any { sourceName.contains(it, ignoreCase = true) }
    }

    fun getCategoryIcon(sourceName: String): String {
        return when {
            isFinancialSource(sourceName) -> "💹"
            isSocialMedia(sourceName) -> "🐦"
            isNewsSource(sourceName) -> "📰"
            else -> "📢"
        }
    }

    fun getCategoryName(sourceName: String): String {
        return when {
            isFinancialSource(sourceName) -> "Financial"
            isSocialMedia(sourceName) -> "Social"
            isNewsSource(sourceName) -> "News"
            else -> "Other"
        }
    }

    fun NotificationItem.getCategoryInfo(): Pair<String, String> {
        return NotificationCategoryHelper.getCategoryIcon(sourceName) to
                NotificationCategoryHelper.getCategoryName(sourceName)
    }
}
