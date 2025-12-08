package com.example.budgettracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.Achievement
import com.example.budgettracker.data.AchievementType
import com.example.budgettracker.data.Achievements
import com.example.budgettracker.firebase.FirebaseManager
import com.example.budgettracker.firebase.models.FirebaseTransaction
import com.example.budgettracker.firebase.repository.FirebaseTransactionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AchievementViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _allAchievements = MutableLiveData<List<Achievement>>()
    val allAchievements: LiveData<List<Achievement>> = _allAchievements
    
    private val _earnedAchievements = MutableLiveData<List<Achievement>>()
    val earnedAchievements: LiveData<List<Achievement>> = _earnedAchievements
    
    private val _newAchievement = MutableLiveData<Achievement?>()
    val newAchievement: LiveData<Achievement?> = _newAchievement

    private val transactionRepository = FirebaseTransactionRepository()
    private var currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid
    
    init {
        // Initialize with empty lists
        _allAchievements.value = emptyList()
        _earnedAchievements.value = emptyList()
    }

    fun initializeUserData(userId: String) {
        currentUserId = userId
        loadAchievements()
    }

    private fun loadAchievements() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val snapshot = FirebaseManager.getAchievements(userId)
                val achievements = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    try {
                        Achievement(
                            name = data["name"] as? String ?: return@mapNotNull null,
                            description = data["description"] as? String ?: return@mapNotNull null,
                            iconResourceId = (data["iconResourceId"] as? Number)?.toInt() ?: return@mapNotNull null,
                            type = AchievementType.valueOf(data["type"] as? String ?: return@mapNotNull null),
                            threshold = (data["threshold"] as? Number)?.toInt() ?: return@mapNotNull null,
                            isEarned = data["isEarned"] as? Boolean ?: false,
                            dateEarned = data["dateEarned"] as? String
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _allAchievements.value = achievements
                _earnedAchievements.value = achievements.filter { it.isEarned }
                
                // If we don't have achievements in Firebase yet, initialize with predefined ones
                if (achievements.isEmpty()) {
                    initializePredefinedAchievements(userId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private suspend fun initializePredefinedAchievements(userId: String) {
        try {
            Achievements.getAllAchievements.forEach { achievement ->
                val achievementData = mapOf(
                    "name" to achievement.name,
                    "description" to achievement.description,
                    "iconResourceId" to achievement.iconResourceId,
                    "type" to achievement.type.name,
                    "threshold" to achievement.threshold,
                    "isEarned" to false,
                    "dateEarned" to ""
                )
                FirebaseManager.saveAchievement(userId, achievementData)
            }
            
            // Reload achievements after initialization
            loadAchievements()
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Clear the new achievement notification
     */
    fun clearNewAchievementNotification() {
        _newAchievement.value = null
    }
    
    /**
     * Check if any achievements have been earned based on user activity
     */
    fun checkForAchievements() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                var transactions: List<FirebaseTransaction> = emptyList()
                
                // Get all transactions for various checks
                transactionRepository.getAllTransactions(userId).let { result ->
                    result.fold(
                        onSuccess = { transactionsList -> transactions = transactionsList },
                        onFailure = { /* Handle error */ }
                    )
                }
                
                if (transactions.isNotEmpty()) {
                // Check transaction count achievements
                    checkTransactionCountAchievements(userId, transactions.size)
                    
                    // Check category usage achievements
                    checkCategoryAchievements(userId, transactions)
                    
                    // Check login streak achievements using timestamps
                    checkLoginStreakAchievements(userId, transactions)

                    // Check budget achievements 
                    checkBudgetAchievements(userId, transactions)
                    
                    // Check saving goal achievements
                    checkSavingAchievements(userId, transactions)
                    
                    // Check for receipt attachments
                    checkReceiptAchievement(userId, transactions)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private suspend fun checkTransactionCountAchievements(userId: String, transactionCount: Int) {
        checkAchievement(userId, Achievements.FIRST_TRANSACTION, transactionCount)
        checkAchievement(userId, Achievements.TEN_TRANSACTIONS, transactionCount)
        checkAchievement(userId, Achievements.FIFTY_TRANSACTIONS, transactionCount)
        checkAchievement(userId, Achievements.HUNDRED_TRANSACTIONS, transactionCount)
    }
    
    private suspend fun checkCategoryAchievements(userId: String, transactions: List<FirebaseTransaction>) {
        val uniqueCategories = transactions.map { it.category }.distinct().size
        checkAchievement(userId, Achievements.CATEGORY_EXPLORER, uniqueCategories)
        
        // Special case for Category Master - requires using all predefined categories
        val expectedCategoryCount = 13 // Number of expense categories in Categories object
        if (uniqueCategories >= expectedCategoryCount) {
            markAchievementAsEarned(userId, Achievements.CATEGORY_MASTER)
        }
    }
    
    private suspend fun checkLoginStreakAchievements(userId: String, transactions: List<FirebaseTransaction>) {
        val consecutiveDays = calculateConsecutiveDays(transactions)
        checkAchievement(userId, Achievements.THREE_DAY_STREAK, consecutiveDays)
        checkAchievement(userId, Achievements.WEEK_STREAK, consecutiveDays)
        checkAchievement(userId, Achievements.MONTH_STREAK, consecutiveDays)
    }
    
    private suspend fun calculateConsecutiveDays(transactions: List<FirebaseTransaction>): Int {
        if (transactions.isEmpty()) return 0
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val uniqueDatesOrdered = transactions
            .map { it.date }
            .distinct()
            .mapNotNull { dateStr -> 
                try { dateFormat.parse(dateStr)?.time ?: 0 } 
                catch (e: Exception) { null } 
            }
            .sorted()
            .reversed() // Get most recent dates first
        
        if (uniqueDatesOrdered.isEmpty()) return 0
        
        var maxConsecutiveDays = 1
        var currentStreak = 1
        
        for (i in 1 until uniqueDatesOrdered.size) {
            val currentDate = uniqueDatesOrdered[i-1]
            val nextDate = uniqueDatesOrdered[i]
            
            val diffInDays = (currentDate - nextDate) / (1000 * 60 * 60 * 24)
            
            if (diffInDays == 1L) {
                currentStreak++
                maxConsecutiveDays = maxOf(maxConsecutiveDays, currentStreak)
            } else if (diffInDays > 1L) {
                currentStreak = 1
            }
        }
        
        return maxConsecutiveDays
    }
    
    private suspend fun checkBudgetAchievements(userId: String, transactions: List<FirebaseTransaction>) {
        try {
            val budgetDoc = FirebaseManager.getBudget(userId)
            val data = budgetDoc.data
            
            if (data != null) {
                // Mark the budget setup achievement as earned
                checkAchievement(userId, Achievements.BUDGET_SETUP, 1)
                
                // Budget streak achievements can be implemented here in the future
                // This would require tracking days under budget over time
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private suspend fun checkSavingAchievements(userId: String, transactions: List<FirebaseTransaction>) {
        // Calculate total savings (income - expense)
        val totalIncome = transactions.filter { !it.isExpense }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.isExpense }.sumOf { it.amount }
        val totalSavings = totalIncome - totalExpenses
        
        if (totalSavings > 0) {
            checkAchievement(userId, Achievements.SAVING_START, totalSavings.toInt())
            checkAchievement(userId, Achievements.SAVING_PRO, totalSavings.toInt())
            checkAchievement(userId, Achievements.SAVING_EXPERT, totalSavings.toInt())
            checkAchievement(userId, Achievements.SAVING_MASTER, totalSavings.toInt())
        }
    }
    
    private suspend fun checkReceiptAchievement(userId: String, transactions: List<FirebaseTransaction>) {
        val hasReceipt = transactions.any { !it.imageUrl.isNullOrEmpty() }
        if (hasReceipt) {
            checkAchievement(userId, Achievements.FIRST_RECEIPT, 1)
        }
    }

    private suspend fun checkAchievement(userId: String, achievement: Achievement, currentValue: Int) {
        try {
            val snapshot = FirebaseManager.getAchievements(userId)
            val existing = snapshot.documents.any { doc ->
                val data = doc.data
                data?.get("name") as? String == achievement.name && 
                data["isEarned"] as? Boolean == true
            }
            
            if (!existing && currentValue >= achievement.threshold) {
                markAchievementAsEarned(userId, achievement)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Mark an achievement as earned
     */
    private suspend fun markAchievementAsEarned(userId: String, achievement: Achievement) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            
            val earnedAchievement = achievement.copy(
                isEarned = true,
                dateEarned = currentDate
            )
            
            // First find the existing achievement document
            val snapshot = FirebaseManager.getAchievements(userId)
            var docId: String? = null
            
            for (doc in snapshot.documents) {
                if (doc.data?.get("name") as? String == achievement.name) {
                    docId = doc.id
                    break
                }
            }
            
            // If found, update it
            if (docId != null) {
            val achievementData = mapOf<String, Any>(
                "name" to earnedAchievement.name,
                "description" to earnedAchievement.description,
                "iconResourceId" to earnedAchievement.iconResourceId,
                "type" to earnedAchievement.type.name,
                "threshold" to earnedAchievement.threshold,
                "isEarned" to earnedAchievement.isEarned,
                "dateEarned" to (earnedAchievement.dateEarned ?: "")
            )
            
                FirebaseManager.updateAchievement(userId, docId, achievementData)
            
                // Update the new achievement notification and earned achievements list
            _newAchievement.postValue(earnedAchievement)
                
                // Update local list
                val currentList = _earnedAchievements.value?.toMutableList() ?: mutableListOf()
                currentList.add(earnedAchievement)
                _earnedAchievements.postValue(currentList)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
} 