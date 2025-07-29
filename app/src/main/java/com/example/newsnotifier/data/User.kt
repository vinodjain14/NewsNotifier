package com.example.newsnotifier.data

/**
 * Represents a user in the application.
 * WARNING: This stores a simple hash of the password for demonstration.
 * In a real application, never store passwords directly or use simple hashing.
 */
data class User(
    val name: String,
    val email: String,
    val hashedPassword: String // In a real app, this would be a strong, salted hash
)
