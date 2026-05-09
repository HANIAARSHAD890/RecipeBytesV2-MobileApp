package com.example.recipebytes.models
data class User(
    val uid: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)