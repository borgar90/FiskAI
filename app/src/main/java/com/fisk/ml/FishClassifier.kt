package com.fisk.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class Classification(
    val label: String,
    val confidence: Float,
    val fishId: Int
)

class FishClassifier(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()
    
    companion object {
        private const val MODEL_FILE = "fish_model.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INPUT_SIZE = 224
        private const val NUM_CHANNELS = 3
        private const val NUM_CLASSES = 10
    }
    
    init {
        loadModel()
        loadLabels()
    }
    
    /**
     * Load TensorFlow Lite model from assets
     */
    private fun loadModel() {
        try {
            val modelFile = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                // Enable GPU delegate if available
                // addDelegate(GpuDelegate())
            }
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            e.printStackTrace()
            // Model file might not exist yet - this is expected for initial setup
        }
    }
    
    /**
     * Load model file from assets
     */
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Load class labels from assets
     */
    private fun loadLabels() {
        try {
            context.assets.open(LABELS_FILE).bufferedReader().useLines { lines ->
                labels.addAll(lines.toList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If labels file doesn't exist, use default Norwegian fish names
            labels.addAll(
                listOf(
                    "Torsk",
                    "Sei",
                    "Hyse",
                    "Laks",
                    "Ørret",
                    "Makrell",
                    "Sild",
                    "Rødspette",
                    "Kveite",
                    "Abbor"
                )
            )
        }
    }
    
    /**
     * Classify fish from bitmap image
     */
    fun classifyImage(bitmap: Bitmap): List<Classification> {
        if (interpreter == null) {
            return emptyList()
        }
        
        // Preprocess image
        val processedBitmap = ImageProcessor.preprocessImage(bitmap, INPUT_SIZE)
        val inputBuffer = bitmapToByteBuffer(processedBitmap)
        
        // Run inference
        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter?.run(inputBuffer, output)
        
        // Process results
        return processOutput(output[0])
    }
    
    /**
     * Convert bitmap to ByteBuffer for model input
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * NUM_CHANNELS)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        for (pixel in pixels) {
            // Normalize pixel values to -1.0 to 1.0
            val r = ((pixel shr 16) and 0xFF) / 127.5f - 1.0f
            val g = ((pixel shr 8) and 0xFF) / 127.5f - 1.0f
            val b = (pixel and 0xFF) / 127.5f - 1.0f
            
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        
        return byteBuffer
    }
    
    /**
     * Process model output and return top classifications
     */
    private fun processOutput(output: FloatArray): List<Classification> {
        val classifications = mutableListOf<Classification>()
        
        for (i in output.indices) {
            val label = if (i < labels.size) labels[i] else "Unknown"
            classifications.add(
                Classification(
                    label = label,
                    confidence = output[i],
                    fishId = i
                )
            )
        }
        
        // Sort by confidence (descending) and return top 3
        return classifications
            .sortedByDescending { it.confidence }
            .take(3)
    }
    
    /**
     * Clean up resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
