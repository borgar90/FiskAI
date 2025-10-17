package com.fisk

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
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
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        // Rotate bitmap if needed
        return rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
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
            val results = fishClassifier.classifyImage(bitmap)
            
            // Convert bitmap to byte array for passing to next activity
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val imageBytes = stream.toByteArray()
            
            runOnUiThread {
                binding.btnCapture.isEnabled = true
                binding.btnCapture.text = "Ta Bilde"
                
                if (results.isNotEmpty()) {
                    val intent = Intent(this, ResultActivity::class.java).apply {
                        putExtra("image", imageBytes)
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
