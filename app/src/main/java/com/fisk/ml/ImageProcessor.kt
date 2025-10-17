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
     * Estimate brightness [0..1] using average luma on a downscaled sample
     */
    fun estimateBrightness(bitmap: Bitmap): Float {
        val sample = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        var sum = 0f
        val n = sample.width * sample.height
        val pixels = IntArray(n)
        sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
        for (p in pixels) {
            val r = (p shr 16 and 0xFF)
            val g = (p shr 8 and 0xFF)
            val b = (p and 0xFF)
            // ITU-R BT.601 luma approximation
            val y = 0.299f * r + 0.587f * g + 0.114f * b
            sum += y
        }
        return (sum / n) / 255f
    }

    /**
     * Estimate sharpness using simple Laplacian variance on grayscale sample
     */
    fun estimateSharpness(bitmap: Bitmap): Float {
        val sample = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
        val w = sample.width
        val h = sample.height
        val gray = FloatArray(w * h)
        val px = IntArray(w * h)
        sample.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val p = px[i]
            val r = (p shr 16 and 0xFF).toFloat()
            val g = (p shr 8 and 0xFF).toFloat()
            val b = (p and 0xFF).toFloat()
            gray[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }
        var sum = 0.0
        var sumSq = 0.0
        fun idx(x: Int, y: Int) = y * w + x
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val c = -4 * gray[idx(x, y)] + gray[idx(x - 1, y)] + gray[idx(x + 1, y)] + gray[idx(x, y - 1)] + gray[idx(x, y + 1)]
                sum += c
                sumSq += c * c
            }
        }
        val n = (w - 2) * (h - 2)
        val mean = sum / n
        val varLap = sumSq / n - mean * mean
        return varLap.toFloat()
    }

    /**
     * Compute how much of the image falls within a centered square margin (for framing check)
     */
    fun centralMargin(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val minDim = minOf(w, h).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val margin = 0.5f * minDim // central square half-size factor
        val left = cx - margin
        val right = cx + margin
        val top = cy - margin
        val bottom = cy + margin
        val areaCentral = (right - left) * (bottom - top)
        val areaTotal = (w * h).toFloat()
        return (areaCentral / areaTotal).coerceIn(0f, 1f)
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
