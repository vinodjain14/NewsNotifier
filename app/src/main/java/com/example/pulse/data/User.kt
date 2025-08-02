package com.example.pulse.data

/**
 * Data class representing a user in the application.
 * Note: This is a simplified representation. In a real application,
 * user data would typically be managed by FirebaseUser or a custom backend.
 */
data class User(
    val uid: String, // Firebase User ID
    val displayName: String?,
    val email: String?
)
