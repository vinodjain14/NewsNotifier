package com.example.pulse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pulse.data.MarketFeed
import com.example.pulse.data.Subscription
import com.example.pulse.data.SubscriptionType
import com.example.pulse.ui.components.AnimatedGradientButton
import com.example.pulse.ui.components.ElevatedModernCard
import com.example.pulse.ui.components.StaticGradientBackground
import com.example.pulse.ui.theme.BackgroundLight
import com.example.pulse.utils.SubscriptionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

@Composable
fun OnboardingScreen(
    subscriptionManager: SubscriptionManager,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val marketList: List<MarketFeed> = remember {
        val jsonString = context.assets.open("Finance_Market_RSS_Feeds.json").bufferedReader().use { it.readText() }
        val marketListType = object : TypeToken<List<MarketFeed>>() {}.type
        Gson().fromJson(jsonString, marketListType)
    }
    // Correctly use the "Category" field for display
    val categories = remember { marketList.map { it.category }.distinct().sorted() }
    val selectedCategories = remember { mutableStateListOf<String>() }

    StaticGradientBackground(colors = listOf(BackgroundLight, Color.White)) {
        Scaffold(
            containerColor = Color.Transparent,
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "What are you interested in?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryRow(
                                category = category,
                                isSelected = selectedCategories.contains(category),
                                onToggle = {
                                    if (selectedCategories.contains(category)) {
                                        selectedCategories.remove(category)
                                    } else {
                                        selectedCategories.add(category)
                                    }
                                }
                            )
                        }
                    }

                    AnimatedGradientButton(
                        text = "Done",
                        onClick = {
                            // Filter subscriptions based on the selected "Category"
                            val subscriptions = marketList
                                .filter { it.category in selectedCategories }
                                .map { marketFeed ->
                                    Subscription(
                                        id = UUID.randomUUID().toString(),
                                        name = marketFeed.websiteName,
                                        type = SubscriptionType.RSS_FEED,
                                        sourceUrl = marketFeed.url,
                                        market = marketFeed.market,
                                        category = marketFeed.category
                                    )
                                }
                            subscriptionManager.overwriteSubscriptions(subscriptions)
                            onOnboardingComplete()
                        },
                        enabled = selectedCategories.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        icon = Icons.Default.Check
                    )
                }
            }
        )
    }
}

@Composable
fun CategoryRow(category: String, isSelected: Boolean, onToggle: () -> Unit) {
    ElevatedModernCard(onClick = onToggle) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = category, style = MaterialTheme.typography.bodyLarge)
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        }
    }
}