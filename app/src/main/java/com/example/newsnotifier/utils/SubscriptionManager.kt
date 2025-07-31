package com.example.newsnotifier.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.newsnotifier.data.Subscription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("subscriptions", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _subscriptionsFlow = MutableStateFlow<List<Subscription>>(loadSubscriptions())
    val subscriptionsFlow: StateFlow<List<Subscription>> = _subscriptionsFlow.asStateFlow()

    fun addSubscription(subscription: Subscription): Boolean {
        val currentList = _subscriptionsFlow.value.toMutableList()

        if (currentList.any { it.sourceUrl == subscription.sourceUrl && it.type == subscription.type }) {
            return false
        }

        currentList.add(subscription)
        _subscriptionsFlow.value = currentList
        saveSubscriptions(currentList)
        return true
    }

    fun removeSubscription(subscriptionId: String) {
        val currentList = _subscriptionsFlow.value.toMutableList()
        currentList.removeAll { it.id == subscriptionId }
        _subscriptionsFlow.value = currentList
        saveSubscriptions(currentList)
    }

    fun getSubscriptions(): List<Subscription> {
        return _subscriptionsFlow.value
    }

    private fun saveSubscriptions(subscriptions: List<Subscription>) {
        val json = gson.toJson(subscriptions)
        prefs.edit().putString(KEY_SUBSCRIPTIONS, json).apply()
    }

    private fun loadSubscriptions(): List<Subscription> {
        val json = prefs.getString(KEY_SUBSCRIPTIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Subscription>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_SUBSCRIPTIONS = "subscriptions_list"
    }
}