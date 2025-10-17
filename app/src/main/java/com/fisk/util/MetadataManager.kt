package com.fisk.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SpeciesMeta(
    val label: String,
    val norwegianName: String?,
    val englishName: String?,
    val scientificName: String?,
    val description: String?,
    val habitat: String?,
    val averageSize: String?,
    val characteristics: List<String>
)

object MetadataManager {
    @Volatile private var byLabel: Map<String, SpeciesMeta>? = null

    fun getByLabel(context: Context, label: String): SpeciesMeta? {
        if (byLabel == null) load(context)
        return byLabel?.get(label)
    }

    private fun load(context: Context) {
        try {
            val json = context.assets.open("metadata.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val arr: JSONArray = root.optJSONArray("species") ?: JSONArray()
            val map = mutableMapOf<String, SpeciesMeta>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val label = o.optString("label")
                val meta = SpeciesMeta(
                    label = label,
                    norwegianName = o.optString("norwegianName", null),
                    englishName = o.optString("englishName", null),
                    scientificName = o.optString("scientificName", null),
                    description = o.optString("description", null),
                    habitat = o.optString("habitat", null),
                    averageSize = o.optString("averageSize", null),
                    characteristics = o.optJSONArray("characteristics")?.let { ja ->
                        (0 until ja.length()).map { idx -> ja.optString(idx) }.filter { it.isNotBlank() }
                    } ?: emptyList()
                )
                if (label.isNotBlank()) map[label] = meta
            }
            byLabel = map
        } catch (_: Throwable) {
            byLabel = emptyMap()
        }
    }
}
