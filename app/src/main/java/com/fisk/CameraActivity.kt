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
import android.view.ScaleGestureDetector
import android.view.MotionEvent
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
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    
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
        setupZoomGesture()
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

    private fun setupZoomGesture() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val camera = camera ?: return false
                val currentZoom = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val scaleFactor = detector.scaleFactor
                val newZoom = (currentZoom * scaleFactor).coerceIn(
                    camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1f,
                    camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 10f
                )
                camera.cameraControl.setZoomRatio(newZoom)
                return true
            }
        })

        binding.viewFinder.setOnTouchListener { _, event: MotionEvent ->
            scaleGestureDetector.onTouchEvent(event)
            true
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

        // Capture directly to a file to avoid fragile YUV conversions
        val photoFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val path = photoFile.absolutePath
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        classifyAndShowResult(bitmap, existingImagePath = path)
                    } else {
                        Toast.makeText(
                            this@CameraActivity,
                            "Kunne ikke ta bilde",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo file save failed: ${exception.message}", exception)
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
    
    private fun classifyAndShowResult(bitmap: Bitmap, existingImagePath: String? = null) {
        // Show loading indicator
        binding.btnCapture.isEnabled = false
        binding.btnCapture.text = "Analyserer..."
        
        // Run classification in background
        cameraExecutor.execute {
            val results = try { fishClassifier.classifyImage(bitmap) } catch (t: Throwable) { Log.e(TAG, "Classification error", t); emptyList() }
            // Second pass: flipped image for robustness in single-class scenarios
            val flipped = flipHorizontal(bitmap)
            val resultsFlipped = try { fishClassifier.classifyImage(flipped) } catch (_: Throwable) { emptyList() }
            
            // Reuse existing saved file path if provided; otherwise save a new copy
            val imagePath: String? = existingImagePath ?: try {
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
                    val topFlip = resultsFlipped.firstOrNull()
                    // Detect single-class mode by presence of synthetic 'Other'
                    val isSingleClassMode = results.any { it.label.equals("Other", ignoreCase = true) }
                    if (isSingleClassMode) {
                        val highConf = top.confidence >= 0.98f
                        val agrees = topFlip != null && topFlip.label == top.label && topFlip.confidence >= 0.98f
                        if (top.label.equals("Other", ignoreCase = true) || !highConf || !agrees) {
                            Toast.makeText(
                                this,
                                "Usikker gjenkjenning – prøv igjen med bedre lys/utsnitt",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }
                    } else {
                        // Multi-class: relax threshold and don't require flip agreement
                        if (top.confidence < 0.60f) {
                            Toast.makeText(
                                this,
                                "Usikker gjenkjenning – prøv igjen med bedre lys/utsnitt",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }
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

    private fun flipHorizontal(src: Bitmap): Bitmap {
        val m = Matrix()
        m.preScale(-1f, 1f)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
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
