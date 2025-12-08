package com.example.budgettracker.firebase.repository

import android.util.Log
import com.example.budgettracker.firebase.FirebaseManager
import com.example.budgettracker.firebase.models.FirebaseTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseTransactionRepository {
    private var transactionListener: ListenerRegistration? = null

    fun listenToAllTransactions(userId: String, onUpdate: (List<FirebaseTransaction>) -> Unit, onError: (Exception) -> Unit) {
        // Remove previous listener if any
        transactionListener?.remove()
        val verifiedUserId = userId.ifEmpty {
            FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("User not authenticated")
        }
        transactionListener = FirebaseManager.db.collection("users")
            .document(verifiedUserId)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepository", "Snapshot listener error", error)
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.map { doc ->
                        FirebaseTransaction.fromMap(doc.data ?: emptyMap(), doc.id)
                    }
                    onUpdate(transactions)
                }
            }
    }

    fun removeTransactionListener() {
        transactionListener?.remove()
        transactionListener = null
    }

    suspend fun addTransaction(userId: String, transaction: FirebaseTransaction) {
        try {
            withContext(Dispatchers.IO) {
                // Ensure we're using the correct user ID
                val verifiedUserId = userId.ifEmpty {
                    FirebaseAuth.getInstance().currentUser?.uid
                        ?: throw Exception("User not authenticated")
                }
                
                FirebaseManager.db.collection("users")
                    .document(verifiedUserId)
                    .collection("transactions")
                    .add(transaction.toMap())
                    .await()
                
                Log.d("FirebaseRepository", "Transaction added successfully")
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to add transaction", e)
            throw Exception("Failed to add transaction: ${e.message}")
        }
    }

    suspend fun getAllTransactions(userId: String): Result<List<FirebaseTransaction>> {
        return try {
            withContext(Dispatchers.IO) {
            // Ensure we're using the correct user ID
            val verifiedUserId = userId.ifEmpty {
                FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not authenticated")
            }
            
            val snapshot = FirebaseManager.db.collection("users")
                .document(verifiedUserId)
                .collection("transactions")
                .get()
                .await()
            
            val transactions = snapshot.documents.map { doc ->
                FirebaseTransaction.fromMap(doc.data ?: emptyMap(), doc.id)
            }
            
                Result.success(transactions)
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to get transactions", e)
            Result.failure(Exception("Failed to get transactions: ${e.message}"))
        }
    }

    suspend fun getTransactionsByDateRange(userId: String, startDate: String, endDate: String): Flow<List<FirebaseTransaction>> = flow {
        try {
            // Ensure we're using the correct user ID
            val verifiedUserId = userId.ifEmpty {
                FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not authenticated")
            }
            
            val snapshot = FirebaseManager.db.collection("users")
                .document(verifiedUserId)
                .collection("transactions")
                .get()
                .await()
            
            val transactions = snapshot.documents
                .map { doc -> FirebaseTransaction.fromMap(doc.data ?: emptyMap(), doc.id) }
                .filter { it.date in startDate..endDate }
                
            emit(transactions)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to get transactions by date range", e)
            emit(emptyList<FirebaseTransaction>())
            throw Exception("Failed to retrieve transactions by date range: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteTransaction(userId: String, transactionId: String) {
        try {
            withContext(Dispatchers.IO) {
                // Ensure we're using the correct user ID
                val verifiedUserId = userId.ifEmpty {
                    FirebaseAuth.getInstance().currentUser?.uid
                        ?: throw Exception("User not authenticated")
                }
                
                FirebaseManager.db.collection("users")
                    .document(verifiedUserId)
                    .collection("transactions")
                    .document(transactionId)
                    .delete()
                    .await()
                    
                Log.d("FirebaseRepository", "Transaction deleted: $transactionId")
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to delete transaction", e)
            throw Exception("Failed to delete transaction: ${e.message}")
        }
    }

    suspend fun updateTransaction(userId: String, transaction: FirebaseTransaction) {
        try {
            withContext(Dispatchers.IO) {
                // Ensure we're using the correct user ID
                val verifiedUserId = userId.ifEmpty {
                    FirebaseAuth.getInstance().currentUser?.uid
                        ?: throw Exception("User not authenticated")
                }
                
                FirebaseManager.db.collection("users")
                    .document(verifiedUserId)
                    .collection("transactions")
                    .document(transaction.id)
                    .set(transaction.toMap())
                    .await()
                    
                Log.d("FirebaseRepository", "Transaction updated: ${transaction.id}")
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to update transaction", e)
            throw Exception("Failed to update transaction: ${e.message}")
        }
    }
} 