package com.example.pulse.data

import androidx.compose.ui.graphics.Color
import com.example.pulse.ui.theme.Primary
import com.example.pulse.ui.theme.Purple40
import com.example.pulse.ui.theme.Purple80
import com.example.pulse.ui.theme.PurpleGrey40

object AppDefaults {
    val xPersonalities = listOf(
        PredefinedSource("Elon Musk", "@elonmusk", SubscriptionType.TWITTER, "Tech"),
        PredefinedSource("Naval Ravikant", "@naval", SubscriptionType.TWITTER, "Philosophy"),
        PredefinedSource("Vitalik Buterin", "@VitalikButerin", SubscriptionType.TWITTER, "Crypto"),
        PredefinedSource("Lex Fridman", "@lexfridman", SubscriptionType.TWITTER, "AI & Science")
    )

    val newsChannels = listOf(
        PredefinedSource("CNN News", "http://rss.cnn.com/rss/cnn_topstories.rss", SubscriptionType.RSS_FEED, "World"),
        PredefinedSource("BBC News", "http://feeds.bbci.co.uk/news/rss.xml", SubscriptionType.RSS_FEED, "World"),
        PredefinedSource("Reuters", "https://www.reuters.com/tools/rss", SubscriptionType.RSS_FEED, "Business"),
        PredefinedSource("The Wall Street Journal", "https://feeds.a.dj.com/rss/RSSWorldNews.xml", SubscriptionType.RSS_FEED, "Business"),
        PredefinedSource("TechCrunch", "https://techcrunch.com/feed/", SubscriptionType.RSS_FEED, "Tech"),
        PredefinedSource("The Verge", "https://www.theverge.com/rss/index.xml", SubscriptionType.RSS_FEED, "Tech")
    )

    data class PredefinedSource(
        val name: String,
        val sourceUrl: String,
        val type: SubscriptionType,
        val tag: String? = null
    )

    /**
     * Generates initials from a given name.
     * *** FIX: This function is now safer and handles null or empty names. ***
     * @param name The full name or source name.
     * @return A string containing the first one or two letters, or a default "?" if the name is invalid.
     */
    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) {
            return "?"
        }
        val words = name.split(" ").filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}"
            words.isNotEmpty() -> "${words[0].first()}"
            else -> "?"
        }.uppercase()
    }

    fun getColorFor(name: String): Color {
        val hash = name.hashCode()
        return when (hash % 4) {
            0 -> Primary
            1 -> Purple40
            2 -> Purple80
            else -> PurpleGrey40
        }
    }
}
