package com.example.budgettracker.data

import android.graphics.Color

/**
 * Category data class representing a transaction category with name, color, and type
 */
data class Category(
    val name: String,
    val color: Int,
    val isExpense: Boolean
)

/**
 * Object containing predefined categories for the app
 */
object Categories {
    // Expense categories
    val FOOD = Category("Food", Color.parseColor("#FF5722"), true)
    val HOUSING = Category("Housing", Color.parseColor("#E91E63"), true)
    val TRANSPORTATION = Category("Transportation", Color.parseColor("#9C27B0"), true)
    val UTILITIES = Category("Utilities", Color.parseColor("#673AB7"), true)
    val ENTERTAINMENT = Category("Entertainment", Color.parseColor("#3F51B5"), true)
    val HEALTHCARE = Category("Healthcare", Color.parseColor("#2196F3"), true)
    val EDUCATION = Category("Education", Color.parseColor("#03A9F4"), true)
    val SHOPPING = Category("Shopping", Color.parseColor("#00BCD4"), true)
    val PERSONAL_CARE = Category("Personal Care", Color.parseColor("#009688"), true)
    val TRAVEL = Category("Travel", Color.parseColor("#4CAF50"), true)
    val DEBT = Category("Debt", Color.parseColor("#8BC34A"), true)
    val GIFTS = Category("Gifts", Color.parseColor("#CDDC39"), true)
    val OTHER_EXPENSE = Category("Other Expense", Color.parseColor("#FFC107"), true)
    
    // Income categories
    val SALARY = Category("Salary", Color.parseColor("#4CAF50"), false)
    val BUSINESS = Category("Business", Color.parseColor("#8BC34A"), false)
    val INVESTMENTS = Category("Investments", Color.parseColor("#CDDC39"), false)
    val RENTAL = Category("Rental", Color.parseColor("#FFEB3B"), false)
    val REFUNDS = Category("Refunds", Color.parseColor("#FFC107"), false)
    val GIFTS_INCOME = Category("Gifts Received", Color.parseColor("#FF9800"), false)
    val OTHER_INCOME = Category("Other Income", Color.parseColor("#FF5722"), false)
    
    // Get all predefined categories
    val getAllCategories = listOf(
        FOOD, HOUSING, TRANSPORTATION, UTILITIES, ENTERTAINMENT, HEALTHCARE,
        EDUCATION, SHOPPING, PERSONAL_CARE, TRAVEL, DEBT, GIFTS, OTHER_EXPENSE,
        SALARY, BUSINESS, INVESTMENTS, RENTAL, REFUNDS, GIFTS_INCOME, OTHER_INCOME
    )
    
    // Get all expense categories
    val getExpenseCategories = getAllCategories.filter { it.isExpense }
    
    // Get all income categories
    val getIncomeCategories = getAllCategories.filter { !it.isExpense }
    
    // Get category by name
    fun getCategoryByName(name: String): Category {
        return getAllCategories.find { it.name.equals(name, ignoreCase = true) }
            ?: if (name.contains("expense", ignoreCase = true)) {
                OTHER_EXPENSE
            } else {
                OTHER_INCOME
            }
    }
} 