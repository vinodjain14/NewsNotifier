package com.example.newsnotifier.utils

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.example.newsnotifier.R // Make sure this import is present and correct

/**
 * Manages user authentication state using Firebase Authentication.
 */
class AuthManager(private val context: Context) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient

    // MutableStateFlow to hold the current logged-in user
    private val _loggedInUserFlow = MutableStateFlow<FirebaseUser?>(null)
    val loggedInUserFlow: StateFlow<FirebaseUser?> = _loggedInUserFlow.asStateFlow()

    init {
        // Configure Google Sign In to request the user's ID, email address, and basic profile.
        // The ID token is required to authenticate with Firebase.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // This string is generated from google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Listen for changes in the Firebase authentication state
        firebaseAuth.addAuthStateListener { auth ->
            _loggedInUserFlow.value = auth.currentUser
        }
    }

    /**
     * Provides the Intent needed to start the Google Sign-In flow.
     */
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handles the result from the Google Sign-In activity.
     * Authenticates with Firebase using the Google ID token.
     * @param data The Intent data from the activity result.
     * @return True if authentication was successful, false otherwise.
     */
    suspend fun firebaseAuthWithGoogle(data: Intent?): Boolean {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            println("Firebase Google Auth failed: ${e.message}")
            false
        }
    }

    /**
     * Logs out the current user from Firebase and Google Sign-In.
     */
    fun logoutUser() {
        firebaseAuth.signOut()
        googleSignInClient.signOut() // Also sign out from Google
    }

    /**
     * Returns the currently logged-in Firebase user.
     * This method is deprecated in favor of observing loggedInUserFlow.
     * Use loggedInUserFlow.value directly or collectAsState() in Composables.
     */
    @Deprecated("Use loggedInUserFlow.value or collectAsState() in Composables instead.")
    fun getLoggedInUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}
    