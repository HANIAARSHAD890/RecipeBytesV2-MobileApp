package com.example.recipebytes.models
// Represents a user account with profile details
data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val bio: String = "",
    val profileImage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)