package com.example.pulse.data

import com.google.gson.annotations.SerializedName

data class MarketFeed(
    @SerializedName("Market")
    val market: String,

    @SerializedName("Website name")
    val websiteName: String,

    @SerializedName("URL")
    val url: String
)
