package com.example.budgettracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.R
import com.example.budgettracker.data.CategorySpending
import java.text.NumberFormat
import java.util.Locale

class CategorySpendingAdapter(private val categorySpendingList: List<CategorySpending>) :
    RecyclerView.Adapter<CategorySpendingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryText: TextView = view.findViewById(R.id.category_text)
        val amountText: TextView = view.findViewById(R.id.amount_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_spending, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categorySpending = categorySpendingList[position]
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        
        holder.categoryText.text = categorySpending.category
        holder.amountText.text = numberFormat.format(categorySpending.total)
    }

    override fun getItemCount() = categorySpendingList.size
} 