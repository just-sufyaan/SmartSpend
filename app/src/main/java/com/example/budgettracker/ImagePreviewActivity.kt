package com.example.budgettracker

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File

class ImagePreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val imageView: ImageView = findViewById(R.id.full_image_view)
        val imageUri = intent.getStringExtra("imageUri")

        if (!imageUri.isNullOrEmpty()) {
            val isFilePath = imageUri.startsWith("/") ||
                !(imageUri.startsWith("content://") || imageUri.startsWith("file://"))
            if (isFilePath) {
                Glide.with(this)
                    .load(File(imageUri))
                    .into(imageView)
            } else {
                Glide.with(this)
                    .load(Uri.parse(imageUri))
                    .into(imageView)
            }
        }

        // Handle back button
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }
    }
}
