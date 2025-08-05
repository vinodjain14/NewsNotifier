package com.example.pulse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pulse.data.MarketFeed
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseMarketScreen(
    onNavigateToAllNotifications: () -> Unit,
    onNavigateToManageSubscriptions: () -> Unit,
    onNavigateToMyProfile: () -> Unit
) {
    val context = LocalContext.current
    val jsonString = context.assets.open("Finance_Market_RSS_Feeds.json").bufferedReader().use { it.readText() }
    val marketListType = object : TypeToken<List<MarketFeed>>() {}.type
    val marketList: List<MarketFeed> = Gson().fromJson(jsonString, marketListType)
    val marketNames = marketList.map { it.market }.distinct().sorted()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pulse Market", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToAllNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "All Notifications")
                    }
                    IconButton(onClick = onNavigateToManageSubscriptions) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "Manage Subscriptions")
                    }
                    IconButton(onClick = onNavigateToMyProfile) {
                        Icon(Icons.Default.Person, contentDescription = "My Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(marketNames) { marketName ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAllNotifications() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = marketName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
