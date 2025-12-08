package com.example.budgettracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.R
import com.example.budgettracker.firebase.models.FirebaseTransaction
import com.example.budgettracker.utils.ImageLoader
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private val onItemClick: (FirebaseTransaction) -> Unit,
    private val onItemLongClick: (FirebaseTransaction) -> Unit,
    private val onReceiptClick: (String) -> Unit
) : ListAdapter<FirebaseTransaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
        
        holder.itemView.setOnClickListener { onItemClick(transaction) }
        holder.itemView.setOnLongClickListener { 
            onItemLongClick(transaction)
            true
        }
        // Animate item appearance
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay((position * 30).toLong())
            .start()
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val amountText: TextView = itemView.findViewById(R.id.transaction_amount)
        private val descriptionText: TextView = itemView.findViewById(R.id.transaction_description)
        private val categoryText: TextView = itemView.findViewById(R.id.transaction_category)
        private val dateText: TextView = itemView.findViewById(R.id.transaction_date)
        private val receiptImage: ImageView = itemView.findViewById(R.id.transaction_receipt_image)
        private val typeIndicator: View = itemView.findViewById(R.id.transaction_type_indicator)

        fun bind(transaction: FirebaseTransaction) {
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            val amount = numberFormat.format(transaction.amount)
            
            amountText.text = amount
            descriptionText.text = transaction.description
            categoryText.text = transaction.category
            dateText.text = transaction.date
            
            // Set type indicator color
            typeIndicator.setBackgroundResource(
                if (transaction.isExpense) R.color.expense_red else R.color.income_green
            )
            
            // Load receipt image
            ImageLoader.loadTransactionImage(receiptImage, transaction.imageUrl)
            
            // Show/hide receipt image
            if (transaction.imageUrl != null) {
                receiptImage.visibility = View.VISIBLE
                receiptImage.setOnClickListener {
                    onReceiptClick(transaction.imageUrl)
                }
            } else {
                receiptImage.visibility = View.GONE
                receiptImage.setOnClickListener(null)
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<FirebaseTransaction>() {
        override fun areItemsTheSame(oldItem: FirebaseTransaction, newItem: FirebaseTransaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FirebaseTransaction, newItem: FirebaseTransaction): Boolean {
            return oldItem == newItem
        }
    }
} 