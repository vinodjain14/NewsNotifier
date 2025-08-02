package com.example.pulse.utils

import android.content.Context
import android.content.Intent
import com.example.pulse.data.NotificationItem

object SharingUtil {

    fun shareArticle(context: Context, item: NotificationItem) {
        val shareText = buildString {
            append(item.title)
            append("\n\n")
            if (item.message.isNotBlank()) {
                append(item.message)
                append("\n\n")
            }
            append("Source: ${item.sourceName}")
            append("\n\n")
            append("Shared via NOTT - News On The Top")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, item.title)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
    }

    fun shareMultipleArticles(context: Context, items: List<NotificationItem>) {
        val shareText = buildString {
            append("My Reading List from NOTT:\n\n")
            items.forEachIndexed { index, item ->
                append("${index + 1}. ${item.title}")
                append("\n   - ${item.sourceName}")
                append("\n\n")
            }
            append("Shared via NOTT - News On The Top")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "My NOTT Reading List")
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Reading List"))
    }
}