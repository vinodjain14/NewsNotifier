package com.example.newsnotifier.utils

import android.content.Context
import com.example.newsnotifier.data.Subscription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages subscriptions using SharedPreferences for persistence.
 * In a real app, for more complex data, you might use Room Database.
 */
class SubscriptionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("subscriptions_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val SUBSCRIPTIONS_KEY = "subscriptions_list"

    // MutableStateFlow to hold the current list of subscriptions and emit updates
    private val _subscriptionsFlow = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptionsFlow: StateFlow<List<Subscription>> = _subscriptionsFlow.asStateFlow()

    init {
        // Initialize the flow with the currently saved subscriptions when the manager is created
        _subscriptionsFlow.value = getSubscriptionsInternal()
    }

    /**
     * Retrieves all stored subscriptions. This is an internal helper.
     */
    private fun getSubscriptionsInternal(): MutableList<Subscription> {
        val json = prefs.getString(SUBSCRIPTIONS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Subscription>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    /**
     * Retrieves all stored subscriptions. Public access.
     */
    fun getSubscriptions(): List<Subscription> {
        return _subscriptionsFlow.value
    }

    /**
     * Adds a new subscription.
     * @param subscription The subscription to add.
     */
    fun addSubscription(subscription: Subscription) {
        val currentList = _subscriptionsFlow.value.toMutableList()
        if (currentList.none { it.id == subscription.id }) { // Prevent duplicates
            currentList.add(subscription)
            saveSubscriptions(currentList)
        }
    }

    /**
     * Updates an existing subscription.
     * @param updatedSubscription The subscription with updated details.
     */
    fun updateSubscription(updatedSubscription: Subscription) {
        val currentList = _subscriptionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedSubscription.id }
        if (index != -1) {
            currentList[index] = updatedSubscription
            saveSubscriptions(currentList)
        }
    }

    /**
     * Removes a subscription by its ID.
     * @param subscriptionId The ID of the subscription to remove.
     */
    fun removeSubscription(subscriptionId: String) {
        val currentList = _subscriptionsFlow.value.toMutableList()
        currentList.removeIf { it.id == subscriptionId }
        saveSubscriptions(currentList)
    }

    /**
     * Saves the current list of subscriptions to SharedPreferences and updates the flow.
     * @param subscriptions The list of subscriptions to save.
     */
    private fun saveSubscriptions(subscriptions: List<Subscription>) {
        val json = gson.toJson(subscriptions)
        prefs.edit().putString(SUBSCRIPTIONS_KEY, json).apply()
        _subscriptionsFlow.value = subscriptions // Update the flow after saving
    }
}
