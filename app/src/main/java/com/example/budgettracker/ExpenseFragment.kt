package com.example.budgettracker

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.adapters.TransactionAdapter
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import com.example.budgettracker.viewmodel.TransactionViewModel

class ExpenseFragment : Fragment() {

    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var emptyMessage: TextView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var totalExpensesText: TextView

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val transactionViewModel: TransactionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_expense_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startDateInput = view.findViewById(R.id.startDateInput)
        endDateInput = view.findViewById(R.id.endDateInput)
        expensesRecyclerView = view.findViewById(R.id.expenses_recyclerview)
        emptyMessage = view.findViewById(R.id.empty_message)
        totalExpensesText = view.findViewById(R.id.total_expenses)

        transactionAdapter = TransactionAdapter(
            onItemClick = {},
            onItemLongClick = {},
            onReceiptClick = { imageUrl ->
                val context = requireContext()
                val intent = Intent(context, ImagePreviewActivity::class.java)
                intent.putExtra("imageUri", imageUrl)
                context.startActivity(intent)
            }
        )
        expensesRecyclerView.adapter = transactionAdapter
        expensesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        setupDatePicker(startDateInput)
        setupDatePicker(endDateInput)

        // Observe transactions from Firebase
        transactionViewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            filterAndDisplayExpenses(transactions ?: emptyList())
        }
    }

    private fun setupDatePicker(input: TextInputEditText) {
        input.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    input.setText(date)
                    // Filter whenever a date is picked
                    val transactions = transactionViewModel.transactions.value ?: emptyList()
                    filterAndDisplayExpenses(transactions)
                },
                year, month, day
            ).show()
        }
    }

    private fun filterAndDisplayExpenses(transactions: List<com.example.budgettracker.firebase.models.FirebaseTransaction>) {
        val startDate = startDateInput.text.toString().trim()
        val endDate = endDateInput.text.toString().trim()

        if (startDate.isBlank() || endDate.isBlank()) {
            emptyMessage.text = "Select a date range to view expenses."
            emptyMessage.visibility = View.VISIBLE
            expensesRecyclerView.visibility = View.GONE
            transactionAdapter.submitList(emptyList())
            totalExpensesText.text = "R 0.00"
            return
        }

        val start = try { dateFormat.parse(startDate) } catch (e: Exception) { null }
        val end = try { dateFormat.parse(endDate) } catch (e: Exception) { null }
        if (start == null || end == null) {
            emptyMessage.text = "Invalid date format."
            emptyMessage.visibility = View.VISIBLE
            expensesRecyclerView.visibility = View.GONE
            transactionAdapter.submitList(emptyList())
            totalExpensesText.text = "R 0.00"
            return
        }

        val filtered = transactions.filter { txn ->
            val isExpense = txn.isExpense
            val txnDateStr = txn.date
            val txnDate = try { dateFormat.parse(txnDateStr) } catch (e: Exception) { null }
            val inRange = txnDate != null && !txnDate.before(start) && !txnDate.after(end)
            isExpense && inRange
        }.sortedByDescending { it.date }

        val total = filtered.sumOf { it.amount }
        totalExpensesText.text = "R %.2f".format(total)

        if (filtered.isEmpty()) {
            emptyMessage.text = "No expenses found for the selected date range."
            emptyMessage.visibility = View.VISIBLE
            expensesRecyclerView.visibility = View.GONE
            transactionAdapter.submitList(emptyList())
        } else {
            emptyMessage.visibility = View.GONE
            expensesRecyclerView.visibility = View.VISIBLE
            transactionAdapter.submitList(filtered)
        }
    }
}
