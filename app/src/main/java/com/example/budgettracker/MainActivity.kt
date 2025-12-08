package com.example.budgettracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.budgettracker.firebase.FirebaseManager
import com.example.budgettracker.viewmodel.AchievementViewModel
import com.example.budgettracker.viewmodel.BudgetViewModel
import com.example.budgettracker.viewmodel.TransactionViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()
    private lateinit var achievementViewModel: AchievementViewModel
    
    companion object {
        const val ADD_TRANSACTION_REQUEST_CODE = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize ViewModels
        achievementViewModel = ViewModelProvider(this).get(AchievementViewModel::class.java)

        // Initialize user data first
        initializeUserData()
    }

    private fun initializeUserData() {
        val currentUser = FirebaseManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // Initialize transaction data
                transactionViewModel.initializeUserData(currentUser.uid)
                
                // Initialize budget data
                budgetViewModel.initializeUserData(currentUser.uid)
                
                // Initialize achievement data
                achievementViewModel.initializeUserData(currentUser.uid)
                
                // After data is initialized, setup UI
                setupUI()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                // If there's an error, try to restart the activity
                recreate()
            }
        }
    }

    private fun setupUI() {
        // Load HomeFragment
        loadFragment(HomeFragment())

        // Setup BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Apply animations to menu items
        for (i in 0 until bottomNav.menu.size()) {
            val menuItem = bottomNav.menu.getItem(i)
            val itemView = bottomNav.findViewById<View>(menuItem.itemId)
            
            itemView?.let { view ->
                view.setOnClickListener {
                    // Apply bounce animation
                    val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_anim)
                    view.startAnimation(bounceAnim)
                    
                    // Handle navigation
                    bottomNav.selectedItemId = menuItem.itemId
                }
            }
        }
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_budget -> loadFragment(BudgetFragment())
                R.id.nav_expenses -> loadFragment(ExpenseFragment())
                R.id.nav_graph -> loadFragment(SpendingAnalyticsFragment())
                R.id.nav_achievements -> loadFragment(AchievementsFragment())
            }
            true
        }

        // Setup FAB
        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivityForResult(intent, ADD_TRANSACTION_REQUEST_CODE)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Profile
        val profileButton: ImageButton = findViewById(R.id.profile_button)
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        fragment.enterTransition = MaterialFadeThrough()
        fragment.exitTransition = MaterialFadeThrough()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_TRANSACTION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is HomeFragment) {
                currentFragment.refreshData()
            }
            
            // Check for new achievements after adding a transaction
            achievementViewModel.checkForAchievements()
        }
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseManager.getCurrentUser() == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
