package com.fisk.util

import android.content.Context
import org.json.JSONObject

data class ThresholdConfig(
    val multiClass: Float = 0.6f,
    val singleClass: Float = 0.98f,
    val requireFlipAgreement: Boolean = true,
    val minBrightness: Float = 0.15f,
    val minSharpness: Float = 0.08f,
    val minMargin: Float = 0.15f,
    val perClass: Map<String, Float> = emptyMap()
)

object ConfigManager {
    @Volatile private var cached: ThresholdConfig? = null

    fun getThresholds(context: Context): ThresholdConfig {
        cached?.let { return it }
        return try {
            val json = context.assets.open("config.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val th = root.optJSONObject("thresholds")
            val multi = th?.optDouble("multiClass", 0.6)?.toFloat() ?: 0.6f
            val single = th?.optDouble("singleClass", 0.98)?.toFloat() ?: 0.98f
            val requireFlip = root.optBoolean("requireFlipAgreement", true)
            val minBrightness = root.optDouble("minBrightness", 0.15).toFloat()
            val minSharpness = root.optDouble("minSharpness", 0.08).toFloat()
            val minMargin = root.optDouble("minMargin", 0.15).toFloat()
            val pct = root.optJSONObject("perClassThresholds")
            val map = mutableMapOf<String, Float>()
            if (pct != null) {
                val names = pct.names()
                if (names != null) {
                    for (i in 0 until names.length()) {
                        val key = names.getString(i)
                        val v = pct.optDouble(key, Double.NaN)
                        if (!v.isNaN()) map[key] = v.toFloat()
                    }
                }
            }
            ThresholdConfig(
                multiClass = multi,
                singleClass = single,
                requireFlipAgreement = requireFlip,
                minBrightness = minBrightness,
                minSharpness = minSharpness,
                minMargin = minMargin,
                perClass = map
            ).also {
                cached = it
            }
        } catch (_: Throwable) {
            ThresholdConfig().also { cached = it }
        }
    }
}
