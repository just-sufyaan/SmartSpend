package com.example.budgettracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.adapters.CategorySpendingAdapter
import com.example.budgettracker.data.CategorySpending
import com.example.budgettracker.firebase.FirebaseManager
import com.example.budgettracker.viewmodel.BudgetViewModel
import com.example.budgettracker.viewmodel.TransactionViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BudgetFragment : Fragment() {

    private lateinit var budgetStartDateInput: TextInputEditText
    private lateinit var budgetEndDateInput: TextInputEditText
    private lateinit var categoryStartDateInput: TextInputEditText
    private lateinit var categoryEndDateInput: TextInputEditText
    private lateinit var categorySpendingList: RecyclerView
    private lateinit var categoryMessageText: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val calendar = Calendar.getInstance()
    private lateinit var categoryFilterSpinner: Spinner
    private var allCategories: List<String> = emptyList()

    private val budgetViewModel: BudgetViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_budget_fragment, container, false)

        val minBudgetInput = view.findViewById<TextInputEditText>(R.id.min_budget_input)
        val maxBudgetInput = view.findViewById<TextInputEditText>(R.id.max_budget_input)
        val saveButton = view.findViewById<Button>(R.id.save_budget)
        val pickMonthButton = view.findViewById<Button>(R.id.pick_month_button)
        val monthYearDisplay = view.findViewById<TextView>(R.id.month_year_display)
        
        // Initialize budget date inputs
        budgetStartDateInput = view.findViewById(R.id.budget_start_date)
        budgetEndDateInput = view.findViewById(R.id.budget_end_date)
        
        // Initialize category date inputs
        categoryStartDateInput = view.findViewById(R.id.category_start_date)
        categoryEndDateInput = view.findViewById(R.id.category_end_date)
        
        categorySpendingList = view.findViewById(R.id.category_spending_list)
        categoryMessageText = view.findViewById(R.id.category_message_text)
        categoryFilterSpinner = view.findViewById(R.id.category_filter_spinner)

        // Observe budget data and load existing values
        budgetViewModel.budget.observe(viewLifecycleOwner) { budget ->
            if (budget != null) {
                // Load existing budget values into input fields
                minBudgetInput.setText(String.format("%.2f", budget.minBudget))
                maxBudgetInput.setText(String.format("%.2f", budget.maxBudget))
                
                val startDate = budgetStartDateInput.text.toString()
                val endDate = budgetEndDateInput.text.toString()
                if (startDate.isNotBlank() && endDate.isNotBlank()) {
                    getMonthlyExpenses()
                }
            } else {
                // Clear input fields if no budget exists
                minBudgetInput.text?.clear()
                maxBudgetInput.text?.clear()
                val statusTextView = view.findViewById<TextView>(R.id.statusTextView)
                statusTextView.text = ""
                statusTextView.visibility = View.GONE
            }
        }

        // Setup category filter spinner
        lifecycleScope.launch {
            val transactions = transactionViewModel.transactions.value ?: emptyList()
            // Only get expense categories
            allCategories = transactions
                .filter { it.isExpense }
                .map { it.category }
                .distinct()
            val spinnerItems = listOf("All Categories") + allCategories
            categoryFilterSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, spinnerItems)

            categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    fetchAndDisplayCategorySpending()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        categorySpendingList.layoutManager = LinearLayoutManager(requireContext())
        showCategoryMessage("Select a date range to view category spending")

        pickMonthButton.setOnClickListener {
            showMonthPicker(monthYearDisplay)
        }

        setupDatePicker(budgetStartDateInput, true)
        setupDatePicker(budgetEndDateInput, true)
        setupDatePicker(categoryStartDateInput, false)
        setupDatePicker(categoryEndDateInput, false)

        saveButton.setOnClickListener {
            val minText = minBudgetInput.text.toString()
            val maxText = maxBudgetInput.text.toString()

            if (minText.isBlank() || maxText.isBlank()) {
                Toast.makeText(requireContext(), "Please enter both values.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minBudget = minText.toDoubleOrNull()
            val maxBudget = maxText.toDoubleOrNull()

            if (minBudget == null || maxBudget == null || minBudget >= maxBudget) {
                Toast.makeText(requireContext(), "Enter valid amounts. Min must be less than Max.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            budgetViewModel.saveBudget(minBudget, maxBudget)
            Toast.makeText(requireContext(), "Budget saved!", Toast.LENGTH_SHORT).show()
            // Do not call getMonthlyExpenses() here; rely on observer
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            budgetViewModel.initializeUserData(currentUserId)
            transactionViewModel.initializeUserData(currentUserId)
            
            // Update category filter spinner
            updateCategorySpinner()
        }
    }

    private fun updateCategorySpinner() {
        transactionViewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            // Only get expense categories
            allCategories = transactions
                .filter { it.isExpense }
                .map { it.category }
                .distinct()
            val spinnerItems = listOf("All Categories") + allCategories
            categoryFilterSpinner.adapter = ArrayAdapter(
                requireContext(), 
                android.R.layout.simple_spinner_dropdown_item, 
                spinnerItems
            )

            categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    fetchAndDisplayCategorySpending()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun showMonthPicker(display: TextView) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, _ ->
                val selectedDate = "Selected: ${getMonthName(selectedMonth)} $selectedYear"
                display.text = selectedDate

                // Set start and end date inputs to first and last day of selected month
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, selectedYear)
                cal.set(Calendar.MONTH, selectedMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val firstDay = dateFormat.format(cal.time)
                budgetStartDateInput.setText(firstDay)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val lastDay = dateFormat.format(cal.time)
                budgetEndDateInput.setText(lastDay)
                // Only update budget status
                getMonthlyExpenses()
            },
            year, month, calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.updateDate(year, month, 1)
        datePickerDialog.show()
    }

    private fun getMonthName(month: Int): String {
        return dateFormat.calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) ?: ""
    }

    private fun setupDatePicker(input: TextInputEditText, isBudgetDate: Boolean) {
        // Prevent keyboard from showing up
        input.isFocusable = false
        input.isFocusableInTouchMode = false
        input.isClickable = true
        
        input.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    input.setText(date)
                    if (isBudgetDate) {
                        getMonthlyExpenses()
                    } else {
                        fetchAndDisplayCategorySpending()
                    }
                },
                year, month, day
            ).show()
        }
    }

    private fun getMonthlyExpenses() {
        val startDate = budgetStartDateInput.text.toString()
        val endDate = budgetEndDateInput.text.toString()

        if (startDate.isBlank() || endDate.isBlank()) {
            return
        }

        lifecycleScope.launch {
            val transactions = transactionViewModel.transactions.value ?: emptyList()
            val start = try { dateFormat.parse(startDate) } catch (e: Exception) { null }
            val end = try { dateFormat.parse(endDate) } catch (e: Exception) { null }
            if (start == null || end == null) return@launch
            val expenses = transactions.filter {
                it.isExpense && try {
                    val tDate = dateFormat.parse(it.date)
                    tDate != null && !tDate.before(start) && !tDate.after(end)
                } catch (e: Exception) { false }
            }
            val totalExpenses = expenses.sumOf { it.amount }
            compareBudgetAndSpending(totalExpenses)
        }
    }

    private fun compareBudgetAndSpending(totalExpenses: Double) {
        val budget = budgetViewModel.budget.value
        if (budget != null) {
            val statusTextView = view?.findViewById<TextView>(R.id.statusTextView)
            statusTextView?.visibility = View.VISIBLE
            
            val percentOfBudget = (totalExpenses / budget.maxBudget) * 100
            val (status, color) = when {
                totalExpenses < budget.minBudget -> {
                    val underAmount = budget.minBudget - totalExpenses
                    "Under Budget: R${String.format("%.2f", totalExpenses)} / Min: R${String.format("%.2f", budget.minBudget)}\nNeed R${String.format("%.2f", underAmount)} more to reach minimum" to android.R.color.holo_blue_dark
                }
                totalExpenses > budget.maxBudget -> {
                    val overAmount = totalExpenses - budget.maxBudget
                    "Over Budget: R${String.format("%.2f", totalExpenses)} / Max: R${String.format("%.2f", budget.maxBudget)}\nExceeded by R${String.format("%.2f", overAmount)} (${String.format("%.0f", percentOfBudget)}%)" to android.R.color.holo_red_dark
                }
                else -> {
                    "Within Budget: R${String.format("%.2f", totalExpenses)}\nMin: R${String.format("%.2f", budget.minBudget)} - Max: R${String.format("%.2f", budget.maxBudget)} (${String.format("%.0f", percentOfBudget)}%)" to android.R.color.holo_green_dark
                }
            }

            statusTextView?.text = status
            statusTextView?.setTextColor(resources.getColor(color, null))
        }
    }

    private fun fetchAndDisplayCategorySpending() {
        val start = categoryStartDateInput.text.toString()
        val end = categoryEndDateInput.text.toString()
        
        if (start.isBlank() || end.isBlank()) {
            showCategoryMessage("Select start and end dates")
            return
        }
        
        val selectedCategory = categoryFilterSpinner.selectedItem?.toString()
        val transactions = transactionViewModel.transactions.value ?: emptyList()
        
        // Parse dates for comparison
        val startDate = try { dateFormat.parse(start) } catch (e: Exception) { null }
        val endDate = try { dateFormat.parse(end) } catch (e: Exception) { null }
        
        if (startDate == null || endDate == null) {
            showCategoryMessage("Invalid date format")
            return
        }
        
        val categoryData = if (selectedCategory == "All Categories" || selectedCategory.isNullOrBlank()) {
            transactions
                .filter { transaction -> 
                    transaction.isExpense && try {
                        val transDate = dateFormat.parse(transaction.date)
                        transDate != null && !transDate.before(startDate) && !transDate.after(endDate)
                    } catch (e: Exception) { 
                        false 
                    }
                }
                .groupBy { it.category }
                .map { (category, transactions) ->
                    CategorySpending(
                        category = category,
                        total = transactions.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.total }
        } else {
            transactions
                .filter { transaction -> 
                    transaction.isExpense && 
                    transaction.category == selectedCategory && 
                    try {
                        val transDate = dateFormat.parse(transaction.date)
                        transDate != null && !transDate.before(startDate) && !transDate.after(endDate)
                    } catch (e: Exception) { 
                        false 
                    }
                }
                .groupBy { it.category }
                .map { (category, transactions) ->
                    CategorySpending(
                        category = category,
                        total = transactions.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.total }
        }

        if (categoryData.isEmpty()) {
            showCategoryMessage("No data found for the selected date range and category.")
            categorySpendingList.adapter = null
        } else {
            categorySpendingList.adapter = CategorySpendingAdapter(categoryData)
            categoryMessageText.visibility = View.GONE
        }
    }

    private fun showCategoryMessage(message: String) {
        categoryMessageText.text = message
        categoryMessageText.visibility = View.VISIBLE
    }
}
