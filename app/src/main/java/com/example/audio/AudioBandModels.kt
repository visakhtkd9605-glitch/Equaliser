package com.example.audio

data class EqBandSpec(
    val index: Int,
    val centerHz: Float,
    val label: String,
    val cutoffHz: Float
)

object CanonicalEqBands {
    val bands10 = listOf(
        EqBandSpec(0, 31f, "31", 44f),
        EqBandSpec(1, 62f, "62", 88f),
        EqBandSpec(2, 125f, "125", 177f),
        EqBandSpec(3, 250f, "250", 355f),
        EqBandSpec(4, 500f, "500", 710f),
        EqBandSpec(5, 1000f, "1k", 1420f),
        EqBandSpec(6, 2000f, "2k", 2840f),
        EqBandSpec(7, 4000f, "4k", 5680f),
        EqBandSpec(8, 8000f, "8k", 11360f),
        EqBandSpec(9, 16000f, "16k", 20000f)
    )

    val bands5Labels = listOf("60", "230", "910", "3.6k", "14k")

    /**
     * Maps 10 canonical dB band gains to an arbitrary hardware band center frequency (in Hz)
     * via log-frequency linear interpolation.
     */
    fun sampleGainAtHz(gains10: List<Float>, targetHz: Float): Float {
        if (gains10.isEmpty()) return 0f
        if (targetHz <= bands10.first().centerHz) return gains10.first()
        if (targetHz >= bands10.last().centerHz) return gains10.last()

        val logTarget = kotlin.math.ln(targetHz.toDouble())
        for (i in 0 until bands10.lastIndex) {
            val f0 = bands10[i].centerHz
            val f1 = bands10[i + 1].centerHz
            if (targetHz in f0..f1) {
                val log0 = kotlin.math.ln(f0.toDouble())
                val log1 = kotlin.math.ln(f1.toDouble())
                val t = ((logTarget - log0) / (log1 - log0)).toFloat().coerceIn(0f, 1f)
                val g0 = gains10.getOrElse(i) { 0f }
                val g1 = gains10.getOrElse(i + 1) { 0f }
                return g0 + (g1 - g0) * t
            }
        }
        return 0f
    }
}

data class ExternalAudioSession(
    val sessionId: Int,
    val packageName: String,
    val attachedAtMs: Long = System.currentTimeMillis()
)

data class ActiveSystemStream(
    val usageLabel: String,
    val contentTypeLabel: String
)

data class EqualizerEngineStatus(
    val eqEnabled: Boolean = true,
    val globalMixSupported: Boolean = false,
    val globalMixAttached: Boolean = false,
    val tenBandSupported: Boolean = false,
    val tenBandActive: Boolean = false,
    val hardwareBandCount: Int = 5,
    val hardwareBandFrequenciesHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
    val bassBoostSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
    val loudnessSupported: Boolean = false,
    val externalSessions: List<ExternalAudioSession> = emptyList(),
    val playerSessionId: Int = 0,
    val testToneSessionId: Int = 0,
    val activeSystemStreams: List<ActiveSystemStream> = emptyList(),
    val visualizerActive: Boolean = false,
    val statusMessage: String = "Initializing DSP Engine"
)

data class TrackFormatInfo(
    val title: String = "No track loaded",
    val mimeType: String = "Ready for any format",
    val codec: String = "--",
    val sampleRateHz: Int = 0,
    val channelCount: Int = 0,
    val bitrateBps: Int = 0
)
