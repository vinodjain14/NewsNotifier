package com.example.pulse.utils

import android.content.Context
import com.example.pulse.data.Subscription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user subscriptions using SharedPreferences for local storage.
 */
class SubscriptionManager(private val context: Context) {

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
    private val subscriptionsKey = "subscriptions"

    private val _subscriptionsFlow = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptionsFlow: StateFlow<List<Subscription>> = _subscriptionsFlow.asStateFlow()

    init {
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        val json = prefs.getString(subscriptionsKey, null)
        val type = object : TypeToken<List<Subscription>>() {}.type
        _subscriptionsFlow.value = gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveSubscriptions(subscriptions: List<Subscription>) {
        val json = gson.toJson(subscriptions)
        prefs.edit().putString(subscriptionsKey, json).apply()
        _subscriptionsFlow.value = subscriptions
    }

    /**
     * Overwrites the user's current subscriptions with a new list.
     * @param newSubscriptions The complete new list of subscriptions.
     */
    fun overwriteSubscriptions(newSubscriptions: List<Subscription>) {
        saveSubscriptions(newSubscriptions)
    }

    fun addSubscription(subscription: Subscription) {
        val currentSubscriptions = _subscriptionsFlow.value.toMutableList()
        if (currentSubscriptions.none { it.id == subscription.id }) {
            currentSubscriptions.add(subscription)
            saveSubscriptions(currentSubscriptions)
        }
    }

    fun removeSubscription(subscriptionId: String) {
        val currentSubscriptions = _subscriptionsFlow.value.toMutableList()
        val updatedSubscriptions = currentSubscriptions.filterNot { it.id == subscriptionId }
        saveSubscriptions(updatedSubscriptions)
    }
}