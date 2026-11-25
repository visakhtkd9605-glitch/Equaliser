package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class ReferenceToneMode(val label: String, val subtitle: String) {
    ACOUSTIC_CHORD("Studio Warm Chord", "65Hz + 196Hz + 440Hz + 1.3kHz + 4.4kHz harmonic stack"),
    PINK_NOISE("Full-Spectrum Pink Noise", "Balanced acoustic noise to audition all 10 EQ bands"),
    SUB_BASS_PULSE("Sub-Bass Groove Pulse", "55Hz & 110Hz rhythmic pulse for bass boost tuning")
}

class ToneTestPlayer(private val audioEngine: EqualizerAudioEngine) {

    private val _activeMode = MutableStateFlow<ReferenceToneMode?>(null)
    val activeMode: StateFlow<ReferenceToneMode?> = _activeMode.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null

    fun toggle(mode: ReferenceToneMode) {
        if (_activeMode.value == mode) {
            stop()
        } else {
            start(mode)
        }
    }

    fun start(mode: ReferenceToneMode) {
        stop()
        _activeMode.value = mode

        playJob = scope.launch {
            val sampleRate = 44100
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
                .build()

            audioTrack = track
            audioEngine.setTestToneSessionId(track.audioSessionId)
            track.play()

            val buffer = ShortArray(2048)
            var sampleIndex = 0L
            // Voss-McCartney / simple IIR pink noise state
            var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0

            while (isActive) {
                for (i in buffer.indices) {
                    val t = sampleIndex.toDouble() / sampleRate
                    val sample = when (mode) {
                        ReferenceToneMode.ACOUSTIC_CHORD -> {
                            val env = 0.75 + 0.25 * sin(2.0 * PI * 0.5 * t)
                            val s = 0.28 * sin(2.0 * PI * 65.41 * t) +
                                0.22 * sin(2.0 * PI * 196.0 * t) +
                                0.20 * sin(2.0 * PI * 440.0 * t) +
                                0.16 * sin(2.0 * PI * 1318.5 * t) +
                                0.12 * sin(2.0 * PI * 4400.0 * t)
                            s * env * 0.45
                        }
                        ReferenceToneMode.PINK_NOISE -> {
                            val white = Random.nextDouble(-1.0, 1.0)
                            b0 = 0.99886 * b0 + white * 0.0555179
                            b1 = 0.99332 * b1 + white * 0.0750759
                            b2 = 0.96900 * b2 + white * 0.1538520
                            b3 = 0.86650 * b3 + white * 0.3104856
                            b4 = 0.55000 * b4 + white * 0.5329522
                            b5 = -0.7616 * b5 - white * 0.0168980
                            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
                            b6 = white * 0.115926
                            (pink * 0.08).coerceIn(-0.85, 0.85)
                        }
                        ReferenceToneMode.SUB_BASS_PULSE -> {
                            val beat = ((t * 2.0) % 1.0)
                            val punch = kotlin.math.exp(-beat * 4.5)
                            val sub = 0.60 * sin(2.0 * PI * 55.0 * t) +
                                0.30 * sin(2.0 * PI * 110.0 * t) +
                                0.10 * sin(2.0 * PI * 220.0 * t)
                            sub * punch * 0.65
                        }
                    }
                    buffer[i] = (sample * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    sampleIndex++
                }
                val written = track.write(buffer, 0, buffer.size)
                if (written < 0) break
            }
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        audioEngine.setTestToneSessionId(0)
        _activeMode.value = null
    }
}
