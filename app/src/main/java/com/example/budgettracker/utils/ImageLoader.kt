package com.example.budgettracker.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.budgettracker.R
import java.io.File

object ImageLoader {
    fun loadTransactionImage(imageView: ImageView, imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            // Load placeholder if no image
            Glide.with(imageView.context)
                .load(R.drawable.ic_receipt_placeholder)
                .into(imageView)
            return
        }

        val imageFile = LocalImageStorage.getImage(imagePath)
        if (imageFile != null) {
            // Load local image
            Glide.with(imageView.context)
                .load(imageFile)
                .placeholder(R.drawable.ic_receipt_placeholder)
                .error(R.drawable.ic_receipt_placeholder)
                .into(imageView)
        } else {
            // Load placeholder if file not found
            Glide.with(imageView.context)
                .load(R.drawable.ic_receipt_placeholder)
                .into(imageView)
        }
    }

    fun loadProfileImage(imageView: ImageView, imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            // Load placeholder if no image
            Glide.with(imageView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(imageView)
            return
        }

        val imageFile = LocalImageStorage.getImage(imagePath)
        if (imageFile != null) {
            // Load local image
            Glide.with(imageView.context)
                .load(imageFile)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(imageView)
        } else {
            // Load placeholder if file not found
            Glide.with(imageView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(imageView)
        }
    }
} 