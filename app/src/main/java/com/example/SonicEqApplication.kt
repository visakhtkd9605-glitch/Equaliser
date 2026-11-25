package com.example

import android.app.Application
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.audio.AudioSessionReceiver
import com.example.audio.EqualizerAudioEngine
import com.example.audio.ToneTestPlayer
import com.example.audio.TrackFormatInfo
import com.example.data.db.EqualizerDatabase
import com.example.data.prefs.SettingsDataStore
import com.example.data.repository.EqualizerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SonicEqApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: EqualizerDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var repository: EqualizerRepository
        private set
    lateinit var audioEngine: EqualizerAudioEngine
        private set
    lateinit var toneTestPlayer: ToneTestPlayer
        private set

    private val _activeTrackFormat = MutableStateFlow(TrackFormatInfo())
    val activeTrackFormat: StateFlow<TrackFormatInfo> = _activeTrackFormat.asStateFlow()

    private val dynamicReceiver = AudioSessionReceiver()

    override fun onCreate() {
        super.onCreate()
        database = EqualizerDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        repository = EqualizerRepository(database.equalizerDao(), settingsDataStore)
        audioEngine = EqualizerAudioEngine(this)
        toneTestPlayer = ToneTestPlayer(audioEngine)

        // Seed default genres/presets and keep AudioEngine synced with DataStore settings
        appScope.launch {
            repository.seedDefaultsIfNeeded()
            repository.settings.collect { settings ->
                audioEngine.applySettings(settings)
            }
        }

        // Dynamic registration for system audio session broadcasts
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                this,
                dynamicReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(dynamicReceiver, filter)
        }
    }

    @OptIn(UnstableApi::class)
    fun updateActiveTrackFormat(player: Player) {
        val currentItem = player.currentMediaItem
        val title = currentItem?.mediaMetadata?.title?.toString()
            ?: currentItem?.localConfiguration?.uri?.lastPathSegment
            ?: "Unknown Track"

        var mime = "Universal Audio"
        var codec = "--"
        var sampleRate = 0
        var channels = 0
        var bitrate = 0

        for (group in player.currentTracks.groups) {
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) {
                    val format = group.getTrackFormat(i)
                    if (format.sampleMimeType?.startsWith("audio") == true) {
                        mime = format.sampleMimeType ?: mime
                        codec = format.codecs ?: format.sampleMimeType?.substringAfter("/")?.uppercase() ?: "--"
                        sampleRate = format.sampleRate.coerceAtLeast(0)
                        channels = format.channelCount.coerceAtLeast(0)
                        bitrate = format.bitrate.coerceAtLeast(0)
                    }
                }
            }
        }

        _activeTrackFormat.value = TrackFormatInfo(
            title = title,
            mimeType = mime,
            codec = codec,
            sampleRateHz = sampleRate,
            channelCount = channels,
            bitrateBps = bitrate
        )
    }

    override fun onTerminate() {
        runCatching { unregisterReceiver(dynamicReceiver) }
        toneTestPlayer.stop()
        audioEngine.releaseAll()
        super.onTerminate()
    }
}
