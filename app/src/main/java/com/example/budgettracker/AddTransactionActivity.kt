package com.example.budgettracker

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.budgettracker.data.Categories
import com.example.budgettracker.firebase.models.FirebaseTransaction
import com.example.budgettracker.util.ImageHelper
import com.example.budgettracker.util.ValidationHelper
import com.example.budgettracker.viewmodel.TransactionViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.budgettracker.firebase.FirebaseManager
import com.google.android.material.snackbar.Snackbar

class AddTransactionActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private val transactionViewModel: TransactionViewModel by viewModels()
    
    private lateinit var categorySpinner: Spinner
    private lateinit var labelInput: TextInputEditText
    private lateinit var labelLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountLayout: TextInputLayout
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var dateInput: TextInputEditText
    private lateinit var transactionTypeRadioGroup: RadioGroup
    private lateinit var expenseRadioButton: RadioButton
    private lateinit var incomeRadioButton: RadioButton
    private lateinit var addTransactionBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        // Initialize UI components
        initializeUI()
        
        // Initialize ViewModel with current user
        initializeViewModel()
        
        // Set up UI behavior
        setupUIBehavior()
        setupInputFieldFocusAnimations()
    }
    
    private fun initializeUI() {
        labelInput = findViewById(R.id.labelInput)
        labelLayout = findViewById(R.id.labelLayout)
        amountInput = findViewById(R.id.amountInput)
        amountLayout = findViewById(R.id.amountLayout)
        descriptionInput = findViewById(R.id.descriptionInput)
        dateInput = findViewById(R.id.dateInput)
        addTransactionBtn = findViewById(R.id.addTransactionBtn)
        
        // Get the category spinner and transaction type radio group
        categorySpinner = findViewById(R.id.categorySpinner)
        transactionTypeRadioGroup = findViewById(R.id.transactionTypeRadioGroup)
        expenseRadioButton = findViewById(R.id.expenseRadioButton)
        incomeRadioButton = findViewById(R.id.incomeRadioButton)
        
        // Default to expense
        expenseRadioButton.isChecked = true
        setupCategorySpinner()
        
        // Set today's date by default
        dateInput.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time))
    }
    
    private fun initializeViewModel() {
        // Check if user is logged in
        val currentUser = FirebaseManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add a transaction", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // Initialize with user ID
        transactionViewModel.initializeUserData(currentUser.uid)
        
        // Observe transaction result
        transactionViewModel.transactionResult.observe(this) { result ->
            result.fold(
                onSuccess = {
                    Snackbar.make(findViewById(android.R.id.content), "Transaction saved successfully!", Snackbar.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                },
                onFailure = { error ->
                    Snackbar.make(findViewById(android.R.id.content), "Error: ${error.message}", Snackbar.LENGTH_LONG).show()
                    addTransactionBtn.isEnabled = true
                }
            )
        }
        
        transactionViewModel.loading.observe(this) { isLoading ->
            addTransactionBtn.isEnabled = !isLoading
            if (isLoading) {
                Toast.makeText(this, "Saving transaction...", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupUIBehavior() {
        // Set up category spinner behavior
        transactionTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.expenseRadioButton -> setupCategorySpinner(true)
                R.id.incomeRadioButton -> setupCategorySpinner(false)
            }
        }

        // Handle close button
        findViewById<ImageButton>(R.id.closeBtn).setOnClickListener {
            finish()
        }

        // Add text watchers for validation
        labelInput.addTextChangedListener {
            if (!it.isNullOrEmpty()) labelLayout.error = null
        }

        amountInput.addTextChangedListener {
            if (!it.isNullOrEmpty()) amountLayout.error = null
        }

        // Set up date picker
        dateInput.setOnClickListener {
            showDatePicker()
        }

        // Set up time pickers
        findViewById<TextInputEditText>(R.id.startTimeInput).setOnClickListener {
            showTimePicker(it as TextInputEditText)
        }

        findViewById<TextInputEditText>(R.id.endTimeInput).setOnClickListener {
            showTimePicker(it as TextInputEditText)
        }

        // Set up receipt image picker
        findViewById<ImageView>(R.id.receiptImage).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Set up add transaction button
        addTransactionBtn.setOnClickListener {
            addTransactionBtn.isEnabled = false
            validateAndSaveTransaction(
                categorySpinner.selectedItem.toString(),
                amountInput.text.toString(),
                descriptionInput.text.toString(),
                dateInput.text.toString(),
                findViewById<TextInputEditText>(R.id.startTimeInput).text.toString(),
                findViewById<TextInputEditText>(R.id.endTimeInput).text.toString(),
                expenseRadioButton.isChecked
            )
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)
                dateInput.setText(formattedDate)
            }, year, month, day)

        datePickerDialog.show()
    }
    
    private fun showTimePicker(timeInput: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            timeInput.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
        }, hour, minute, true)
        timePickerDialog.show()
    }
    
    private fun setupCategorySpinner(showExpenses: Boolean = true) {
        val categories = if (showExpenses) {
            Categories.getExpenseCategories.map { it.name }
        } else {
            Categories.getIncomeCategories.map { it.name }
        }
        
        val adapter = ArrayAdapter(
            this, 
            android.R.layout.simple_spinner_item, 
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }
    
    private fun validateAndSaveTransaction(
        category: String,
        amountString: String,
        description: String,
        date: String,
        startTime: String,
        endTime: String,
        isExpense: Boolean
    ) {
        // Check if user is logged in
        val currentUser = FirebaseManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add a transaction", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Validate the amount
        val amountValidation = ValidationHelper.validateAmount(amountString)
        if (!amountValidation.first) {
            amountLayout.error = amountValidation.second
            addTransactionBtn.isEnabled = true
            return
        }
        
        // Validate the date
        val dateValidation = ValidationHelper.validateDate(date)
        if (!dateValidation.first) {
            Toast.makeText(this, dateValidation.second, Toast.LENGTH_SHORT).show()
            addTransactionBtn.isEnabled = true
            return
        }
        
        val amount = amountString.toDouble()
        
        // Save using ViewModel
        transactionViewModel.addTransaction(
            context = this,
            amount = amount,
            description = description,
            category = category,
            isExpense = isExpense,
            imageUri = selectedImageUri
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val originalUri = data.data!!

            // Save image to app storage with compression
            val savedPath = ImageHelper.saveImageToInternalStorage(this, originalUri)
            if (savedPath != null) {
                selectedImageUri = Uri.fromFile(java.io.File(savedPath))
                val receiptImage = findViewById<ImageView>(R.id.receiptImage)
                receiptImage.setImageURI(selectedImageUri)
                
                // Show a preview button
                val previewButton = findViewById<Button>(R.id.previewButton)
                previewButton.visibility = View.VISIBLE
                previewButton.setOnClickListener {
                    val intent = Intent(this, ImagePreviewActivity::class.java)
                    intent.putExtra("imageUri", selectedImageUri.toString())
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupInputFieldFocusAnimations() {
        val fields = listOf(
            findViewById<TextInputEditText>(R.id.amountInput),
            findViewById<TextInputEditText>(R.id.dateInput),
            findViewById<TextInputEditText>(R.id.startTimeInput),
            findViewById<TextInputEditText>(R.id.endTimeInput),
            findViewById<TextInputEditText>(R.id.descriptionInput)
        )
        fields.forEach { field ->
            field.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.04f else 1f).scaleY(if (hasFocus) 1.04f else 1f).setDuration(180).start()
            }
        }
    }
}
