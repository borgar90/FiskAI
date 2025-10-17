package com.fisk.ui

import android.graphics.BitmapFactory
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
        val fishId = intent.getIntExtra("fishId", 0)
        val confidence = intent.getFloatExtra("confidence", 0f)
        val topResults = intent.getStringArrayListExtra("topResults") ?: arrayListOf()
        val topConfidences = intent.getFloatArrayExtra("topConfidences") ?: floatArrayOf()
        
        // Display image
        imageBytes?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            binding.ivCapturedFish.setImageBitmap(bitmap)
        }
        
        // Get fish data
        val fish = FishDatabase.getFishById(fishId)
        
        fish?.let {
            binding.tvFishName.text = it.norwegianName
            binding.tvScientificName.text = it.scientificName
            binding.tvEnglishName.text = it.englishName
            binding.tvConfidence.text = "Sikkerhet: ${(confidence * 100).toInt()}%"
            
            binding.tvDescription.text = it.description
            binding.tvHabitat.text = "Habitat: ${it.habitat}"
            binding.tvSize.text = "Størrelse: ${it.averageSize}"
            
            // Display characteristics
            val characteristicsText = it.characteristics.joinToString("\n") { char ->
                "• $char"
            }
            binding.tvCharacteristics.text = characteristicsText
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
