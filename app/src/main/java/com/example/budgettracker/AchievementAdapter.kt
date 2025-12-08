package com.example.budgettracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.data.Achievement

class AchievementAdapter(private val context: Context) : 
    ListAdapter<Achievement, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val itemView = LayoutInflater.from(context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = getItem(position)
        holder.bind(achievement)
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

    inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.achievement_icon)
        private val nameTextView: TextView = itemView.findViewById(R.id.achievement_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.achievement_description)
        private val statusTextView: TextView = itemView.findViewById(R.id.achievement_status)
        
        fun bind(achievement: Achievement) {
            // Set icon
            iconImageView.setImageResource(achievement.iconResourceId)
            
            // Set text content
            nameTextView.text = achievement.name
            descriptionTextView.text = achievement.description
            
            // Set status and appearance
            if (achievement.isEarned) {
                statusTextView.text = "Unlocked: ${achievement.dateEarned ?: "Unknown"}"
                statusTextView.setTextColor(ContextCompat.getColor(context, R.color.green))
                itemView.alpha = 1.0f
                iconImageView.setColorFilter(null)
                nameTextView.alpha = 1.0f
                descriptionTextView.alpha = 1.0f
            } else {
                statusTextView.text = "Locked"
                statusTextView.setTextColor(ContextCompat.getColor(context, R.color.gray))
                itemView.alpha = 0.7f
                iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.gray))
                nameTextView.alpha = 0.7f
                descriptionTextView.alpha = 0.7f
            }
        }
    }

    private class AchievementDiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem == newItem
        }
    }
} 