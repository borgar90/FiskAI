package com.fisk.ml

import android.graphics.Bitmap
import android.graphics.Matrix

object ImageProcessor {
    
    /**
     * Resize and crop image to required model input size
     */
    fun preprocessImage(bitmap: Bitmap, targetSize: Int = 224): Bitmap {
        // Calculate crop dimensions (center crop)
        val dimension = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - dimension) / 2
        val yOffset = (bitmap.height - dimension) / 2
        
        // Crop to square
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            xOffset,
            yOffset,
            dimension,
            dimension
        )
        
        // Resize to model input size
        return Bitmap.createScaledBitmap(
            croppedBitmap,
            targetSize,
            targetSize,
            true
        )
    }
    
    /**
     * Rotate image by degrees
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    
    /**
     * Convert bitmap to ByteArray for TensorFlow Lite
     */
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val bytes = ByteArray(bitmap.width * bitmap.height * 3)
        var index = 0
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                
                // Extract RGB values (normalize to 0-255)
                bytes[index++] = ((pixel shr 16) and 0xFF).toByte()
                bytes[index++] = ((pixel shr 8) and 0xFF).toByte()
                bytes[index++] = (pixel and 0xFF).toByte()
            }
        }
        
        return bytes
    }
    
    /**
     * Normalize pixel values to -1.0 to 1.0 range
     */
    fun normalizePixels(bytes: ByteArray): FloatArray {
        return FloatArray(bytes.size) { i ->
            (bytes[i].toInt() and 0xFF) / 127.5f - 1.0f
        }
    }
}
