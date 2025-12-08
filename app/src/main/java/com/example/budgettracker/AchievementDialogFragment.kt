package com.example.budgettracker

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.budgettracker.data.Achievement
import android.view.animation.AnimationUtils

class AchievementDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_ACHIEVEMENT = "achievement"

        fun newInstance(achievement: Achievement): AchievementDialogFragment {
            val args = Bundle().apply {
                putString("achievement_name", achievement.name)
                putString("achievement_desc", achievement.description)
                putInt("achievement_icon", achievement.iconResourceId)
            }
            return AchievementDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_achievement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val achievementName = arguments?.getString("achievement_name")
        val achievementDesc = arguments?.getString("achievement_desc")
        val achievementIcon = arguments?.getInt("achievement_icon") ?: android.R.drawable.star_big_on

        val nameTextView: TextView = view.findViewById(R.id.achievement_name)
        val descriptionTextView: TextView = view.findViewById(R.id.achievement_description)
        val iconImageView: ImageView = view.findViewById(R.id.achievement_icon)
        val closeButton: Button = view.findViewById(R.id.close_button)

        nameTextView.text = achievementName
        descriptionTextView.text = achievementDesc
        iconImageView.setImageResource(achievementIcon)

        // Apply animation to the achievement icon
        val rotateAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_achievement)
        val scaleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_achievement)
        iconImageView.startAnimation(rotateAnim)
        nameTextView.startAnimation(scaleAnim)

        closeButton.setOnClickListener {
            dismiss()
        }
    }
} 