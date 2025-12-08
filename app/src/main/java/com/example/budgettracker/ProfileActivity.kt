package com.example.budgettracker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.budgettracker.firebase.FirebaseManager
import com.example.budgettracker.firebase.models.FirebaseTransaction
import com.example.budgettracker.viewmodel.AchievementViewModel
import com.example.budgettracker.viewmodel.TransactionViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.example.budgettracker.utils.ImageLoader
import com.example.budgettracker.utils.LocalImageStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class ProfileActivity : AppCompatActivity() {

    private lateinit var profilePic: ImageView
    private lateinit var usernameInput: TextInputEditText
    private lateinit var currentPasswordInput: TextInputEditText
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var saveUsernameBtn: Button
    private lateinit var savePasswordBtn: Button
    private lateinit var logoutBtn: Button
    private lateinit var versionText: TextView
    private lateinit var awardsText: TextView
    private lateinit var statsText: TextView
    
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val achievementViewModel: AchievementViewModel by viewModels()
    
    private var selectedImageUri: Uri? = null
    private var profileImageUrl: String? = null
    private var currentUserId: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                
                // Show the selected image immediately
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(profilePic)
                
                // Upload to Firebase
                uploadProfileImage(uri)
                    }
        }
    }
    
    private fun uploadProfileImage(uri: Uri) {
        val userId = currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        lifecycleScope.launch {
            try {
                // Show progress
                Toast.makeText(this@ProfileActivity, "Uploading profile image...", Toast.LENGTH_SHORT).show()
                    
                // Upload to Firebase Storage and update profile
                val imageUrl = FirebaseManager.uploadProfileImage(userId, uri)
                    
                // Update UI and save the URL
                profileImageUrl = imageUrl
                
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    
                    // Check for profile complete achievement
                    achievementViewModel.checkForAchievements()
                }
                } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize views
        profilePic = findViewById(R.id.profile_pic)
        usernameInput = findViewById(R.id.username_input)
        currentPasswordInput = findViewById(R.id.current_password_input)
        newPasswordInput = findViewById(R.id.new_password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        saveUsernameBtn = findViewById(R.id.save_username_btn)
        savePasswordBtn = findViewById(R.id.save_password_btn)
        logoutBtn = findViewById(R.id.logout_btn)
        versionText = findViewById(R.id.version_text)
        awardsText = findViewById(R.id.awards_text)
        statsText = findViewById(R.id.stats_text)

        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (currentUserId == null) {
            // Handle not logged in
            Toast.makeText(this, "Please log in to access your profile", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize ViewModels with current user
        transactionViewModel.initializeUserData(currentUserId!!)
        achievementViewModel.initializeUserData(currentUserId!!)
        
        // Load user profile
        loadUserProfile()

        // Profile picture change button
        findViewById<ImageButton>(R.id.change_profile_pic_btn).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }

        // Save username button
        saveUsernameBtn.setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            updateUsername(newUsername)
        }

        // Save password button
        savePasswordBtn.setOnClickListener {
            val currentPassword = currentPasswordInput.text.toString()
            val newPassword = newPasswordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            
            updatePassword(currentPassword, newPassword, confirmPassword)
        }

        // Back button
        findViewById<Button>(R.id.back_to_main_btn).setOnClickListener {
            finish()
        }

        // Logout button
        logoutBtn.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Set version text
        versionText.text = "App Version 1.0.0"
        
        // Observe ViewModels to update UI
        observeViewModels()

        setupInputFieldFocusAnimations()
    }
    
    private fun loadUserProfile() {
        val userId = currentUserId ?: return
        
        lifecycleScope.launch {
            try {
                val userDoc = FirebaseManager.getUserProfile(userId)
                val userData = userDoc.data
                
                if (userData != null) {
                    val username = userData["username"] as? String ?: ""
                    profileImageUrl = userData["profileImageUrl"] as? String
                    
                    runOnUiThread {
                        usernameInput.setText(username)
                        
                        // Load profile picture if available
                        if (!profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@ProfileActivity)
                                .load(profileImageUrl)
                                .circleCrop()
                                .into(profilePic)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading user profile", e)
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun observeViewModels() {
        // Observe transactions for stats
        transactionViewModel.transactions.observe(this) { transactions ->
            updateUserStatistics(transactions)
        }
        
        // Observe earned achievements
        achievementViewModel.earnedAchievements.observe(this) { achievements ->
            updateAwardsText(achievements)
        }
    }

    private fun updateUsername(newUsername: String) {
        if (newUsername.isEmpty()) {
            usernameInput.error = "Please enter a username"
            return
        }
        
        val userId = currentUserId ?: return
        
        lifecycleScope.launch {
            try {
                saveUsernameBtn.isEnabled = false
                FirebaseManager.updateUsername(userId, newUsername)
                
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Username updated!", Toast.LENGTH_SHORT).show()
                    saveUsernameBtn.isEnabled = true
                    
                    // Check for profile complete achievement
                    achievementViewModel.checkForAchievements()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Failed to update username: ${e.message}", Toast.LENGTH_SHORT).show()
                    saveUsernameBtn.isEnabled = true
                }
            }
        }
    }

    private fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
            when {
                currentPassword.isEmpty() -> {
                    currentPasswordInput.error = "Please enter your current password"
                    currentPasswordInput.requestFocus()
                }
                newPassword.isEmpty() -> {
                    newPasswordInput.error = "Please enter a new password"
                    newPasswordInput.requestFocus()
                }
                confirmPassword.isEmpty() -> {
                    confirmPasswordInput.error = "Please confirm your new password"
                    confirmPasswordInput.requestFocus()
                }
                newPassword != confirmPassword -> {
                    confirmPasswordInput.error = "Passwords do not match"
                    confirmPasswordInput.requestFocus()
                }
                else -> {
                lifecycleScope.launch {
                    try {
                        savePasswordBtn.isEnabled = false
                        FirebaseManager.updateUserPassword(currentPassword, newPassword)
                        
                        runOnUiThread {
                            Toast.makeText(this@ProfileActivity, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                    clearPasswordFields()
                            savePasswordBtn.isEnabled = true
            }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ProfileActivity, "Failed to update password: ${e.message}", Toast.LENGTH_SHORT).show()
                            currentPasswordInput.error = "Current password may be incorrect"
                            savePasswordBtn.isEnabled = true
                        }
                    }
                }
            }
        }
    }
    
    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseManager.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearPasswordFields() {
        currentPasswordInput.text?.clear()
        newPasswordInput.text?.clear()
        confirmPasswordInput.text?.clear()
    }

    private fun updateUserStatistics(transactions: List<FirebaseTransaction>) {
        if (transactions.isEmpty()) {
            statsText.text = "📊 No transactions yet\nStart tracking your expenses and income!"
            return
        }
        
        val totalExpenses = transactions.filter { it.isExpense }.sumOf { it.amount }
        val totalIncome = transactions.filter { !it.isExpense }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses
                val transactionCount = transactions.size
        val expenseCount = transactions.count { it.isExpense }
        val incomeCount = transactions.count { !it.isExpense }
        val categoriesUsed = transactions.map { it.category }.toSet().size
        
        val consecutiveDays = calculateConsecutiveDays(transactions)
        
        val statsString = """
            📈 ACTIVITY SUMMARY
            
            💰 Financial Overview:
            • Total Expenses: R${"%.2f".format(totalExpenses)}
            • Total Income: R${"%.2f".format(totalIncome)}
            • Current Balance: R${"%.2f".format(balance)}
            
            📝 Transaction Stats:
            • Total Transactions: $transactionCount
            • Expenses: $expenseCount | Income: $incomeCount
            • Categories Used: $categoriesUsed
            
            🔥 Streak: $consecutiveDays consecutive days
        """.trimIndent()
        
        statsText.text = statsString
    }

    private fun updateAwardsText(achievements: List<com.example.budgettracker.data.Achievement>) {
        if (achievements.isEmpty()) {
            awardsText.text = "🏆 No awards yet\nKeep tracking to earn achievements!"
            return
        }
        
        val earnedCount = achievements.size
        val totalCount = achievementViewModel.allAchievements.value?.size ?: 0
        val progressPercentage = if (totalCount > 0) (earnedCount * 100 / totalCount) else 0
        
        val awardsString = """
            🏆 ACHIEVEMENTS
            
            Progress: $earnedCount/$totalCount ($progressPercentage%)
            
            🎯 Recent Awards:
            ${achievements.take(3).joinToString("\n") { "• ${it.name}" }}
        """.trimIndent()
        
        awardsText.text = awardsString
        
        // Make the text clickable to show all achievements
        if (achievements.size > 3) {
            awardsText.setOnClickListener {
                showAllAchievementsDialog(achievements)
            }
            awardsText.text = "$awardsString\n\n💡 Tap to view all ${achievements.size} achievements"
        } else {
            awardsText.setOnClickListener(null)
        }
    }
    
    private fun showAllAchievementsDialog(achievements: List<com.example.budgettracker.data.Achievement>) {
        val achievementsList = achievements.joinToString("\n\n") { achievement ->
            "🏆 ${achievement.name}\n${achievement.description}"
        }
        
        AlertDialog.Builder(this)
            .setTitle("All Your Achievements")
            .setMessage(achievementsList)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun calculateConsecutiveDays(transactions: List<FirebaseTransaction>): Int {
        if (transactions.isEmpty()) return 0
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val uniqueDates = transactions.map { it.date }.toSet()
        
        // Check for consecutive days
        var maxConsecutive = 1
        var current = 1
        
        val sortedDates = uniqueDates.map { dateFormat.parse(it)?.time ?: 0 }.sorted()
            
        for (i in 1 until sortedDates.size) {
            val diff = (sortedDates[i] - sortedDates[i-1]) / (1000 * 60 * 60 * 24)
            
            if (diff == 1L) {
                    current++
                    maxConsecutive = maxOf(maxConsecutive, current)
                } else {
                    current = 1
            }
        }
        
        return maxConsecutive
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupInputFieldFocusAnimations() {
        val fields = listOf(
            findViewById<TextInputEditText>(R.id.username_input),
            findViewById<TextInputEditText>(R.id.current_password_input),
            findViewById<TextInputEditText>(R.id.new_password_input),
            findViewById<TextInputEditText>(R.id.confirm_password_input)
        )
        fields.forEach { field ->
            field.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.04f else 1f).scaleY(if (hasFocus) 1.04f else 1f).setDuration(180).start()
            }
        }
    }
}
