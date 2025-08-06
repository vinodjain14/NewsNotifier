package com.example.pulse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionSelectionScreen(
    onConfirm: (List<Subscription>) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val marketList: List<MarketFeed> = remember {
        val jsonString = context.assets.open("Finance_Market_RSS_Feeds.json").bufferedReader().use { it.readText() }
        val marketListType = object : TypeToken<List<MarketFeed>>() {}.type
        Gson().fromJson(jsonString, marketListType)
    }

    val groupedFeeds = remember { marketList.groupBy { it.market } }
    val selectedFeeds = remember { mutableStateListOf<MarketFeed>() }

    StaticGradientBackground(colors = listOf(BackgroundLight, Color.White)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Select Your Interests") },
                    actions = {
                        TextButton(onClick = onSkip) {
                            Text("Skip")
                            Icon(Icons.Default.ArrowForward, contentDescription = "Skip")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                AnimatedGradientButton(
                    text = "Confirm Selections",
                    onClick = {
                        val selectedSubscriptions = selectedFeeds.map { feed ->
                            Subscription(
                                name = feed.websiteName,
                                type = SubscriptionType.RSS_FEED,
                                sourceUrl = feed.url,
                                market = feed.market,
                                category = feed.category
                            )
                        }
                        onConfirm(selectedSubscriptions)
                    },
                    enabled = selectedFeeds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    icon = Icons.Default.Check
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                groupedFeeds.forEach { (market, feeds) ->
                    item {
                        Text(
                            text = market,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(feeds) { feed ->
                        FeedSelectionCard(
                            feed = feed,
                            isSelected = selectedFeeds.contains(feed),
                            onToggle = {
                                if (selectedFeeds.contains(feed)) {
                                    selectedFeeds.remove(feed)
                                } else {
                                    selectedFeeds.add(feed)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedSelectionCard(
    feed: MarketFeed,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    ElevatedModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = feed.websiteName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}