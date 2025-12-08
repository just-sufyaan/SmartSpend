package com.example.budgettracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.firebase.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _budget = MutableLiveData<Budget>()
    val budget: LiveData<Budget?> = _budget
    
    private val _saveBudgetResult = MutableLiveData<Result<Unit>>()
    val saveBudgetResult: LiveData<Result<Unit>> = _saveBudgetResult
    
    private var currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    fun initializeUserData(userId: String) {
        currentUserId = userId
        loadBudget()
    }

    private fun loadBudget() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val budgetDoc = FirebaseManager.getBudget(userId)
                val data = budgetDoc.data
                if (data != null) {
                    val minBudget = (data["minBudget"] as? Number)?.toDouble() ?: 0.0
                    val maxBudget = (data["maxBudget"] as? Number)?.toDouble() ?: 0.0
                    _budget.value = Budget(minBudget = minBudget, maxBudget = maxBudget)
                } else {
                    // No budget set yet in Firebase
                    _budget.value = null
                }
            } catch (e: Exception) {
                _saveBudgetResult.value = Result.failure(e)
            }
        }
    }
    
    fun saveBudget(minBudget: Double, maxBudget: Double) {
        if (minBudget >= maxBudget) {
            _saveBudgetResult.value = Result.failure(
                IllegalArgumentException("Minimum budget must be less than maximum budget")
            )
            return
        }
        
        val userId = currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _saveBudgetResult.value = Result.failure(Exception("User not authenticated"))
            return
        }
        
        viewModelScope.launch {
            try {
                val budgetData = mapOf(
                    "minBudget" to minBudget,
                    "maxBudget" to maxBudget
                )
                FirebaseManager.saveBudget(userId, budgetData)
                _budget.value = Budget(minBudget = minBudget, maxBudget = maxBudget)
                _saveBudgetResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _saveBudgetResult.value = Result.failure(e)
            }
        }
    }
    
    fun getBudgetStatus(currentExpenses: Double): String {
        val budgetValue = budget.value ?: return "No budget set"
        
        return when {
            currentExpenses < budgetValue.minBudget -> "Under Budget"
            currentExpenses > budgetValue.maxBudget -> "Over Budget"
            else -> "Within Budget"
        }
    }
}

data class Budget(
    val minBudget: Double,
    val maxBudget: Double
) 