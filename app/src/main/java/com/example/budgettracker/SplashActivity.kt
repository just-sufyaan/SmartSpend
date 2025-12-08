package com.example.budgettracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Find views
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val appNameText = findViewById<TextView>(R.id.appNameText)
        val taglineText = findViewById<TextView>(R.id.taglineText)
        val splashContent = findViewById<LinearLayout>(R.id.splashContent)
        
        // Load animations
        val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeInAnimation.duration = 1000
        
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        
        // Apply animations
        logoImage.startAnimation(fadeInAnimation)
        
        // Delay other animations
        Handler(Looper.getMainLooper()).postDelayed({
            appNameText.visibility = View.VISIBLE
            appNameText.startAnimation(slideUpAnimation)
        }, 300)
        
        Handler(Looper.getMainLooper()).postDelayed({
            taglineText.visibility = View.VISIBLE
            taglineText.startAnimation(slideUpAnimation)
        }, 500)
        
        // Navigate to next screen after delay
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)
    }
}
