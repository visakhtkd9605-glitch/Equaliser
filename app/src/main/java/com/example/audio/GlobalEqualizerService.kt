package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.SonicEqApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GlobalEqualizerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val app = applicationContext as SonicEqApplication
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                app.audioEngine.setPlayerAudioSessionId(audioSessionId)
            }

            override fun onTracksChanged(tracks: Tracks) {
                app.updateActiveTrackFormat(player)
            }
        })

        app.audioEngine.setPlayerAudioSessionId(player.audioSessionId)
        exoPlayer = player

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()

        serviceScope.launch {
            app.repository.settings.collect { settings ->
                if (settings.persistentNotificationEnabled && settings.eqEnabled) {
                    promoteForegroundNotification(
                        title = "SonicEQ Global Audio Engine",
                        subtitle = if (settings.globalOutputMixEnabled) {
                            "System-wide EQ & DSP active"
                        } else {
                            "App session equalizer active"
                        },
                        eqEnabled = settings.eqEnabled
                    )
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as SonicEqApplication
        when (intent?.action) {
            ACTION_TOGGLE_EQ -> {
                serviceScope.launch {
                    val current = app.repository.settings.first()
                    app.repository.setEqEnabled(!current.eqEnabled)
                }
            }
            ACTION_START_GLOBAL_EQ -> {
                promoteForegroundNotification(
                    title = "SonicEQ Global Audio Engine",
                    subtitle = "Processing system-wide audio streams",
                    eqEnabled = true
                )
            }
            ACTION_STOP_FOREGROUND_ONLY -> {
                if (exoPlayer?.isPlaying != true) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun promoteForegroundNotification(title: String, subtitle: String, eqEnabled: Boolean) {
        val notification = buildNotification(title, subtitle, eqEnabled)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(title: String, subtitle: String, eqEnabled: Boolean): Notification {
        val openAppPending = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val togglePending = PendingIntent.getService(
            this,
            11,
            Intent(this, GlobalEqualizerService::class.java).apply { action = ACTION_TOGGLE_EQ },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleLabel = if (eqEnabled) "Bypass EQ" else "Enable EQ"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppPending)
            .addAction(0, toggleLabel, togglePending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Global Equalizer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps SonicEQ processing active for system-wide and streaming audio"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "soniceq_global_channel"
        private const val NOTIFICATION_ID = 4401
        const val ACTION_START_GLOBAL_EQ = "com.example.action.START_GLOBAL_EQ"
        const val ACTION_TOGGLE_EQ = "com.example.action.TOGGLE_EQ"
        const val ACTION_STOP_FOREGROUND_ONLY = "com.example.action.STOP_FOREGROUND_ONLY"

        fun ensureStarted(context: Context) {
            val intent = Intent(context, GlobalEqualizerService::class.java).apply {
                action = ACTION_START_GLOBAL_EQ
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }
}
