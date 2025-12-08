package com.example.budgettracker

import android.app.Application
import com.google.firebase.FirebaseApp

class BudgetTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 