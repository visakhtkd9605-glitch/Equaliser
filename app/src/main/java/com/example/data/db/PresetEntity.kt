package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val genreCategoryId: Long,
    val genreName: String,
    val imageUri: String? = null,
    val bandLevelsCsv: String, // 10 canonical dB values (-12f..+12f)
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerStrength: Int = 0, // 0..1000
    val loudnessGainMb: Int = 0, // 0..1500
    val preampDb: Float = 0f, // -12f..+12f
    val isBuiltIn: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun parsedBandGainsDb(): List<Float> {
        val parsed = bandLevelsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        return if (parsed.size == 10) parsed else List(10) { idx -> parsed.getOrElse(idx) { 0f } }
    }
}
