package com.example.budgettracker.util

import android.text.TextUtils
import android.util.Patterns
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object ValidationHelper {

    /**
     * Check if the email is valid
     * @param email email to validate
     * @return pair of (isValid, errorMessage)
     */
    fun validateEmail(email: String): Pair<Boolean, String?> {
        if (TextUtils.isEmpty(email)) {
            return Pair(false, "Email cannot be empty")
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Pair(false, "Invalid email format")
        }
        
        return Pair(true, null)
    }
    
    /**
     * Check if the password is valid (length, complexity)
     * @param password password to validate
     * @return pair of (isValid, errorMessage)
     */
    fun validatePassword(password: String): Pair<Boolean, String?> {
        if (TextUtils.isEmpty(password)) {
            return Pair(false, "Password cannot be empty")
        }
        
        if (password.length < 6) {
            return Pair(false, "Password must be at least 6 characters")
        }
        
        // Check for at least one digit
        if (!password.any { it.isDigit() }) {
            return Pair(false, "Password must contain at least one number")
        }
        
        return Pair(true, null)
    }
    
    /**
     * Check if the amount is valid (non-empty, positive number)
     * @param amountString the amount to validate
     * @return pair of (isValid, errorMessage)
     */
    fun validateAmount(amountString: String): Pair<Boolean, String?> {
        if (TextUtils.isEmpty(amountString)) {
            return Pair(false, "Amount cannot be empty")
        }
        
        val amount = amountString.toDoubleOrNull()
        if (amount == null) {
            return Pair(false, "Invalid amount format")
        }
        
        if (amount <= 0) {
            return Pair(false, "Amount must be greater than zero")
        }
        
        return Pair(true, null)
    }
    
    /**
     * Check if the date is valid (non-empty, correct format)
     * @param dateString the date to validate
     * @param format the expected date format
     * @return pair of (isValid, errorMessage)
     */
    fun validateDate(dateString: String, format: String = "yyyy-MM-dd"): Pair<Boolean, String?> {
        if (TextUtils.isEmpty(dateString)) {
            return Pair(false, "Date cannot be empty")
        }
        
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        dateFormat.isLenient = false
        
        try {
            dateFormat.parse(dateString)
            return Pair(true, null)
        } catch (e: ParseException) {
            return Pair(false, "Invalid date format. Expected: $format")
        }
    }
    
    /**
     * Check if required field is not empty
     * @param value the value to validate
     * @param fieldName name of the field for error message
     * @return pair of (isValid, errorMessage)
     */
    fun validateRequired(value: String, fieldName: String): Pair<Boolean, String?> {
        return if (TextUtils.isEmpty(value)) {
            Pair(false, "$fieldName is required")
        } else {
            Pair(true, null)
        }
    }
} 