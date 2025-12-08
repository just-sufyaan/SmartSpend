package com.example.budgettracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object LocalImageStorage {
    private const val IMAGE_DIR = "transaction_images"
    
    fun saveImage(context: Context, imageUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        
        // Create directory if it doesn't exist
        val directory = File(context.filesDir, IMAGE_DIR)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        // Generate unique filename
        val filename = "img_${UUID.randomUUID()}.jpg"
        val file = File(directory, filename)
        
        // Save compressed bitmap
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        
        return file.absolutePath
    }
    
    fun getImage(path: String): File? {
        val file = File(path)
        return if (file.exists()) file else null
    }
    
    fun deleteImage(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
} 