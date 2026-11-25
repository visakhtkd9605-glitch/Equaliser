package com.example.data.repository

import com.example.data.db.EqualizerDao
import com.example.data.db.GenreCategoryEntity
import com.example.data.db.PresetEntity
import com.example.data.prefs.EqualizerSettings
import com.example.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EqualizerRepository(
    private val dao: EqualizerDao,
    private val settingsDataStore: SettingsDataStore
) {
    private val seedMutex = Mutex()

    val categories: Flow<List<GenreCategoryEntity>> = dao.observeCategories()
    val presets: Flow<List<PresetEntity>> = dao.observePresets()
    val settings: Flow<EqualizerSettings> = settingsDataStore.settingsFlow

    suspend fun seedDefaultsIfNeeded() {
        seedMutex.withLock {
            if (dao.getCategoryCount() > 0 && dao.getPresetCount() > 0) return

            val flatId = dao.insertCategory(
                GenreCategoryEntity(
                    name = "Studio Flat",
                    description = "Transparent reference curve",
                    accentColorHex = 0xFF94A3B8,
                    isBuiltIn = true
                )
            )
            val rockId = dao.insertCategory(
                GenreCategoryEntity(
                    name = "Rock",
                    description = "Punchy low-end & crisp guitar bite",
                    accentColorHex = 0xFFFF5A79,
                    isBuiltIn = true
                )
            )
            val jazzId = dao.insertCategory(
                GenreCategoryEntity(
                    name = "Jazz",
                    description = "Warm upright bass & brass presence",
                    accentColorHex = 0xFFFFB020,
                    isBuiltIn = true
                )
            )
            val electronicId = dao.insertCategory(
                GenreCategoryEntity(
                    name = "Electronic",
                    description = "Deep sub-bass & sparkling highs",
                    accentColorHex = 0xFF22D3EE,
                    isBuiltIn = true
                )
            )
            val classicalId = dao.insertCategory(
                GenreCategoryEntity(
                    name = "Classical",
                    description = "Open hall staging & string clarity",
                    accentColorHex = 0xFFA78BFA,
                    isBuiltIn = true
                )
            )
            val hipHopId = dao.insertCategory(
                GenreCategoryEntity(
                    name = "Hip-Hop",
                    description = "Sub-octave impact & vocal snap",
                    accentColorHex = 0xFF34D399,
                    isBuiltIn = true
                )
            )
            val vocalId = dao.insertCategory(
                GenreCategoryEntity(
                    name = "Vocal & Podcast",
                    description = "Speech intelligibility & de-rumble",
                    accentColorHex = 0xFF38BDF8,
                    isBuiltIn = true
                )
            )

            val defaults = listOf(
                PresetEntity(
                    name = "Reference Studio",
                    genreCategoryId = flatId,
                    genreName = "Studio Flat",
                    imageUri = "drawable:img_studio_hero_1795642438125",
                    bandLevelsCsv = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                    bassBoostStrength = 0,
                    virtualizerStrength = 0,
                    loudnessGainMb = 0,
                    preampDb = 0f,
                    isBuiltIn = true
                ),
                PresetEntity(
                    name = "Arena Rock",
                    genreCategoryId = rockId,
                    genreName = "Rock",
                    imageUri = "drawable:img_preset_rock_1795642437515",
                    bandLevelsCsv = "4.5,3.5,2.0,0.5,-1.5,-1.0,1.5,3.5,4.5,4.0",
                    bassBoostStrength = 380,
                    virtualizerStrength = 250,
                    loudnessGainMb = 150,
                    preampDb = 0.5f,
                    isBuiltIn = true
                ),
                PresetEntity(
                    name = "Midnight Velvet Jazz",
                    genreCategoryId = jazzId,
                    genreName = "Jazz",
                    imageUri = "drawable:img_preset_jazz_1795642438201",
                    bandLevelsCsv = "3.0,2.5,1.5,2.0,-1.0,0.5,2.0,2.5,3.5,2.0",
                    bassBoostStrength = 240,
                    virtualizerStrength = 420,
                    loudnessGainMb = 100,
                    preampDb = 0f,
                    isBuiltIn = true
                ),
                PresetEntity(
                    name = "Neon Club Bass",
                    genreCategoryId = electronicId,
                    genreName = "Electronic",
                    imageUri = "drawable:img_preset_electronic_1795642439228",
                    bandLevelsCsv = "7.0,5.5,2.5,-0.5,-2.0,0.0,2.0,4.0,5.5,6.0",
                    bassBoostStrength = 680,
                    virtualizerStrength = 480,
                    loudnessGainMb = 300,
                    preampDb = -1.0f,
                    isBuiltIn = true
                ),
                PresetEntity(
                    name = "Symphony Hall",
                    genreCategoryId = classicalId,
                    genreName = "Classical",
                    imageUri = "drawable:img_studio_hero_1795642438125",
                    bandLevelsCsv = "2.5,1.5,0.5,0.0,0.0,0.5,2.0,3.0,3.5,3.0",
                    bassBoostStrength = 100,
                    virtualizerStrength = 580,
                    loudnessGainMb = 0,
                    preampDb = 0.5f,
                    isBuiltIn = true
                ),
                PresetEntity(
                    name = "808 Sub Punch",
                    genreCategoryId = hipHopId,
                    genreName = "Hip-Hop",
                    imageUri = "drawable:img_preset_electronic_1795642439228",
                    bandLevelsCsv = "7.5,6.0,3.0,0.5,-1.5,-1.0,1.5,3.0,2.5,2.0",
                    bassBoostStrength = 760,
                    virtualizerStrength = 220,
                    loudnessGainMb = 250,
                    preampDb = -1.5f,
                    isBuiltIn = true
                ),
                PresetEntity(
                    name = "Crisp Voice & Air",
                    genreCategoryId = vocalId,
                    genreName = "Vocal & Podcast",
                    imageUri = "drawable:img_preset_jazz_1795642438201",
                    bandLevelsCsv = "-5.0,-3.0,-1.0,1.0,2.5,3.5,4.0,3.0,1.5,0.5",
                    bassBoostStrength = 0,
                    virtualizerStrength = 120,
                    loudnessGainMb = 350,
                    preampDb = 1.0f,
                    isBuiltIn = true
                )
            )
            dao.insertPresets(defaults)
        }
    }

    suspend fun addCategory(name: String, description: String, accentColorHex: Long): Long {
        return dao.insertCategory(
            GenreCategoryEntity(
                name = name.trim(),
                description = description.trim(),
                accentColorHex = accentColorHex,
                isBuiltIn = false
            )
        )
    }

    suspend fun deleteCategory(categoryId: Long) {
        dao.deleteCustomCategory(categoryId)
    }

    suspend fun addPreset(
        name: String,
        genreCategoryId: Long,
        genreName: String,
        imageUri: String?,
        bandGainsDb: List<Float>,
        bassBoostStrength: Int,
        virtualizerStrength: Int,
        loudnessGainMb: Int,
        preampDb: Float
    ): Long {
        return dao.insertPreset(
            PresetEntity(
                name = name.trim(),
                genreCategoryId = genreCategoryId,
                genreName = genreName,
                imageUri = imageUri,
                bandLevelsCsv = bandGainsDb.joinToString(",") { "%.2f".format(it) },
                bassBoostStrength = bassBoostStrength.coerceIn(0, 1000),
                virtualizerStrength = virtualizerStrength.coerceIn(0, 1000),
                loudnessGainMb = loudnessGainMb.coerceIn(0, 1500),
                preampDb = preampDb.coerceIn(-12f, 12f),
                isBuiltIn = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updatePresetNameAndImage(id: Long, newName: String, newImageUri: String?) {
        dao.updatePresetNameAndImage(
            id = id,
            newName = newName.trim(),
            newImageUri = newImageUri,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updatePreset(preset: PresetEntity) {
        dao.updatePreset(preset.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePreset(presetId: Long) {
        dao.deletePresetById(presetId)
    }

    suspend fun setEqEnabled(enabled: Boolean) = settingsDataStore.setEqEnabled(enabled)
    suspend fun setGlobalOutputMixEnabled(enabled: Boolean) = settingsDataStore.setGlobalOutputMixEnabled(enabled)
    suspend fun setAutoAttachExternalSessions(enabled: Boolean) = settingsDataStore.setAutoAttachExternalSessions(enabled)
    suspend fun setUseTenBandEngine(enabled: Boolean) = settingsDataStore.setUseTenBandEngine(enabled)
    suspend fun setLimiterEnabled(enabled: Boolean) = settingsDataStore.setLimiterEnabled(enabled)
    suspend fun setPersistentNotificationEnabled(enabled: Boolean) = settingsDataStore.setPersistentNotificationEnabled(enabled)
    suspend fun setBassBoostEnabled(enabled: Boolean) = settingsDataStore.setBassBoostEnabled(enabled)
    suspend fun setVirtualizerEnabled(enabled: Boolean) = settingsDataStore.setVirtualizerEnabled(enabled)
    suspend fun setLoudnessEnabled(enabled: Boolean) = settingsDataStore.setLoudnessEnabled(enabled)

    suspend fun saveLiveParameters(
        activePresetId: Long,
        bandGainsDb: List<Float>,
        bassStrength: Int,
        virtualizerStrength: Int,
        loudnessMb: Int,
        preampDb: Float
    ) {
        settingsDataStore.saveLiveParameters(
            activePresetId = activePresetId,
            bandGainsDb = bandGainsDb,
            bassStrength = bassStrength,
            virtualizerStrength = virtualizerStrength,
            loudnessMb = loudnessMb,
            preampDb = preampDb
        )
    }
}
