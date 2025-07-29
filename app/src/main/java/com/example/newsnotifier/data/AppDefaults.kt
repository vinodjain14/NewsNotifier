package com.example.newsnotifier.data

/**
 * Defines default and predefined news sources for the app.
 */
object AppDefaults {

    data class PredefinedSource(
        val name: String,
        val type: SubscriptionType,
        val sourceUrl: String
    )

    val newsChannels = listOf(
        PredefinedSource("Reuters", SubscriptionType.RSS_FEED, "http://feeds.reuters.com/reuters/topNews"),
        PredefinedSource("BBC News", SubscriptionType.RSS_FEED, "http://feeds.bbci.co.uk/news/rss.xml"),
        PredefinedSource("CNN News", SubscriptionType.RSS_FEED, "http://rss.cnn.com/rss/cnn_topstories.rss"), // Updated name
        PredefinedSource("The New York Times", SubscriptionType.RSS_FEED, "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"),
        PredefinedSource("The Guardian", SubscriptionType.RSS_FEED, "https://www.theguardian.com/world/rss"),
        PredefinedSource("Al Jazeera", SubscriptionType.RSS_FEED, "https://www.aljazeera.com/xml/rss/all.xml"),
        PredefinedSource("NDTV", SubscriptionType.RSS_FEED, "https://feeds.feedburner.com/ndtv/latest"),
        PredefinedSource("The Hindu", SubscriptionType.RSS_FEED, "https://www.thehindu.com/feeder/default.rss"),
        PredefinedSource("The Economic Times", SubscriptionType.RSS_FEED, "https://economictimes.indiatimes.com/rssfeedsdefault.cms"),
        PredefinedSource("Livemint", SubscriptionType.RSS_FEED, "https://www.livemint.com/rss/homepage"),
        PredefinedSource("Moneycontrol", SubscriptionType.RSS_FEED, "https://www.moneycontrol.com/rss/latestnews.xml"),
        PredefinedSource("WSJ", SubscriptionType.RSS_FEED, "https://feeds.a.dj.com/rss/RssPublic.xml"), // Added WSJ RSS
        PredefinedSource("MarketWatch", SubscriptionType.RSS_FEED, "https://www.marketwatch.com/public/rss/mw_topstories.xml"), // Added MarketWatch RSS
        PredefinedSource("Financial Times", SubscriptionType.RSS_FEED, "https://www.ft.com/rss/home") // Added Financial Times RSS
    )

    val xPersonalities = listOf(
        PredefinedSource("Donald Trump", SubscriptionType.TWITTER, "realDonaldTrump"),
        PredefinedSource("Elon Musk", SubscriptionType.TWITTER, "elonmusk"),
        PredefinedSource("Fox News", SubscriptionType.TWITTER, "FoxNews"),
        PredefinedSource("MAGA Voice", SubscriptionType.TWITTER, "MAGAVoice"),
        PredefinedSource("WSJ", SubscriptionType.TWITTER, "wsj"),
        PredefinedSource("CNN News", SubscriptionType.TWITTER, "CNN"),
        PredefinedSource("MarketWatch", SubscriptionType.TWITTER, "MarketWatch"),
        PredefinedSource("Financial Times", SubscriptionType.TWITTER, "FT"),
        PredefinedSource("Yahoo Finance", SubscriptionType.TWITTER, "YahooFinance"),
        PredefinedSource("Jim Cramer", SubscriptionType.TWITTER, "jimcramer"),
        PredefinedSource("Modi", SubscriptionType.TWITTER, "narendramodi")
    )
}
