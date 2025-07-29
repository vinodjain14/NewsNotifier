package com.example.newsnotifier.data

/**
 * Defines default and predefined news sources for the app.
 */
object AppDefaults {

    data class PredefinedSource(
        val name: String,
        val type: SubscriptionType,
        val sourceUrl: String,
        val description: String, // Added for UI
        val tag: String? = null // Added for UI (e.g., "TRENDING", "RELIABLE")
    )

    val newsChannels = listOf(
        PredefinedSource("Reuters", SubscriptionType.RSS_FEED, "http://feeds.reuters.com/reuters/topNews", "International news and wire reports", "RELIABLE"),
        PredefinedSource("BBC News", SubscriptionType.RSS_FEED, "http://feeds.bbci.co.uk/news/rss.xml", "Global news and current affairs"),
        PredefinedSource("CNN News", SubscriptionType.RSS_FEED, "http://rss.cnn.com/rss/cnn_topstories.rss", "Breaking news and in-depth analysis"),
        PredefinedSource("The New York Times", SubscriptionType.RSS_FEED, "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml", "Award-winning journalism"),
        PredefinedSource("The Guardian", SubscriptionType.RSS_FEED, "https://www.theguardian.com/world/rss", "Independent UK news"),
        PredefinedSource("Al Jazeera", SubscriptionType.RSS_FEED, "https://www.aljazeera.com/xml/rss/all.xml", "Middle East and international news"),
        PredefinedSource("NDTV", SubscriptionType.RSS_FEED, "https://feeds.feedburner.com/ndtv/latest", "Indian news and entertainment"),
        PredefinedSource("The Hindu", SubscriptionType.RSS_FEED, "https://www.thehindu.com/feeder/default.rss", "Comprehensive Indian news coverage"),
        PredefinedSource("The Economic Times", SubscriptionType.RSS_FEED, "https://economictimes.indiatimes.com/rssfeedsdefault.cms", "Indian business and economy news"),
        PredefinedSource("Livemint", SubscriptionType.RSS_FEED, "https://www.livemint.com/rss/homepage", "Financial news and analysis"),
        PredefinedSource("Moneycontrol", SubscriptionType.RSS_FEED, "https://www.moneycontrol.com/rss/latestnews.xml", "Indian stock market and finance news"),
        PredefinedSource("WSJ", SubscriptionType.RSS_FEED, "https://feeds.a.dj.com/rss/RssPublic.xml", "Business and financial news from around the world"),
        PredefinedSource("MarketWatch", SubscriptionType.RSS_FEED, "https://www.marketwatch.com/public/rss/mw_topstories.xml", "Stock market, financial, and business news"),
        PredefinedSource("Financial Times", SubscriptionType.RSS_FEED, "https://www.ft.com/rss/home", "Global business and finance news")
    )

    val xPersonalities = listOf(
        PredefinedSource("Donald Trump", SubscriptionType.TWITTER, "realDonaldTrump", "Political updates and statements", "TRENDING"),
        PredefinedSource("Elon Musk", SubscriptionType.TWITTER, "elonmusk", "Tech innovations and business news"),
        PredefinedSource("Fox News", SubscriptionType.TWITTER, "FoxNews", "Breaking news and political coverage"),
        PredefinedSource("MAGA Voice", SubscriptionType.TWITTER, "MAGAVoice", "Conservative political commentary"),
        PredefinedSource("WSJ", SubscriptionType.TWITTER, "wsj", "Official Wall Street Journal tweets"),
        PredefinedSource("CNN News", SubscriptionType.TWITTER, "CNN", "Official CNN breaking news and updates"),
        PredefinedSource("MarketWatch", SubscriptionType.TWITTER, "MarketWatch", "Real-time market news and analysis"),
        PredefinedSource("Financial Times", SubscriptionType.TWITTER, "FT", "Official Financial Times tweets"),
        PredefinedSource("Yahoo Finance", SubscriptionType.TWITTER, "YahooFinance", "Financial news, data & quotes"),
        PredefinedSource("Jim Cramer", SubscriptionType.TWITTER, "jimcramer", "Mad Money host's market insights"),
        PredefinedSource("Modi", SubscriptionType.TWITTER, "narendramodi", "Official account of the Prime Minister of India")
    )

    /**
     * Generates initials from a given name for display in circular/square icons.
     */
    fun getInitials(name: String): String {
        val words = name.split(" ", "-").filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
            words.size == 1 -> words[0].first().toString().uppercase()
            else -> ""
        }
    }
}
