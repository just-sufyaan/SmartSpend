package com.example.budgettracker.firebase.models

import com.google.firebase.auth.FirebaseAuth

data class FirebaseTransaction(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: String = "",
    val isExpense: Boolean = true,
    val imageUrl: String? = null,
    val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        val map = hashMapOf(
            "amount" to amount,
            "description" to description,
            "category" to category,
            "date" to date,
            "isExpense" to isExpense,
            "timestamp" to timestamp
        )
        
        // Only add non-null fields
        if (imageUrl != null) {
            map["imageUrl"] = imageUrl
        }
        
        // Always ensure userId is set
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        map["userId"] = currentUserId ?: userId
        
        return map
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): FirebaseTransaction {
            return FirebaseTransaction(
                id = id,
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                description = map["description"] as? String ?: "",
                category = map["category"] as? String ?: "",
                date = map["date"] as? String ?: "",
                isExpense = map["isExpense"] as? Boolean ?: true,
                imageUrl = map["imageUrl"] as? String,
                userId = map["userId"] as? String ?: FirebaseAuth.getInstance().currentUser?.uid ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
} 