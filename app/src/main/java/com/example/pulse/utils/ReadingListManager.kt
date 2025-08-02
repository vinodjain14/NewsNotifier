package com.example.pulse

import android.content.Context
import android.content.SharedPreferences
import com.example.pulse.data.NotificationItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReadingListManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("reading_list", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _readingListFlow = MutableStateFlow<List<NotificationItem>>(loadReadingList())
    val readingListFlow: StateFlow<List<NotificationItem>> = _readingListFlow.asStateFlow()

    fun addToReadingList(notification: NotificationItem) {
        val currentList = _readingListFlow.value.toMutableList()
        if (!currentList.any { it.id == notification.id }) {
            currentList.add(notification)
            _readingListFlow.value = currentList
            saveReadingList(currentList)
        }
    }

    fun removeFromReadingList(notificationId: String) {
        val currentList = _readingListFlow.value.toMutableList()
        currentList.removeAll { it.id == notificationId }
        _readingListFlow.value = currentList
        saveReadingList(currentList)
    }

    fun clearReadingList() {
        _readingListFlow.value = emptyList()
        saveReadingList(emptyList())
    }

    fun getReadingList(): List<NotificationItem> {
        return _readingListFlow.value
    }

    fun isInReadingList(notificationId: String): Boolean {
        return _readingListFlow.value.any { it.id == notificationId }
    }

    private fun saveReadingList(items: List<NotificationItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(KEY_READING_LIST, json).apply()
    }

    private fun loadReadingList(): List<NotificationItem> {
        val json = prefs.getString(KEY_READING_LIST, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NotificationItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_READING_LIST = "reading_list_items"
    }
}