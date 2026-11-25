package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soniceq_prefs")

data class EqualizerSettings(
    val eqEnabled: Boolean = true,
    val globalOutputMixEnabled: Boolean = true,
    val autoAttachExternalSessions: Boolean = true,
    val useTenBandEngine: Boolean = true,
    val limiterEnabled: Boolean = true,
    val persistentNotificationEnabled: Boolean = true,
    val bassBoostEnabled: Boolean = true,
    val virtualizerEnabled: Boolean = false,
    val loudnessEnabled: Boolean = false,
    val activePresetId: Long = 1L,
    val liveBandGainsCsv: String = "0,0,0,0,0,0,0,0,0,0",
    val liveBassStrength: Int = 250,
    val liveVirtualizerStrength: Int = 180,
    val liveLoudnessMb: Int = 0,
    val livePreampDb: Float = 0f
) {
    fun parsedBandGainsDb(): List<Float> {
        val parsed = liveBandGainsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        return if (parsed.size == 10) parsed else List(10) { idx -> parsed.getOrElse(idx) { 0f } }
    }
}

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val GLOBAL_MIX = booleanPreferencesKey("global_output_mix")
        val AUTO_ATTACH = booleanPreferencesKey("auto_attach_external")
        val TEN_BAND = booleanPreferencesKey("use_ten_band_engine")
        val LIMITER = booleanPreferencesKey("limiter_enabled")
        val PERSISTENT_NOTIF = booleanPreferencesKey("persistent_notification")
        val BASS_ENABLED = booleanPreferencesKey("bass_boost_enabled")
        val VIRTUALIZER_ENABLED = booleanPreferencesKey("virtualizer_enabled")
        val LOUDNESS_ENABLED = booleanPreferencesKey("loudness_enabled")
        val ACTIVE_PRESET_ID = longPreferencesKey("active_preset_id")
        val LIVE_BANDS = stringPreferencesKey("live_band_gains_csv")
        val LIVE_BASS = intPreferencesKey("live_bass_strength")
        val LIVE_VIRTUALIZER = intPreferencesKey("live_virtualizer_strength")
        val LIVE_LOUDNESS = intPreferencesKey("live_loudness_mb")
        val LIVE_PREAMP = floatPreferencesKey("live_preamp_db")
    }

    val settingsFlow: Flow<EqualizerSettings> = context.dataStore.data.map { prefs ->
        EqualizerSettings(
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: true,
            globalOutputMixEnabled = prefs[Keys.GLOBAL_MIX] ?: true,
            autoAttachExternalSessions = prefs[Keys.AUTO_ATTACH] ?: true,
            useTenBandEngine = prefs[Keys.TEN_BAND] ?: true,
            limiterEnabled = prefs[Keys.LIMITER] ?: true,
            persistentNotificationEnabled = prefs[Keys.PERSISTENT_NOTIF] ?: true,
            bassBoostEnabled = prefs[Keys.BASS_ENABLED] ?: true,
            virtualizerEnabled = prefs[Keys.VIRTUALIZER_ENABLED] ?: false,
            loudnessEnabled = prefs[Keys.LOUDNESS_ENABLED] ?: false,
            activePresetId = prefs[Keys.ACTIVE_PRESET_ID] ?: 1L,
            liveBandGainsCsv = prefs[Keys.LIVE_BANDS] ?: "0,0,0,0,0,0,0,0,0,0",
            liveBassStrength = prefs[Keys.LIVE_BASS] ?: 250,
            liveVirtualizerStrength = prefs[Keys.LIVE_VIRTUALIZER] ?: 180,
            liveLoudnessMb = prefs[Keys.LIVE_LOUDNESS] ?: 0,
            livePreampDb = prefs[Keys.LIVE_PREAMP] ?: 0f
        )
    }

    suspend fun setEqEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EQ_ENABLED] = enabled }
    }

    suspend fun setGlobalOutputMixEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLOBAL_MIX] = enabled }
    }

    suspend fun setAutoAttachExternalSessions(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ATTACH] = enabled }
    }

    suspend fun setUseTenBandEngine(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TEN_BAND] = enabled }
    }

    suspend fun setLimiterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LIMITER] = enabled }
    }

    suspend fun setPersistentNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PERSISTENT_NOTIF] = enabled }
    }

    suspend fun setBassBoostEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BASS_ENABLED] = enabled }
    }

    suspend fun setVirtualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIRTUALIZER_ENABLED] = enabled }
    }

    suspend fun setLoudnessEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOUDNESS_ENABLED] = enabled }
    }

    suspend fun saveLiveParameters(
        activePresetId: Long,
        bandGainsDb: List<Float>,
        bassStrength: Int,
        virtualizerStrength: Int,
        loudnessMb: Int,
        preampDb: Float
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PRESET_ID] = activePresetId
            prefs[Keys.LIVE_BANDS] = bandGainsDb.joinToString(",") { "%.2f".format(it) }
            prefs[Keys.LIVE_BASS] = bassStrength.coerceIn(0, 1000)
            prefs[Keys.LIVE_VIRTUALIZER] = virtualizerStrength.coerceIn(0, 1000)
            prefs[Keys.LIVE_LOUDNESS] = loudnessMb.coerceIn(0, 1500)
            prefs[Keys.LIVE_PREAMP] = preampDb.coerceIn(-12f, 12f)
        }
    }
}
