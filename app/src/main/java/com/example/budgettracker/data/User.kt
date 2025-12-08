package com.example.budgettracker.data

data class User(
    val username: String,
    val email: String,
    val balance: Double = 0.0,
    val profileImageUrl: String? = null
)
