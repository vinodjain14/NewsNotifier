package com.example.pulse.utils

import android.content.Context
import android.util.Log
import com.example.pulse.data.Subscription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class SubscriptionManager(private val context: Context) {

    private val gson = Gson()
    private val subscriptionsFile = File(context.filesDir, "subscriptions.json")

    private val _subscriptionsFlow = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptionsFlow: StateFlow<List<Subscription>> = _subscriptionsFlow.asStateFlow()

    init {
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        if (!subscriptionsFile.exists()) {
            _subscriptionsFlow.value = emptyList() // Start with an empty list if no file
            return
        }
        try {
            val json = subscriptionsFile.readText()
            val type = object : TypeToken<List<Subscription>>() {}.type
            val subscriptions: List<Subscription> = gson.fromJson(json, type) ?: emptyList()
            _subscriptionsFlow.value = subscriptions
        } catch (e: Exception) {
            Log.e("SubscriptionManager", "Error loading subscriptions", e)
            _subscriptionsFlow.value = emptyList()
        }
    }

    private fun saveSubscriptions(subscriptions: List<Subscription>) {
        try {
            val json = gson.toJson(subscriptions)
            subscriptionsFile.writeText(json)
            _subscriptionsFlow.value = subscriptions
        } catch (e: Exception) {
            Log.e("SubscriptionManager", "Error saving subscriptions", e)
        }
    }

    fun getSubscriptions(): List<Subscription> {
        return _subscriptionsFlow.value
    }

    fun addSubscription(subscription: Subscription) {
        val currentSubscriptions = _subscriptionsFlow.value.toMutableList()
        if (currentSubscriptions.none { it.sourceUrl == subscription.sourceUrl }) {
            currentSubscriptions.add(subscription)
            saveSubscriptions(currentSubscriptions)
        }
    }

    fun removeSubscription(subscriptionId: String) {
        val currentSubscriptions = _subscriptionsFlow.value.toMutableList()
        currentSubscriptions.removeAll { it.id == subscriptionId }
        saveSubscriptions(currentSubscriptions)
    }

    fun overwriteSubscriptions(subscriptions: List<Subscription>) {
        saveSubscriptions(subscriptions)
    }
}