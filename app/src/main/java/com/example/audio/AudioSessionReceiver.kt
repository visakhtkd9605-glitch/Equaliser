package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.example.SonicEqApplication

class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "External App"
        if (sessionId <= 0) return

        val app = context.applicationContext as? SonicEqApplication ?: return
        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                app.audioEngine.onExternalSessionOpened(sessionId, packageName)
                GlobalEqualizerService.ensureStarted(context)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                app.audioEngine.onExternalSessionClosed(sessionId)
            }
        }
    }
}
