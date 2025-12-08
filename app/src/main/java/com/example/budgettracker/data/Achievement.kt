package com.example.budgettracker.data

/**
 * Data class representing an achievement/badge that can be earned in the app
 */
data class Achievement(
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconResourceId: Int,
    val type: AchievementType,
    val threshold: Int,
    val isEarned: Boolean = false,
    val dateEarned: String? = null
)

/**
 * Enum representing different types of achievements
 */
enum class AchievementType {
    TRANSACTION_COUNT,      // Based on number of transactions recorded
    BUDGET_STREAK,          // Days/weeks staying under budget
    SAVING_GOAL,            // Reaching a saving goal
    LOGIN_STREAK,           // Days with consecutive app logins
    CATEGORY_COMPLETE,      // Using all transaction categories
    SPECIAL                 // Special achievements
}

/**
 * Object containing predefined achievements for the app
 */
object Achievements {
    // Transaction achievements
    val FIRST_TRANSACTION = Achievement(
        name = "First Steps",
        description = "Record your first transaction",
        iconResourceId = android.R.drawable.ic_menu_edit,
        type = AchievementType.TRANSACTION_COUNT,
        threshold = 1
    )
    
    val TEN_TRANSACTIONS = Achievement(
        name = "Getting Started",
        description = "Record 10 transactions",
        iconResourceId = android.R.drawable.ic_menu_recent_history,
        type = AchievementType.TRANSACTION_COUNT,
        threshold = 10
    )
    
    val FIFTY_TRANSACTIONS = Achievement(
        name = "Consistent Tracker",
        description = "Record 50 transactions",
        iconResourceId = android.R.drawable.ic_menu_agenda,
        type = AchievementType.TRANSACTION_COUNT,
        threshold = 50
    )

    val HUNDRED_TRANSACTIONS = Achievement(
        name = "Transaction Master",
        description = "Record 100 transactions",
        iconResourceId = android.R.drawable.ic_menu_sort_by_size,
        type = AchievementType.TRANSACTION_COUNT,
        threshold = 100
    )
    
    // Budget achievements
    val UNDER_BUDGET_WEEK = Achievement(
        name = "Budget Master: Week",
        description = "Stay under budget for a full week",
        iconResourceId = android.R.drawable.ic_menu_week,
        type = AchievementType.BUDGET_STREAK,
        threshold = 7
    )
    
    val UNDER_BUDGET_MONTH = Achievement(
        name = "Budget Master: Month",
        description = "Stay under budget for a full month",
        iconResourceId = android.R.drawable.ic_menu_month,
        type = AchievementType.BUDGET_STREAK,
        threshold = 30
    )

    val UNDER_BUDGET_QUARTER = Achievement(
        name = "Budget Expert: Quarter",
        description = "Stay under budget for three months",
        iconResourceId = android.R.drawable.ic_menu_today,
        type = AchievementType.BUDGET_STREAK,
        threshold = 90
    )
    
    // Savings achievements
    val SAVING_START = Achievement(
        name = "Saving Starter",
        description = "Save your first R100",
        iconResourceId = android.R.drawable.ic_menu_save,
        type = AchievementType.SAVING_GOAL,
        threshold = 100
    )
    
    val SAVING_PRO = Achievement(
        name = "Saving Pro",
        description = "Save R1000 total",
        iconResourceId = android.R.drawable.ic_menu_directions,
        type = AchievementType.SAVING_GOAL,
        threshold = 1000
    )

    val SAVING_EXPERT = Achievement(
        name = "Saving Expert",
        description = "Save R5000 total",
        iconResourceId = android.R.drawable.ic_menu_compass,
        type = AchievementType.SAVING_GOAL,
        threshold = 5000
    )

    val SAVING_MASTER = Achievement(
        name = "Saving Master",
        description = "Save R10000 total",
        iconResourceId = android.R.drawable.ic_menu_mylocation,
        type = AchievementType.SAVING_GOAL,
        threshold = 10000
    )
    
    // Login streak achievements
    val THREE_DAY_STREAK = Achievement(
        name = "Three-Day Streak",
        description = "Use the app for 3 consecutive days",
        iconResourceId = android.R.drawable.ic_menu_today,
        type = AchievementType.LOGIN_STREAK,
        threshold = 3
    )
    
    val WEEK_STREAK = Achievement(
        name = "Week Streak",
        description = "Use the app for 7 consecutive days",
        iconResourceId = android.R.drawable.ic_menu_my_calendar,
        type = AchievementType.LOGIN_STREAK,
        threshold = 7
    )

    val MONTH_STREAK = Achievement(
        name = "Monthly Dedication",
        description = "Use the app for 30 consecutive days",
        iconResourceId = android.R.drawable.ic_menu_month,
        type = AchievementType.LOGIN_STREAK,
        threshold = 30
    )
    
    // Category achievements
    val CATEGORY_EXPLORER = Achievement(
        name = "Category Explorer",
        description = "Use 5 different expense categories",
        iconResourceId = android.R.drawable.ic_menu_sort_by_size,
        type = AchievementType.CATEGORY_COMPLETE,
        threshold = 5
    )
    
    val CATEGORY_MASTER = Achievement(
        name = "Category Master",
        description = "Use all expense categories",
        iconResourceId = android.R.drawable.ic_menu_slideshow,
        type = AchievementType.CATEGORY_COMPLETE,
        threshold = -1  // Special case: check for all categories
    )
    
    // Special achievements
    val FIRST_RECEIPT = Achievement(
        name = "Record Keeper",
        description = "Attach your first receipt photo",
        iconResourceId = android.R.drawable.ic_menu_camera,
        type = AchievementType.SPECIAL,
        threshold = 1
    )

    val BUDGET_SETUP = Achievement(
        name = "Budget Planner",
        description = "Set up your first budget",
        iconResourceId = android.R.drawable.ic_menu_manage,
        type = AchievementType.SPECIAL,
        threshold = 1
    )

    val EXPENSE_ANALYZER = Achievement(
        name = "Data Analyst",
        description = "View your spending analytics for the first time",
        iconResourceId = android.R.drawable.ic_menu_report_image,
        type = AchievementType.SPECIAL,
        threshold = 1
    )

    val PROFILE_COMPLETE = Achievement(
        name = "Identity Set",
        description = "Complete your user profile",
        iconResourceId = android.R.drawable.ic_menu_myplaces,
        type = AchievementType.SPECIAL,
        threshold = 1
    )
    
    // Get all predefined achievements
    val getAllAchievements = listOf(
        FIRST_TRANSACTION, TEN_TRANSACTIONS, FIFTY_TRANSACTIONS, HUNDRED_TRANSACTIONS,
        UNDER_BUDGET_WEEK, UNDER_BUDGET_MONTH, UNDER_BUDGET_QUARTER,
        SAVING_START, SAVING_PRO, SAVING_EXPERT, SAVING_MASTER,
        THREE_DAY_STREAK, WEEK_STREAK, MONTH_STREAK,
        CATEGORY_EXPLORER, CATEGORY_MASTER,
        FIRST_RECEIPT, BUDGET_SETUP, EXPENSE_ANALYZER, PROFILE_COMPLETE
    )
} 