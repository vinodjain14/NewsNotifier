package com.example.newsnotifier.utils

import android.content.Context
import com.example.newsnotifier.data.Subscription
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages user subscriptions using Cloud Firestore for real-time, cloud-synced data.
 */
class SubscriptionManager(private val context: Context) {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _subscriptionsFlow = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptionsFlow: StateFlow<List<Subscription>> = _subscriptionsFlow.asStateFlow()

    init {
        // Listen for authentication changes. When a user logs in, start listening
        // to their subscriptions in Firestore.
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in, attach a real-time listener
                val userSubscriptionsRef = db.collection("users").document(user.uid).collection("subscriptions")

                userSubscriptionsRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // In a real app, you might want to show an error to the user
                        _subscriptionsFlow.value = emptyList()
                        return@addSnapshotListener
                    }

                    // When the data changes in Firestore, this code runs automatically
                    // and updates the app's UI.
                    val subscriptions = snapshot?.toObjects(Subscription::class.java) ?: emptyList()
                    _subscriptionsFlow.value = subscriptions
                }
            } else {
                // User is signed out, clear the subscriptions
                _subscriptionsFlow.value = emptyList()
            }
        }
    }

    /**
     * Overwrites the user's current subscriptions with a new list.
     * This is more efficient than deleting and adding one by one.
     * @param newSubscriptions The complete new list of subscriptions.
     */
    suspend fun overwriteSubscriptions(newSubscriptions: List<Subscription>) {
        val user = auth.currentUser ?: return // Exit if no user is logged in

        val subscriptionsRef = db.collection("users").document(user.uid).collection("subscriptions")

        // First, delete all existing subscriptions for the user.
        val existingSubs = subscriptionsRef.get().await()
        val batch = db.batch()
        existingSubs.documents.forEach { doc ->
            batch.delete(doc.reference)
        }

        // Second, add all the new subscriptions.
        newSubscriptions.forEach { subscription ->
            val newDocRef = subscriptionsRef.document(subscription.id)
            batch.set(newDocRef, subscription)
        }

        // Commit all the changes at once.
        batch.commit().await()
    }

    // The individual add/remove methods are kept for potential manual management screens.
    // Note: These are less efficient for bulk updates.

    fun addSubscription(subscription: Subscription) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("subscriptions").document(subscription.id)
            .set(subscription)
    }

    fun removeSubscription(subscriptionId: String) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("subscriptions").document(subscriptionId)
            .delete()
    }
}
