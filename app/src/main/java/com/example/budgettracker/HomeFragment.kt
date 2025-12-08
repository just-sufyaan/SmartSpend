package com.example.budgettracker

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.adapters.TransactionAdapter
import com.example.budgettracker.firebase.models.FirebaseTransaction
import com.example.budgettracker.viewmodel.BudgetViewModel
import com.example.budgettracker.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.view.ViewCompat

class HomeFragment : Fragment() {

    private lateinit var balance: TextView
    private lateinit var budget: TextView
    private lateinit var expense: TextView
    private lateinit var lastUpdated: TextView
    private lateinit var transactionRecyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var progressBar: ProgressBar
    
    private val transactionViewModel: TransactionViewModel by activityViewModels()
    private val budgetViewModel: BudgetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize views
        balance = view.findViewById(R.id.balance)
        budget = view.findViewById(R.id.budget)
        expense = view.findViewById(R.id.expense)
        lastUpdated = view.findViewById(R.id.last_updated)
        progressBar = view.findViewById(R.id.progressBar)
        transactionRecyclerView = view.findViewById(R.id.transaction_recycler_view)

        // Initialize with zero values
        balance.text = "R 0.00"
        budget.text = "R 0.00"
        expense.text = "R 0.00"
        lastUpdated.text = "No transactions yet"
        progressBar.progress = 0

        // Setup RecyclerView
        val animator = DefaultItemAnimator()
        animator.addDuration = 500
        animator.removeDuration = 300
        transactionRecyclerView.itemAnimator = animator
        transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Create adapter
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                // Handle transaction click
            },
            onItemLongClick = { transaction ->
                // Handle transaction long click
            },
            onReceiptClick = { imageUrl ->
                // Launch ImagePreviewActivity with the image URI
                val context = requireContext()
                val intent = Intent(context, ImagePreviewActivity::class.java)
                intent.putExtra("imageUri", imageUrl)
                context.startActivity(intent)
            }
        )
        transactionRecyclerView.adapter = transactionAdapter

        // Observe loading state
        transactionViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error state
        transactionViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Handle error state if needed
            }
        }

        // Observe transactions from ViewModel
        transactionViewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions ?: emptyList())
            updateDashboard(transactions ?: emptyList())
            updateLastUpdatedDate(transactions ?: emptyList())
        }

        // Observe budget for progress bar
        budgetViewModel.budget.observe(viewLifecycleOwner) { budget ->
            if (budget != null) {
                updateProgressBar(budget.maxBudget)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Animate dashboard cards
        val dashboardViews = listOf(balance, budget, expense, lastUpdated, progressBar)
        dashboardViews.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 40f
            v.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay((i * 60).toLong()).start()
        }
        // FAB hide/show on scroll
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab_add)
        transactionRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) fab.hide() else if (dy < 0) fab.show()
            }
        })
    }

    private fun updateDashboard(transactions: List<FirebaseTransaction>) {
        var totalBalance = 0.0
        var totalBudget = 0.0
        var totalExpense = 0.0

        for (transaction in transactions) {
            if (transaction.isExpense) {
                totalExpense += transaction.amount
                totalBalance -= transaction.amount
            } else {
                totalBudget += transaction.amount
                totalBalance += transaction.amount
            }
        }

        balance.text = "R %.2f".format(totalBalance)
        budget.text = "R %.2f".format(totalBudget)
        expense.text = "R %.2f".format(totalExpense)

        updateProgressBar(totalBudget)
    }
    
    private fun updateProgressBar(maxBudget: Double) {
        var totalExpense = 0.0
        transactionViewModel.transactions.value?.filter { it.isExpense }?.forEach { totalExpense += it.amount }
        
        if (maxBudget > 0) {
            val progress = ((totalExpense / maxBudget) * 100).toInt().coerceAtMost(100)
            ObjectAnimator.ofInt(progressBar, "progress", progress)
                .setDuration(500)
                .start()
        } else {
            progressBar.progress = 0
        }
    }

    private fun updateLastUpdatedDate(transactions: List<FirebaseTransaction>) {
        if (transactions.isNotEmpty()) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            val latestTransaction = transactions.maxByOrNull { it.date }

            latestTransaction?.let {
                try {
                    val parsedDate = inputFormat.parse(it.date)
                    val formattedDate = outputFormat.format(parsedDate!!)
                    lastUpdated.text = "Last updated: $formattedDate"
                } catch (e: Exception) {
                    lastUpdated.text = "Last updated: ${it.date}"
                }
            }
        } else {
            lastUpdated.text = "No transactions yet"
        }
    }

    fun refreshData() {
        // The ViewModel will handle data refresh via LiveData
    }
}
