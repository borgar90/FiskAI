package com.fisk

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.fisk.databinding.ActivityCameraBinding
import com.fisk.ml.FishClassifier
import com.fisk.ui.ResultActivity
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fishClassifier: FishClassifier
    
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    
    companion object {
        private const val TAG = "CameraActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        fishClassifier = FishClassifier(this)
        
        startCamera()
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }
        
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            
            // Image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(this, "Kunne ikke starte kamera", Toast.LENGTH_SHORT).show()
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    
                    if (bitmap != null) {
                        classifyAndShowResult(bitmap)
                    } else {
                        Toast.makeText(
                            this@CameraActivity,
                            "Kunne ikke ta bilde",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(
                        this@CameraActivity,
                        "Feil ved bildeopptak",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
    
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            // Convert YUV_420_888 to NV21
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            // Create JPEG from NV21
            val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            val imageBytes = out.toByteArray()

            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            bitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
            bitmap
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to convert ImageProxy to Bitmap", t)
            null
        }
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun classifyAndShowResult(bitmap: Bitmap) {
        // Show loading indicator
        binding.btnCapture.isEnabled = false
        binding.btnCapture.text = "Analyserer..."
        
        // Run classification in background
        cameraExecutor.execute {
            val results = try {
                fishClassifier.classifyImage(bitmap)
            } catch (t: Throwable) {
                Log.e(TAG, "Classification error", t)
                emptyList()
            }
            
            // Save image to a temporary cache file instead of passing large byte arrays
            val imagePath: String? = try {
                val file = File(cacheDir, "captured_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    fos.flush()
                }
                file.absolutePath
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to save captured image", t)
                null
            }
            
            runOnUiThread {
                binding.btnCapture.isEnabled = true
                binding.btnCapture.text = "Ta Bilde"
                
                if (results.isNotEmpty()) {
                    val top = results[0]
                    // For single-class models we synthesize an "Other" class; avoid false positive navigation
                    if (top.label.equals("Other", ignoreCase = true) || top.confidence < 0.85f) {
                        Toast.makeText(
                            this,
                            "Usikker gjenkjenning – prøv igjen med bedre lys/utsnitt",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@runOnUiThread
                    }
                    val intent = Intent(this, ResultActivity::class.java).apply {
                        imagePath?.let { putExtra("imagePath", it) }
                        putExtra("fishId", results[0].fishId)
                        putExtra("confidence", results[0].confidence)
                        putExtra("topResults", ArrayList(results.map { it.label }))
                        putExtra("topConfidences", results.map { it.confidence }.toFloatArray())
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Kunne ikke identifisere fisk",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun toggleFlash() {
        camera?.let {
            val hasFlash = it.cameraInfo.hasFlashUnit()
            if (hasFlash) {
                it.cameraControl.enableTorch(
                    it.cameraInfo.torchState.value == TorchState.OFF
                )
            } else {
                Toast.makeText(this, "Ingen blits tilgjengelig", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        fishClassifier.close()
    }
}
