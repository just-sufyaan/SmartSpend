package com.example.budgettracker

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.budgettracker.data.CategorySpending
import com.example.budgettracker.firebase.models.FirebaseTransaction
import com.example.budgettracker.viewmodel.BudgetViewModel
import com.example.budgettracker.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import android.widget.AdapterView
import android.widget.ArrayAdapter

class SpendingAnalyticsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var noDataText: TextView
    private lateinit var budgetStatusText: TextView
    private lateinit var timeframeSpinner: Spinner
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_spending_analytics, container, false)

        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
        startDateInput = view.findViewById(R.id.startDateInput)
        endDateInput = view.findViewById(R.id.endDateInput)
        noDataText = view.findViewById(R.id.noDataText)
        budgetStatusText = view.findViewById(R.id.budgetStatusText)
        timeframeSpinner = view.findViewById(R.id.timeframeSpinner)
        
        val updateButton = view.findViewById<Button>(R.id.updateChartsButton)
        val currentMonthButton = view.findViewById<Button>(R.id.currentMonthButton)

        setupDatePicker(startDateInput)
        setupDatePicker(endDateInput)
        
        setDefaultDates()
        
        updateButton.setOnClickListener {
            updateCharts()
        }
        
        currentMonthButton.setOnClickListener {
            setDefaultDates()
            updateCharts()
        }
        
        setupEmptyCharts()
        
        // Observe budget data
        budgetViewModel.budget.observe(viewLifecycleOwner) { budget ->
            // When budget changes, update the charts if we have data
            if (startDateInput.text?.isNotEmpty() == true && endDateInput.text?.isNotEmpty() == true) {
                updateCharts()
            }
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            transactionViewModel.initializeUserData(currentUserId)
            budgetViewModel.initializeUserData(currentUserId)
        }
        
        setupTimeframeSpinner()
        
        // Load the data
        loadSpendingData()
    }

    private fun setupDatePicker(input: TextInputEditText) {
        // Prevent keyboard from showing up
        input.isFocusable = false
        input.isFocusableInTouchMode = false
        input.isClickable = true
        
        input.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    input.setText(date)
                },
                year, month, day
            ).show()
        }
    }

    private fun setDefaultDates() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        
        // Set start date to first day of current month
        calendar.set(year, month, 1)
        startDateInput.setText(dateFormat.format(calendar.time))
        
        // Set end date to last day of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDateInput.setText(dateFormat.format(calendar.time))
    }

    private fun setupEmptyCharts() {
        // Setup empty pie chart
        pieChart.apply {
            description.isEnabled = false
            setDrawEntryLabels(false)
            legend.isEnabled = true
            legend.textSize = 12f
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
        }

        // Setup empty bar chart
        barChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 12f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 12f
            }
            
            axisRight.isEnabled = false
        }
    }

    private fun setupTimeframeSpinner() {
        val timeframes = arrayOf("This Month", "Last Month", "Last 3 Months", "Last 6 Months", "This Year", "All Time")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeframes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeframeSpinner.adapter = adapter
        
        timeframeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadSpendingData()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getDateRangeForTimeframe(timeframe: String): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)
        
        when (timeframe) {
            "This Month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            "Last Month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = dateFormat.format(calendar.time)
                
                // Get last day of previous month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val lastDate = dateFormat.format(calendar.time)
                
                return Pair(startDate, lastDate)
            }
            "Last 3 Months" -> {
                calendar.add(Calendar.MONTH, -3)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            "Last 6 Months" -> {
                calendar.add(Calendar.MONTH, -6)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            "This Year" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            else -> { // "All Time"
                // Return a date from very far past
                val startDate = "2000-01-01"
                return Pair(startDate, endDate)
            }
        }
    }

    private fun loadSpendingData() {
        val selectedTimeframe = timeframeSpinner.selectedItem.toString()
        val dateRange = getDateRangeForTimeframe(selectedTimeframe)
        startDateInput.setText(dateRange.first)
        endDateInput.setText(dateRange.second)

        transactionViewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            // Filter transactions by date range
            val startDate = try { dateFormat.parse(dateRange.first) } catch (e: Exception) { null }
            val endDate = try { dateFormat.parse(dateRange.second) } catch (e: Exception) { null }
            
            val filteredTransactions = if (startDate != null && endDate != null) {
                transactions.filter { transaction ->
                    val transDate = try {
                        dateFormat.parse(transaction.date)
                    } catch (e: Exception) {
                        null
                    }
                    
                    transaction.isExpense && transDate != null && 
                    !transDate.before(startDate) && !transDate.after(endDate)
                }
            } else {
                transactions.filter { it.isExpense }
            }
            
            // Process the data
            val categoryData = filteredTransactions
                .groupBy { it.category }
                .map { (category, categoryTrans) ->
                    CategorySpending(
                        category = category,
                        total = categoryTrans.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.total }
            
            if (categoryData.isEmpty()) {
                noDataText.visibility = View.VISIBLE
                pieChart.visibility = View.GONE
                barChart.visibility = View.GONE
                budgetStatusText.visibility = View.GONE
                return@observe
            }
            
            noDataText.visibility = View.GONE
            pieChart.visibility = View.VISIBLE
            barChart.visibility = View.VISIBLE
            
            // Calculate total spending
            val totalSpending = categoryData.sumOf { it.total }
            
            // Update budget status
            updateBudgetStatus(totalSpending)
            
            updatePieChart(categoryData)
            updateBarChart(categoryData)
        }
    }

    private fun updateCharts() {
        val startDate = startDateInput.text.toString()
        val endDate = endDateInput.text.toString()
        
        if (startDate.isEmpty() || endDate.isEmpty()) {
            return
        }

        transactionViewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            val categoryData = transactions
                .filter { it.isExpense }
                .groupBy { it.category }
                .map { (category, transactions) ->
                    CategorySpending(
                        category = category,
                        total = transactions.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.total }
            
            if (categoryData.isEmpty()) {
                noDataText.visibility = View.VISIBLE
                pieChart.visibility = View.GONE
                barChart.visibility = View.GONE
                budgetStatusText.visibility = View.GONE
                return@observe
            }
            
            noDataText.visibility = View.GONE
            pieChart.visibility = View.VISIBLE
            barChart.visibility = View.VISIBLE
            
            // Calculate total spending
            val totalSpending = categoryData.sumOf { it.total }
            
            // Update budget status
            updateBudgetStatus(totalSpending)
            
            updatePieChart(categoryData)
            updateBarChart(categoryData)
        }
    }

    private fun updateBudgetStatus(totalSpending: Double) {
        budgetViewModel.budget.value?.let { budget ->
            // Set budget status visibility
            budgetStatusText.visibility = View.VISIBLE
            
            // Calculate percentage of max budget
            val percentOfMax = (totalSpending / budget.maxBudget) * 100
            
            when {
                totalSpending < budget.minBudget -> {
                    budgetStatusText.text = "Under Budget: ${String.format("%.2f", totalSpending)} / " +
                            "Min: ${String.format("%.2f", budget.minBudget)}"
                    budgetStatusText.setTextColor(Color.BLUE)
                }
                totalSpending > budget.maxBudget -> {
                    budgetStatusText.text = "Over Budget: ${String.format("%.2f", totalSpending)} / " +
                            "Max: ${String.format("%.2f", budget.maxBudget)} (${String.format("%.0f", percentOfMax)}%)"
                    budgetStatusText.setTextColor(Color.RED)
                }
                else -> {
                    budgetStatusText.text = "Within Budget: ${String.format("%.2f", totalSpending)} / " +
                            "Min: ${String.format("%.2f", budget.minBudget)} - " +
                            "Max: ${String.format("%.2f", budget.maxBudget)} (${String.format("%.0f", percentOfMax)}%)"
                    budgetStatusText.setTextColor(Color.GREEN)
                }
            }
        } ?: run {
            budgetStatusText.visibility = View.VISIBLE
            budgetStatusText.text = "No budget set - Total Spending: ${String.format("%.2f", totalSpending)}"
            budgetStatusText.setTextColor(Color.BLACK)
        }
    }

    private fun updatePieChart(data: List<CategorySpending>) {
        val entries = data.map { 
            PieEntry(it.total.toFloat(), it.category)
        }
        
        val dataSet = PieDataSet(entries, "Spending by Category")
        dataSet.colors = getColorList(data.size)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        
        val pieData = PieData(dataSet)
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.2f", value)
            }
        })
        
        pieChart.data = pieData
        pieChart.animateY(900, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        pieChart.animateX(700, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        pieChart.alpha = 0f
        pieChart.animate().alpha(1f).setDuration(400).start()
        pieChart.invalidate()
    }

    private fun updateBarChart(data: List<CategorySpending>) {
        val barEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        // Convert data to bar entries
        data.forEachIndexed { index, categorySpending ->
            barEntries.add(BarEntry(index.toFloat(), categorySpending.total.toFloat()))
            labels.add(categorySpending.category)
        }
        
        val dataSet = BarDataSet(barEntries, "Spending by Category")
        dataSet.colors = getColorList(data.size)
        dataSet.valueTextSize = 10f
        
        val barData = BarData(dataSet)
        barData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.2f", value)
            }
        })
        barChart.data = barData
        
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelRotationAngle = 45f
        
        // Add budget min/max limit lines if budget exists
        val leftAxis = barChart.axisLeft
        leftAxis.removeAllLimitLines() // Clear previous limit lines
        
        budgetViewModel.budget.value?.let { budget ->
            // Create min budget limit line
            val minBudgetLine = LimitLine(budget.minBudget.toFloat(), "Min Goal")
            minBudgetLine.lineColor = Color.BLUE
            minBudgetLine.lineWidth = 2f
            minBudgetLine.textColor = Color.BLUE
            minBudgetLine.textSize = 12f
            minBudgetLine.enableDashedLine(10f, 10f, 0f)
            minBudgetLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            
            // Create max budget limit line
            val maxBudgetLine = LimitLine(budget.maxBudget.toFloat(), "Max Goal")
            maxBudgetLine.lineColor = Color.RED
            maxBudgetLine.lineWidth = 2f
            maxBudgetLine.textColor = Color.RED
            maxBudgetLine.textSize = 12f
            maxBudgetLine.enableDashedLine(10f, 10f, 0f)
            maxBudgetLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            
            // Add limit lines to the chart
            leftAxis.addLimitLine(minBudgetLine)
            leftAxis.addLimitLine(maxBudgetLine)
            
            // Ensure the axis range covers the limit lines
            val maxY = Math.max(maxBudgetLine.limit * 1.2f, 
                barEntries.maxOfOrNull { it.y }?.times(1.2f) ?: maxBudgetLine.limit * 1.2f)
            leftAxis.axisMaximum = maxY
        }
        
        // Add space for limit line labels
        barChart.extraRightOffset = 50f
        barChart.animateY(900, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        barChart.animateX(700, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        barChart.alpha = 0f
        barChart.animate().alpha(1f).setDuration(400).start()
        barChart.invalidate()
    }

    private fun getColorList(size: Int): List<Int> {
        return ColorTemplate.MATERIAL_COLORS.toList().take(size)
    }
} 