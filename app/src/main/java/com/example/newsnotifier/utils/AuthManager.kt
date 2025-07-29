package com.example.newsnotifier.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.newsnotifier.data.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken // Added import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user authentication state using SharedPreferences.
 * WARNING: This is a simplified and INSECURE authentication manager for demonstration purposes.
 * Passwords are NOT securely hashed or stored. DO NOT use this in a production application.
 * For real apps, use Firebase Authentication, OAuth, or a secure backend.
 */
class AuthManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val KEY_LOGGED_IN_USER = "logged_in_user"
    private val KEY_ALL_USERS = "all_users" // To store multiple registered users

    // StateFlow to observe changes in the logged-in user
    private val _loggedInUserFlow = MutableStateFlow<User?>(null)
    val loggedInUserFlow: StateFlow<User?> = _loggedInUserFlow.asStateFlow()

    init {
        // Initialize the logged-in user state from SharedPreferences on startup
        val userJson = prefs.getString(KEY_LOGGED_IN_USER, null)
        _loggedInUserFlow.value = userJson?.let { gson.fromJson(it, User::class.java) }
    }

    /**
     * Registers a new user.
     * @return true if registration is successful, false if email already exists.
     */
    fun registerUser(name: String, email: String, password: String): Boolean {
        val allUsers = getAllRegisteredUsers().toMutableList()
        if (allUsers.any { it.email.equals(email, ignoreCase = true) }) {
            return false // User with this email already exists
        }

        // In a real app, hash the password securely before storing!
        val newUser = User(name, email, password.hashCode().toString()) // Simple hash for demo
        allUsers.add(newUser)
        saveAllRegisteredUsers(allUsers)
        return true
    }

    /**
     * Logs in a user.
     * @return true if login is successful, false otherwise.
     */
    fun loginUser(email: String, password: String): Boolean {
        val allUsers = getAllRegisteredUsers()
        val user = allUsers.find { it.email.equals(email, ignoreCase = true) && it.hashedPassword == password.hashCode().toString() }

        return if (user != null) {
            saveLoggedInUser(user)
            _loggedInUserFlow.value = user // Update the flow
            true
        } else {
            false
        }
    }

    /**
     * Logs out the current user.
     */
    fun logoutUser() {
        prefs.edit().remove(KEY_LOGGED_IN_USER).apply()
        _loggedInUserFlow.value = null // Clear the flow
    }

    /**
     * Returns the currently logged-in user.
     */
    fun getLoggedInUser(): User? {
        return _loggedInUserFlow.value
    }

    /**
     * Checks if a user is currently logged in.
     */
    fun isLoggedIn(): Boolean {
        return _loggedInUserFlow.value != null
    }

    // --- Private helper methods for SharedPreferences ---

    private fun saveLoggedInUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_LOGGED_IN_USER, userJson).apply()
    }

    private fun getAllRegisteredUsers(): List<User> {
        val json = prefs.getString(KEY_ALL_USERS, null)
        return if (json != null) {
            val type = object : TypeToken<List<User>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveAllRegisteredUsers(users: List<User>) {
        val json = gson.toJson(users)
        prefs.edit().putString(KEY_ALL_USERS, json).apply()
    }
}
