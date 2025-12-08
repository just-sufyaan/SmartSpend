package com.example.budgettracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.data.Achievement
import com.example.budgettracker.viewmodel.AchievementViewModel
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class AchievementsFragment : Fragment() {
    
    private val achievementViewModel: AchievementViewModel by viewModels()
    private lateinit var achievementsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: AchievementAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_achievements, container, false)
        
        achievementsRecyclerView = view.findViewById(R.id.achievements_recycler_view)
        emptyView = view.findViewById(R.id.empty_achievements_view)
        
        setupRecyclerView()
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            achievementViewModel.initializeUserData(currentUserId)
            observeAchievements()
            
            // Check for newly earned achievements
            achievementViewModel.checkForAchievements()
        } else {
            showEmptyState()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = AchievementAdapter(requireContext())
        achievementsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        achievementsRecyclerView.adapter = adapter
    }
    
    private fun observeAchievements() {
        achievementViewModel.allAchievements.observe(viewLifecycleOwner) { achievements ->
            if (achievements.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
                adapter.submitList(achievements)
            }
        }
        
        // Observe for newly earned achievements to show notifications
        achievementViewModel.newAchievement.observe(viewLifecycleOwner) { achievement ->
            achievement?.let {
                showAchievementNotification(it)
                achievementViewModel.clearNewAchievementNotification()
            }
        }
    }
    
    private fun showEmptyState() {
        emptyView.visibility = View.VISIBLE
        achievementsRecyclerView.visibility = View.GONE
    }
    
    private fun hideEmptyState() {
        emptyView.visibility = View.GONE
        achievementsRecyclerView.visibility = View.VISIBLE
    }
    
    private fun showAchievementNotification(achievement: Achievement) {
        // Show a toast or notification when a new achievement is earned
        val dialog = AchievementDialogFragment.newInstance(achievement)
        dialog.show(parentFragmentManager, "achievement_dialog")
    }
} 