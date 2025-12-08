package com.example.budgettracker.firebase

import android.net.Uri
import android.util.Log
import com.example.budgettracker.data.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    internal val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Collections
    private const val USERS_COLLECTION = "users"
    private const val TRANSACTIONS_COLLECTION = "transactions"
    private const val BUDGETS_COLLECTION = "budgets"
    private const val ACHIEVEMENTS_COLLECTION = "achievements"

    // User operations
    suspend fun register(email: String, password: String) = 
        auth.createUserWithEmailAndPassword(email, password).await()

    suspend fun signIn(email: String, password: String) = 
        auth.signInWithEmailAndPassword(email, password).await()

    fun signOut() = auth.signOut()

    fun getCurrentUser() = auth.currentUser

    // User data operations
    suspend fun createUserProfile(user: User) {
        val userId = auth.currentUser?.uid ?: throw Exception("No authenticated user")
        val userData = mapOf(
            "username" to user.username,
            "email" to user.email,
            "balance" to user.balance
        )
        db.collection(USERS_COLLECTION).document(userId).set(userData).await()
    }

    suspend fun getUserProfile(userId: String) =
        db.collection(USERS_COLLECTION).document(userId).get().await()

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) =
        db.collection(USERS_COLLECTION).document(userId).update(updates).await()
        
    suspend fun updateUsername(userId: String, newUsername: String) {
        try {
            val updates = mapOf("username" to newUsername)
            db.collection(USERS_COLLECTION).document(userId).update(updates).await()
            
            // Also update displayName in Firebase Auth if user is authenticated
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build()
                currentUser.updateProfile(profileUpdates).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating username", e)
            throw e
        }
    }
    
    suspend fun updateUserPassword(currentPassword: String, newPassword: String) {
        try {
            val user = auth.currentUser ?: throw Exception("No authenticated user")
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            
            // Re-authenticate user
            user.reauthenticate(credential).await()
            
            // Change password
            user.updatePassword(newPassword).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating password", e)
            throw e
        }
    }
    
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        try {
            Log.d("FirebaseManager", "Starting profile image upload for user: $userId")
            
            // First, ensure the user document exists
            val userDoc = db.collection(USERS_COLLECTION).document(userId)
            val userSnapshot = userDoc.get().await()
            
            Log.d("FirebaseManager", "User document exists: ${userSnapshot.exists()}")
            
            if (!userSnapshot.exists()) {
                Log.d("FirebaseManager", "Creating user document for: $userId")
                // Create user document if it doesn't exist
                val userData = mapOf(
                    "username" to "",
                    "email" to "",
                    "balance" to 0.0,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                userDoc.set(userData).await()
                Log.d("FirebaseManager", "User document created successfully")
            }
            
            // Upload image to Firebase Storage
            Log.d("FirebaseManager", "Uploading image to Firebase Storage")
            val storageRef = storage.reference.child("profile_images/$userId.jpg")
            
            // Check if the image URI is valid
            if (imageUri.toString().isEmpty()) {
                throw Exception("Invalid image URI")
            }
            
            val uploadTask = storageRef.putFile(imageUri).await()
            Log.d("FirebaseManager", "Image upload completed, bytes transferred: ${uploadTask.bytesTransferred}")
            
            // Get the download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d("FirebaseManager", "Download URL obtained: $downloadUrl")
            
            // Update user profile with image URL
            val updates = mapOf("profileImageUrl" to downloadUrl)
            userDoc.update(updates).await()
            Log.d("FirebaseManager", "User profile updated with image URL")
            
            // Update Firebase Auth profile if user is authenticated
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(downloadUrl))
                    .build()
                currentUser.updateProfile(profileUpdates).await()
                Log.d("FirebaseManager", "Firebase Auth profile updated")
            }
            
            return downloadUrl
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error uploading profile image", e)
            Log.e("FirebaseManager", "Error details: ${e.message}")
            Log.e("FirebaseManager", "Error cause: ${e.cause}")
            throw Exception("Failed to upload image: ${e.message}")
        }
    }

    // Transaction operations
    suspend fun saveTransaction(userId: String, transaction: Map<String, Any>) {
        try {
            // Log for debugging
            Log.d("FirebaseManager", "Saving transaction for user: $userId")
            
            // Ensure the user ID is set in the transaction
            val transactionWithUserId = transaction.toMutableMap().apply {
                this["userId"] = userId
            }
            
            // Save to the correct path
            val result = db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TRANSACTIONS_COLLECTION)
                .add(transactionWithUserId)
                .await()
                
            Log.d("FirebaseManager", "Transaction saved with ID: ${result.id}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving transaction", e)
            throw e
        }
    }

    suspend fun getTransactions(userId: String) =
        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_COLLECTION)
            .get()
            .await()

    // Budget operations
    suspend fun saveBudget(userId: String, budget: Map<String, Any>) =
        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(BUDGETS_COLLECTION)
            .document("current")
            .set(budget)
            .await()

    suspend fun getBudget(userId: String) =
        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(BUDGETS_COLLECTION)
            .document("current")
            .get()
            .await()

    // Achievement operations
    suspend fun saveAchievement(userId: String, achievement: Map<String, Any>) =
        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(ACHIEVEMENTS_COLLECTION)
            .add(achievement)
            .await()

    suspend fun getAchievements(userId: String) =
        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(ACHIEVEMENTS_COLLECTION)
            .get()
            .await()
            
    suspend fun updateAchievement(userId: String, achievementId: String, updates: Map<String, Any>) =
        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(ACHIEVEMENTS_COLLECTION)
            .document(achievementId)
            .update(updates)
            .await()
} 