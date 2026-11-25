package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.prefs.EqualizerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.hypot
import kotlin.math.roundToInt

class EqualizerAudioEngine(private val context: Context) {

    private data class SessionEffects(
        val sessionId: Int,
        val equalizer: Equalizer? = null,
        val bassBoost: BassBoost? = null,
        val virtualizer: VirtualizerBundle? = null,
        val loudnessEnhancer: LoudnessEnhancer? = null,
        val dynamicsProcessing: DynamicsProcessing? = null
    ) {
        fun release() {
            runCatching { equalizer?.release() }
            runCatching { bassBoost?.release() }
            runCatching { virtualizer?.release() }
            runCatching { loudnessEnhancer?.release() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { dynamicsProcessing?.release() }
            }
        }
    }

    private class VirtualizerBundle(sessionId: Int) {
        @Suppress("DEPRECATION")
        private val virtualizer = android.media.audiofx.Virtualizer(0, sessionId)
        val strengthSupported: Boolean = runCatching { virtualizer.strengthSupported }.getOrDefault(false)
        fun setEnabled(enabled: Boolean) {
            runCatching { virtualizer.enabled = enabled }
        }
        fun setStrength(strength: Short) {
            runCatching {
                if (virtualizer.strengthSupported) {
                    virtualizer.setStrength(strength)
                }
            }
        }
        fun release() {
            runCatching { virtualizer.release() }
        }
    }

    private val lock = Any()
    private val activeEffects = mutableMapOf<Int, SessionEffects>()
    private val externalSessionsMap = mutableMapOf<Int, ExternalAudioSession>()

    private var currentSettings = EqualizerSettings()
    private var playerSessionId: Int = 0
    private var testToneSessionId: Int = 0

    private var visualizer: Visualizer? = null
    private var visualizerSessionId: Int = -1

    private val _status = MutableStateFlow(
        EqualizerEngineStatus(
            tenBandSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        )
    )
    val status: StateFlow<EqualizerEngineStatus> = _status.asStateFlow()

    private val _spectrumBins = MutableStateFlow(List(24) { 0f })
    val spectrumBins: StateFlow<List<Float>> = _spectrumBins.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null

    init {
        registerPlaybackObserver()
    }

    private fun registerPlaybackObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val cb = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                    updateActiveStreams(configs ?: emptyList())
                }
            }
            playbackCallback = cb
            runCatching {
                audioManager.registerAudioPlaybackCallback(cb, Handler(Looper.getMainLooper()))
                updateActiveStreams(audioManager.activePlaybackConfigurations)
            }
        }
    }

    private fun updateActiveStreams(configs: List<AudioPlaybackConfiguration>) {
        val mapped = configs.map { cfg ->
            val attrs = cfg.audioAttributes
            ActiveSystemStream(
                usageLabel = usageToString(attrs.usage),
                contentTypeLabel = contentTypeToString(attrs.contentType)
            )
        }.distinct()
        _status.update { it.copy(activeSystemStreams = mapped) }
    }

    private fun usageToString(usage: Int): String = when (usage) {
        AudioAttributes.USAGE_MEDIA -> "Media / Streaming"
        AudioAttributes.USAGE_GAME -> "Game Audio"
        AudioAttributes.USAGE_ALARM -> "Alarm"
        AudioAttributes.USAGE_NOTIFICATION -> "Notification"
        AudioAttributes.USAGE_VOICE_COMMUNICATION -> "Voice Call"
        AudioAttributes.USAGE_ASSISTANT -> "Assistant"
        else -> "System Audio"
    }

    private fun contentTypeToString(contentType: Int): String = when (contentType) {
        AudioAttributes.CONTENT_TYPE_MUSIC -> "Music"
        AudioAttributes.CONTENT_TYPE_SPEECH -> "Speech"
        AudioAttributes.CONTENT_TYPE_MOVIE -> "Movie / Video"
        AudioAttributes.CONTENT_TYPE_SONIFICATION -> "UI / Sound"
        else -> "Stream"
    }

    fun applySettings(settings: EqualizerSettings) {
        synchronized(lock) {
            currentSettings = settings

            // Ensure session 0 global mix attachment matches settings
            if (settings.globalOutputMixEnabled && settings.eqEnabled) {
                ensureSessionAttached(0)
            } else {
                releaseSessionInternal(0)
            }

            // Ensure player & test tone sessions are attached if valid
            if (playerSessionId > 0) {
                ensureSessionAttached(playerSessionId)
            }
            if (testToneSessionId > 0) {
                ensureSessionAttached(testToneSessionId)
            }

            // Re-apply parameters to all active sessions
            activeEffects.keys.toList().forEach { sessionId ->
                applyParametersToSession(sessionId, settings)
            }

            refreshStatusLocked()
        }
    }

    fun setPlayerAudioSessionId(sessionId: Int) {
        synchronized(lock) {
            if (playerSessionId > 0 && playerSessionId != sessionId) {
                releaseSessionInternal(playerSessionId)
            }
            playerSessionId = sessionId
            if (sessionId > 0) {
                ensureSessionAttached(sessionId)
                applyParametersToSession(sessionId, currentSettings)
            }
            refreshStatusLocked()
        }
    }

    fun setTestToneSessionId(sessionId: Int) {
        synchronized(lock) {
            if (testToneSessionId > 0 && testToneSessionId != sessionId) {
                releaseSessionInternal(testToneSessionId)
            }
            testToneSessionId = sessionId
            if (sessionId > 0) {
                ensureSessionAttached(sessionId)
                applyParametersToSession(sessionId, currentSettings)
            }
            refreshStatusLocked()
        }
    }

    fun onExternalSessionOpened(sessionId: Int, packageName: String) {
        if (sessionId <= 0) return
        synchronized(lock) {
            externalSessionsMap[sessionId] = ExternalAudioSession(
                sessionId = sessionId,
                packageName = packageName.ifBlank { "External Player" }
            )
            if (currentSettings.autoAttachExternalSessions) {
                ensureSessionAttached(sessionId)
                applyParametersToSession(sessionId, currentSettings)
            }
            refreshStatusLocked()
        }
    }

    fun onExternalSessionClosed(sessionId: Int) {
        synchronized(lock) {
            externalSessionsMap.remove(sessionId)
            if (sessionId != playerSessionId && sessionId != testToneSessionId) {
                releaseSessionInternal(sessionId)
            }
            refreshStatusLocked()
        }
    }

    private fun ensureSessionAttached(sessionId: Int) {
        if (activeEffects.containsKey(sessionId)) return

        val eq = runCatching { Equalizer(0, sessionId) }.getOrNull()
        val bass = runCatching { BassBoost(0, sessionId) }.getOrNull()
        val virt = runCatching { VirtualizerBundle(sessionId) }.getOrNull()
        val loudness = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        val dp = createDynamicsProcessingIfSupported(sessionId)

        if (eq == null && bass == null && virt == null && loudness == null && dp == null) {
            Log.w(TAG, "No AudioEffects could attach to sessionId=$sessionId")
            return
        }

        activeEffects[sessionId] = SessionEffects(
            sessionId = sessionId,
            equalizer = eq,
            bassBoost = bass,
            virtualizer = virt,
            loudnessEnhancer = loudness,
            dynamicsProcessing = dp
        )
    }

    private fun createDynamicsProcessingIfSupported(sessionId: Int): DynamicsProcessing? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            val builder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                1,
                false, 0,
                false, 0,
                true, 10,
                true
            )
            val postEq = DynamicsProcessing.Eq(true, true, 10)
            CanonicalEqBands.bands10.forEachIndexed { idx, spec ->
                postEq.getBand(idx).apply {
                    isEnabled = true
                    cutoffFrequency = spec.cutoffHz
                    gain = 0f
                }
            }
            builder.setPostEqAllChannelsTo(postEq)
            val limiter = DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -0.5f, 0f)
            builder.setLimiterAllChannelsTo(limiter)
            DynamicsProcessing(0, sessionId, builder.build())
        }.getOrNull()
    }

    private fun applyParametersToSession(sessionId: Int, settings: EqualizerSettings) {
        val fx = activeEffects[sessionId] ?: return
        val gains10 = settings.parsedBandGainsDb()
        val masterEnabled = settings.eqEnabled

        // Choose DynamicsProcessing (10-band) on API 28+ when enabled & available, else Equalizer (5-band)
        val useDp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            settings.useTenBandEngine &&
            fx.dynamicsProcessing != null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && fx.dynamicsProcessing != null) {
            runCatching {
                val dp = fx.dynamicsProcessing
                dp.enabled = masterEnabled && useDp
                if (masterEnabled && useDp) {
                    dp.setInputGainAllChannelsTo(settings.livePreampDb)
                    val limiter = DynamicsProcessing.Limiter(
                        true,
                        settings.limiterEnabled,
                        0,
                        1f,
                        60f,
                        10f,
                        -0.5f,
                        0f
                    )
                    dp.setLimiterAllChannelsTo(limiter)
                    CanonicalEqBands.bands10.forEachIndexed { idx, spec ->
                        val band = DynamicsProcessing.EqBand(
                            true,
                            spec.cutoffHz,
                            gains10.getOrElse(idx) { 0f }.coerceIn(-12f, 12f)
                        )
                        dp.setPostEqBandAllChannelsTo(idx, band)
                    }
                }
            }
        }

        fx.equalizer?.let { eq ->
            runCatching {
                eq.enabled = masterEnabled && !useDp
                if (masterEnabled && !useDp) {
                    val range = eq.bandLevelRange
                    val minMb = range[0].toInt()
                    val maxMb = range[1].toInt()
                    val numBands = eq.numberOfBands.toInt()
                    for (bandIdx in 0 until numBands) {
                        val centerHz = eq.getCenterFreq(bandIdx.toShort()) / 1000f
                        val targetDb = CanonicalEqBands.sampleGainAtHz(gains10, centerHz) +
                            (settings.livePreampDb * 0.5f)
                        val targetMb = (targetDb * 100f).roundToInt().coerceIn(minMb, maxMb)
                        eq.setBandLevel(bandIdx.toShort(), targetMb.toShort())
                    }
                }
            }
        }

        fx.bassBoost?.let { bass ->
            runCatching {
                bass.enabled = masterEnabled && settings.bassBoostEnabled
                if (bass.strengthSupported && masterEnabled && settings.bassBoostEnabled) {
                    bass.setStrength(settings.liveBassStrength.coerceIn(0, 1000).toShort())
                }
            }
        }

        fx.virtualizer?.let { virt ->
            runCatching {
                virt.setEnabled(masterEnabled && settings.virtualizerEnabled)
                if (virt.strengthSupported && masterEnabled && settings.virtualizerEnabled) {
                    virt.setStrength(settings.liveVirtualizerStrength.coerceIn(0, 1000).toShort())
                }
            }
        }

        fx.loudnessEnhancer?.let { loud ->
            runCatching {
                loud.enabled = masterEnabled && settings.loudnessEnabled
                if (masterEnabled && settings.loudnessEnabled) {
                    loud.setTargetGain(settings.liveLoudnessMb.coerceIn(0, 1500))
                }
            }
        }
    }

    private fun releaseSessionInternal(sessionId: Int) {
        activeEffects.remove(sessionId)?.release()
    }

    private fun refreshStatusLocked() {
        val sampleFx = activeEffects[playerSessionId]
            ?: activeEffects[testToneSessionId]
            ?: activeEffects.values.firstOrNull()

        val eq = sampleFx?.equalizer
        val hwBands = eq?.let {
            runCatching {
                val count = it.numberOfBands.toInt()
                (0 until count).map { idx -> it.getCenterFreq(idx.toShort()) / 1000 }
            }.getOrNull()
        } ?: listOf(60, 230, 910, 3600, 14000)

        val globalAttached = activeEffects.containsKey(0)
        val tenBandSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            activeEffects.values.any { it.dynamicsProcessing != null }
        val tenBandActive = tenBandSupported && currentSettings.useTenBandEngine

        val statusMsg = when {
            !currentSettings.eqEnabled -> "DSP Bypassed — Pass-through Active"
            globalAttached && externalSessionsMap.isNotEmpty() ->
                "Global Mix + ${externalSessionsMap.size} App Session(s) Active"
            globalAttached -> "Global System Output Mix (Session 0) Active"
            externalSessionsMap.isNotEmpty() ->
                "${externalSessionsMap.size} External App Session(s) Active"
            playerSessionId > 0 -> "Universal Player Audio Session #$playerSessionId Active"
            testToneSessionId > 0 -> "Reference Tone Session #$testToneSessionId Active"
            else -> "Standby — Waiting for Audio Stream"
        }

        _status.update { prev ->
            prev.copy(
                eqEnabled = currentSettings.eqEnabled,
                globalMixSupported = globalAttached || !currentSettings.globalOutputMixEnabled,
                globalMixAttached = globalAttached,
                tenBandSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
                tenBandActive = tenBandActive,
                hardwareBandCount = hwBands.size,
                hardwareBandFrequenciesHz = hwBands,
                bassBoostSupported = activeEffects.values.any { it.bassBoost != null },
                virtualizerSupported = activeEffects.values.any { it.virtualizer != null },
                loudnessSupported = activeEffects.values.any { it.loudnessEnhancer != null },
                externalSessions = externalSessionsMap.values.sortedByDescending { it.attachedAtMs },
                playerSessionId = playerSessionId,
                testToneSessionId = testToneSessionId,
                statusMessage = statusMsg
            )
        }
    }

    fun startVisualizer(preferredSessionId: Int) {
        val targetSession = when {
            preferredSessionId > 0 -> preferredSessionId
            playerSessionId > 0 -> playerSessionId
            testToneSessionId > 0 -> testToneSessionId
            else -> 0
        }
        if (visualizer != null && visualizerSessionId == targetSession) return
        stopVisualizer()

        runCatching {
            val maxRate = Visualizer.getMaxCaptureRate()
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(256)
            val vis = Visualizer(targetSession).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || fft.size < 4) return
                            val bins = MutableList(24) { 0f }
                            val usablePairs = (fft.size / 2).coerceAtLeast(1)
                            for (i in 0 until 24) {
                                val fftIdx = ((i + 1) * usablePairs / 26).coerceIn(1, usablePairs - 1)
                                val re = fft[2 * fftIdx].toFloat()
                                val im = fft[2 * fftIdx + 1].toFloat()
                                val mag = hypot(re, im) / 128f
                                bins[i] = mag.coerceIn(0f, 1f)
                            }
                            _spectrumBins.value = bins
                        }
                    },
                    maxRate / 2,
                    false,
                    true
                )
                enabled = true
            }
            visualizer = vis
            visualizerSessionId = targetSession
            _status.update { it.copy(visualizerActive = true) }
        }.onFailure {
            _status.update { it.copy(visualizerActive = false) }
        }
    }

    fun stopVisualizer() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        visualizerSessionId = -1
        _spectrumBins.value = List(24) { 0f }
        _status.update { it.copy(visualizerActive = false) }
    }

    fun releaseAll() {
        stopVisualizer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackCallback?.let { runCatching { audioManager.unregisterAudioPlaybackCallback(it) } }
        }
        synchronized(lock) {
            activeEffects.values.forEach { it.release() }
            activeEffects.clear()
        }
    }

    companion object {
        private const val TAG = "EqualizerAudioEngine"
    }
}
