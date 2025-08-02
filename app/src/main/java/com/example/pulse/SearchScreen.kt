package com.example.pulse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.data.Subscription
import com.example.pulse.ui.components.StaticGradientBackground
import com.example.pulse.ui.theme.BackgroundLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onAddSubscription: (Subscription) -> Unit
) {
    // FIX: Added necessary imports for getValue and setValue
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Subscription>>(emptyList()) }

    StaticGradientBackground(colors = listOf(BackgroundLight, Color.White)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Find New Sources", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    // FIX: Corrected the lambda syntax
                    onValueChange = { newQuery -> query = newQuery },
                    label = { Text("Search for news, topics, or people...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Placeholder for search logic and results
                // In a real app, you would trigger a network request here
                // and update the `searchResults` state.

                LazyColumn {
                    items(searchResults) { subscription ->
                        // Display each result with an "Add" button
                    }
                }
            }
        }
    }
}