package com.example.budgettracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageHelper {

    private const val COMPRESSION_QUALITY = 80
    private const val MAX_IMAGE_DIMENSION = 1024

    /**
     * Saves an image from a URI to internal storage with compression
     */
    fun saveImageToInternalStorage(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Compress the bitmap
            val compressedBitmap = compressBitmap(bitmap)
            
            // Create a unique filename
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "RECEIPT_$timeStamp.jpg"
            val file = File(context.filesDir, fileName)
            
            // Write the compressed bitmap to internal storage
            val outputStream = FileOutputStream(file)
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            outputStream.close()
            
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Compresses a bitmap by scaling it down if needed
     */
    private fun compressBitmap(originalBitmap: Bitmap): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        
        // If image is already smaller than maximum dimensions, return it as is
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return originalBitmap
        }
        
        // Calculate the scaling factor
        val scaleFactor = when {
            width > height -> MAX_IMAGE_DIMENSION.toFloat() / width
            else -> MAX_IMAGE_DIMENSION.toFloat() / height
        }
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    }
    
    /**
     * Gets a bitmap from a file path
     */
    fun getBitmapFromPath(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 