package com.fisk.ui

import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fisk.data.FishDatabase
import com.fisk.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityResultBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        displayResults()
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnNewPhoto.setOnClickListener {
            finish()
        }
    }
    
    private fun displayResults() {
        // Get data from intent
        val imageBytes = intent.getByteArrayExtra("image")
        val imagePath = intent.getStringExtra("imagePath")
        val fishId = intent.getIntExtra("fishId", 0)
        val confidence = intent.getFloatExtra("confidence", 0f)
        val topResults = intent.getStringArrayListExtra("topResults") ?: arrayListOf()
        val topConfidences = intent.getFloatArrayExtra("topConfidences") ?: floatArrayOf()
        
        // Display image
        if (!imagePath.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                binding.ivCapturedFish.setImageBitmap(bitmap)
            } catch (t: Throwable) {
                Log.e("ResultActivity", "Failed to decode image from path", t)
            }
        } else {
            imageBytes?.let {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                binding.ivCapturedFish.setImageBitmap(bitmap)
            }
        }
        
        // Prefer label-based resolution over static index to avoid wrong mapping (e.g., id 0 -> Torsk)
        val primaryLabel = topResults.firstOrNull()
        var fish = if (primaryLabel != null) FishDatabase.getFishByLabel(primaryLabel) else null
        if (fish == null && primaryLabel == null) {
            // Fallback only if we have no labels
            fish = FishDatabase.getFishById(fishId)
        }

        if (fish != null) {
            binding.tvFishName.text = fish!!.norwegianName
            binding.tvScientificName.text = fish!!.scientificName
            binding.tvEnglishName.text = fish!!.englishName
            binding.tvConfidence.text = "Sikkerhet: ${(confidence * 100).toInt()}%"

            binding.tvDescription.text = fish!!.description
            binding.tvHabitat.text = "Habitat: ${fish!!.habitat}"
            binding.tvSize.text = "Størrelse: ${fish!!.averageSize}"

            val characteristicsText = fish!!.characteristics.joinToString("\n") { char -> "• $char" }
            binding.tvCharacteristics.text = characteristicsText
        } else {
            // No DB match for this label—show the label directly to avoid showing Torsk
            val display = primaryLabel ?: "Ukjent art"
            binding.tvFishName.text = display
            binding.tvScientificName.text = ""
            binding.tvEnglishName.text = ""
            binding.tvConfidence.text = "Sikkerhet: ${(confidence * 100).toInt()}%"
            binding.tvDescription.text = ""
            binding.tvHabitat.text = ""
            binding.tvSize.text = ""
            binding.tvCharacteristics.text = ""
        }
        
        // Display top 3 results
        if (topResults.isNotEmpty()) {
            val resultsText = buildString {
                append("Andre muligheter:\n")
                for (i in 1 until minOf(topResults.size, 3)) {
                    append("${i}. ${topResults[i]} - ${(topConfidences[i] * 100).toInt()}%\n")
                }
            }
            binding.tvOtherResults.text = resultsText
        }
    }
}
