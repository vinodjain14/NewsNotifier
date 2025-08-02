package com.example.pulse

import android.webkit.WebView
import androidx.compose.runtime.Composable
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

@Composable
fun ArticleScreen(url: String) {
    val state = rememberWebViewState(url = url)
    WebView(
        state = state,
        onCreated = { webView ->
            webView.settings.javaScriptEnabled = true
        }
    )
}