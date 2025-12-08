package com.example.budgettracker.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.firebase.models.FirebaseTransaction
import com.example.budgettracker.firebase.repository.FirebaseTransactionRepository
import com.example.budgettracker.utils.LocalImageStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewModel : ViewModel() {
    private val repository = FirebaseTransactionRepository()
    
    private val _transactions = MutableLiveData<List<FirebaseTransaction>>()
    val transactions: LiveData<List<FirebaseTransaction>> = _transactions
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _transactionResult = MutableLiveData<Result<Unit>>()
    val transactionResult: LiveData<Result<Unit>> = _transactionResult

    private var currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    fun initializeUserData(userId: String) {
        currentUserId = userId
        listenToTransactions()
    }

    private fun listenToTransactions() {
        val userId = currentUserId ?: return
        repository.listenToAllTransactions(
            userId,
            onUpdate = { transactions ->
                _transactions.postValue(transactions)
            },
            onError = { error ->
                _error.postValue(error.message)
            }
        )
    }

    fun addTransaction(
        context: Context,
        amount: Double,
        description: String,
        category: String,
        isExpense: Boolean,
        imageUri: Uri? = null
    ) {
        val userId = currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _transactionResult.value = Result.failure(Exception("User is not authenticated"))
            return
        }
        
        viewModelScope.launch {
            var imagePath: String? = null
            try {
                _loading.value = true
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                // Save image locally if provided
                imagePath = if (imageUri != null) {
                    LocalImageStorage.saveImage(context, imageUri)
                } else null
                
                val transaction = FirebaseTransaction(
                    amount = amount,
                    description = description,
                    category = category,
                    date = dateFormat.format(Date()),
                    isExpense = isExpense,
                    imageUrl = imagePath,
                    userId = userId
                )
                repository.addTransaction(userId, transaction)
                _transactionResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _error.value = e.message
                _transactionResult.value = Result.failure(e)
                // Clean up image if transaction failed
                if (e.message?.contains("Failed to add transaction") == true) {
                    imagePath?.let { path ->
                            LocalImageStorage.deleteImage(path)
                    }
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteTransaction(transaction: FirebaseTransaction) {
        val userId = currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _loading.value = true
                // Delete local image if exists
                transaction.imageUrl?.let { path ->
                    LocalImageStorage.deleteImage(path)
                }
                repository.deleteTransaction(userId, transaction.id)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateTransaction(transaction: FirebaseTransaction) {
        val userId = currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _loading.value = true
                repository.updateTransaction(userId, transaction)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun getTransactionsByDateRange(startDate: String, endDate: String) {
        val userId = currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        // You may want to update this to use a real-time listener as well if needed
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeTransactionListener()
    }
} 