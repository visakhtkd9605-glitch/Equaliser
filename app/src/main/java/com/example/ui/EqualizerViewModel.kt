package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.SonicEqApplication
import com.example.audio.EqualizerEngineStatus
import com.example.audio.GlobalEqualizerService
import com.example.audio.ReferenceToneMode
import com.example.audio.TrackFormatInfo
import com.example.data.db.GenreCategoryEntity
import com.example.data.db.PresetEntity
import com.example.data.prefs.EqualizerSettings
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QueueTrack(
    val uri: Uri,
    val title: String,
    val mimeType: String
)

data class PlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentIndex: Int = -1,
    val queue: List<QueueTrack> = emptyList(),
    val formatInfo: TrackFormatInfo = TrackFormatInfo()
)

data class EqualizerUiState(
    val settings: EqualizerSettings = EqualizerSettings(),
    val categories: List<GenreCategoryEntity> = emptyList(),
    val presets: List<PresetEntity> = emptyList(),
    val engineStatus: EqualizerEngineStatus = EqualizerEngineStatus(),
    val spectrumBins: List<Float> = List(24) { 0f },
    val activeToneMode: ReferenceToneMode? = null,
    val playerState: PlayerUiState = PlayerUiState(),
    val isCustomModified: Boolean = false,
    val feedbackMessage: String? = null
)

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SonicEqApplication
    private val repository = app.repository
    private val audioEngine = app.audioEngine
    private val toneTestPlayer = app.toneTestPlayer

    private val _playerState = MutableStateFlow(PlayerUiState())
    private val _isCustomModified = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    val uiState: StateFlow<EqualizerUiState> = combine(
        repository.settings,
        repository.categories,
        repository.presets,
        audioEngine.status,
        audioEngine.spectrumBins,
        toneTestPlayer.activeMode,
        _playerState,
        app.activeTrackFormat,
        _isCustomModified,
        _feedbackMessage
    ) { values ->
        val settings = values[0] as EqualizerSettings
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<GenreCategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val presets = values[2] as List<PresetEntity>
        val engineStatus = values[3] as EqualizerEngineStatus
        @Suppress("UNCHECKED_CAST")
        val spectrum = values[4] as List<Float>
        val toneMode = values[5] as ReferenceToneMode?
        val playerUi = values[6] as PlayerUiState
        val trackFormat = values[7] as TrackFormatInfo
        val customModified = values[8] as Boolean
        val feedback = values[9] as String?

        EqualizerUiState(
            settings = settings,
            categories = categories,
            presets = presets,
            engineStatus = engineStatus,
            spectrumBins = spectrum,
            activeToneMode = toneMode,
            playerState = playerUi.copy(formatInfo = trackFormat),
            isCustomModified = customModified,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EqualizerUiState()
    )

    init {
        GlobalEqualizerService.ensureStarted(app)
        connectMediaController()
        startProgressTicker()
    }

    private fun connectMediaController() {
        val token = SessionToken(app, ComponentName(app, GlobalEqualizerService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val controller = future.get()
                mediaController = controller
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playerState.update {
                            it.copy(
                                isPlaying = isPlaying,
                                durationMs = controller.duration.coerceAtLeast(0L),
                                currentIndex = controller.currentMediaItemIndex
                            )
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _playerState.update {
                            it.copy(
                                currentIndex = controller.currentMediaItemIndex,
                                durationMs = controller.duration.coerceAtLeast(0L)
                            )
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _playerState.update {
                            it.copy(
                                isPlaying = controller.isPlaying,
                                durationMs = controller.duration.coerceAtLeast(0L)
                            )
                        }
                    }
                })
                _playerState.update {
                    it.copy(
                        isConnected = true,
                        isPlaying = controller.isPlaying,
                        durationMs = controller.duration.coerceAtLeast(0L),
                        currentPositionMs = controller.currentPosition.coerceAtLeast(0L)
                    )
                }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressTicker() {
        viewModelScope.launch {
            while (isActive) {
                mediaController?.let { mc ->
                    if (mc.isPlaying) {
                        _playerState.update {
                            it.copy(
                                currentPositionMs = mc.currentPosition.coerceAtLeast(0L),
                                durationMs = mc.duration.coerceAtLeast(0L)
                            )
                        }
                    }
                }
                delay(350)
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun toggleEqEnabled() {
        val next = !uiState.value.settings.eqEnabled
        viewModelScope.launch {
            repository.setEqEnabled(next)
            if (next) GlobalEqualizerService.ensureStarted(app)
        }
    }

    fun setGlobalOutputMixEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setGlobalOutputMixEnabled(enabled)
            if (enabled) GlobalEqualizerService.ensureStarted(app)
        }
    }

    fun setAutoAttachExternalSessions(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoAttachExternalSessions(enabled) }
    }

    fun setUseTenBandEngine(enabled: Boolean) {
        viewModelScope.launch { repository.setUseTenBandEngine(enabled) }
    }

    fun setLimiterEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setLimiterEnabled(enabled) }
    }

    fun setPersistentNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPersistentNotificationEnabled(enabled)
            if (enabled) {
                GlobalEqualizerService.ensureStarted(app)
            } else {
                app.startService(
                    Intent(app, GlobalEqualizerService::class.java).apply {
                        action = GlobalEqualizerService.ACTION_STOP_FOREGROUND_ONLY
                    }
                )
            }
        }
    }

    fun toggleBassBoost() {
        val next = !uiState.value.settings.bassBoostEnabled
        viewModelScope.launch { repository.setBassBoostEnabled(next) }
    }

    fun toggleVirtualizer() {
        val next = !uiState.value.settings.virtualizerEnabled
        viewModelScope.launch { repository.setVirtualizerEnabled(next) }
    }

    fun toggleLoudness() {
        val next = !uiState.value.settings.loudnessEnabled
        viewModelScope.launch { repository.setLoudnessEnabled(next) }
    }

    fun updateTenBandGain(bandIndex: Int, gainDb: Float) {
        val current = uiState.value.settings
        val gains = current.parsedBandGainsDb().toMutableList()
        if (bandIndex in gains.indices) {
            gains[bandIndex] = gainDb.coerceIn(-12f, 12f)
            _isCustomModified.value = true
            viewModelScope.launch {
                repository.saveLiveParameters(
                    activePresetId = current.activePresetId,
                    bandGainsDb = gains,
                    bassStrength = current.liveBassStrength,
                    virtualizerStrength = current.liveVirtualizerStrength,
                    loudnessMb = current.liveLoudnessMb,
                    preampDb = current.livePreampDb
                )
            }
        }
    }

    fun updateFiveBandPair(fiveBandIndex: Int, gainDb: Float) {
        val clamped = gainDb.coerceIn(-12f, 12f)
        val current = uiState.value.settings
        val gains = current.parsedBandGainsDb().toMutableList()
        val firstIdx = (fiveBandIndex * 2).coerceIn(0, 9)
        val secondIdx = (fiveBandIndex * 2 + 1).coerceIn(0, 9)
        gains[firstIdx] = clamped
        gains[secondIdx] = clamped
        _isCustomModified.value = true
        viewModelScope.launch {
            repository.saveLiveParameters(
                activePresetId = current.activePresetId,
                bandGainsDb = gains,
                bassStrength = current.liveBassStrength,
                virtualizerStrength = current.liveVirtualizerStrength,
                loudnessMb = current.liveLoudnessMb,
                preampDb = current.livePreampDb
            )
        }
    }

    fun updateBassStrength(strength: Int) {
        val current = uiState.value.settings
        _isCustomModified.value = true
        viewModelScope.launch {
            repository.saveLiveParameters(
                activePresetId = current.activePresetId,
                bandGainsDb = current.parsedBandGainsDb(),
                bassStrength = strength.coerceIn(0, 1000),
                virtualizerStrength = current.liveVirtualizerStrength,
                loudnessMb = current.liveLoudnessMb,
                preampDb = current.livePreampDb
            )
        }
    }

    fun updateVirtualizerStrength(strength: Int) {
        val current = uiState.value.settings
        _isCustomModified.value = true
        viewModelScope.launch {
            repository.saveLiveParameters(
                activePresetId = current.activePresetId,
                bandGainsDb = current.parsedBandGainsDb(),
                bassStrength = current.liveBassStrength,
                virtualizerStrength = strength.coerceIn(0, 1000),
                loudnessMb = current.liveLoudnessMb,
                preampDb = current.livePreampDb
            )
        }
    }

    fun updateLoudnessGain(gainMb: Int) {
        val current = uiState.value.settings
        _isCustomModified.value = true
        viewModelScope.launch {
            repository.saveLiveParameters(
                activePresetId = current.activePresetId,
                bandGainsDb = current.parsedBandGainsDb(),
                bassStrength = current.liveBassStrength,
                virtualizerStrength = current.liveVirtualizerStrength,
                loudnessMb = gainMb.coerceIn(0, 1500),
                preampDb = current.livePreampDb
            )
        }
    }

    fun updatePreampDb(preampDb: Float) {
        val current = uiState.value.settings
        _isCustomModified.value = true
        viewModelScope.launch {
            repository.saveLiveParameters(
                activePresetId = current.activePresetId,
                bandGainsDb = current.parsedBandGainsDb(),
                bassStrength = current.liveBassStrength,
                virtualizerStrength = current.liveVirtualizerStrength,
                loudnessMb = current.liveLoudnessMb,
                preampDb = preampDb.coerceIn(-12f, 12f)
            )
        }
    }

    fun applyPreset(preset: PresetEntity) {
        _isCustomModified.value = false
        viewModelScope.launch {
            repository.setBassBoostEnabled(preset.bassBoostStrength > 0)
            repository.setVirtualizerEnabled(preset.virtualizerStrength > 0)
            repository.setLoudnessEnabled(preset.loudnessGainMb > 0)
            repository.saveLiveParameters(
                activePresetId = preset.id,
                bandGainsDb = preset.parsedBandGainsDb(),
                bassStrength = preset.bassBoostStrength,
                virtualizerStrength = preset.virtualizerStrength,
                loudnessMb = preset.loudnessGainMb,
                preampDb = preset.preampDb
            )
            _feedbackMessage.value = "Applied \"${preset.name}\""
        }
    }

    fun resetToFlat() {
        val current = uiState.value.settings
        _isCustomModified.value = true
        viewModelScope.launch {
            repository.saveLiveParameters(
                activePresetId = current.activePresetId,
                bandGainsDb = List(10) { 0f },
                bassStrength = 0,
                virtualizerStrength = 0,
                loudnessMb = 0,
                preampDb = 0f
            )
            _feedbackMessage.value = "Bands reset to flat 0.0 dB"
        }
    }

    fun saveCurrentCurveAsPreset(
        name: String,
        category: GenreCategoryEntity,
        imageUri: String?
    ) {
        if (name.isBlank()) return
        val current = uiState.value.settings
        viewModelScope.launch {
            val newId = repository.addPreset(
                name = name,
                genreCategoryId = category.id,
                genreName = category.name,
                imageUri = imageUri,
                bandGainsDb = current.parsedBandGainsDb(),
                bassBoostStrength = current.liveBassStrength,
                virtualizerStrength = current.liveVirtualizerStrength,
                loudnessGainMb = current.liveLoudnessMb,
                preampDb = current.livePreampDb
            )
            _isCustomModified.value = false
            repository.saveLiveParameters(
                activePresetId = newId,
                bandGainsDb = current.parsedBandGainsDb(),
                bassStrength = current.liveBassStrength,
                virtualizerStrength = current.liveVirtualizerStrength,
                loudnessMb = current.liveLoudnessMb,
                preampDb = current.livePreampDb
            )
            _feedbackMessage.value = "Saved preset \"$name\""
        }
    }

    fun addCategory(name: String, description: String, accentColorHex: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(name, description, accentColorHex)
            _feedbackMessage.value = "Added genre \"$name\""
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            _feedbackMessage.value = "Removed custom genre"
        }
    }

    fun addPresetItem(
        name: String,
        category: GenreCategoryEntity,
        imageUri: String?,
        useCurrentCurve: Boolean
    ) {
        if (name.isBlank()) return
        val settings = uiState.value.settings
        val gains = if (useCurrentCurve) settings.parsedBandGainsDb() else List(10) { 0f }
        viewModelScope.launch {
            repository.addPreset(
                name = name,
                genreCategoryId = category.id,
                genreName = category.name,
                imageUri = imageUri,
                bandGainsDb = gains,
                bassBoostStrength = if (useCurrentCurve) settings.liveBassStrength else 150,
                virtualizerStrength = if (useCurrentCurve) settings.liveVirtualizerStrength else 100,
                loudnessGainMb = if (useCurrentCurve) settings.liveLoudnessMb else 0,
                preampDb = if (useCurrentCurve) settings.livePreampDb else 0f
            )
            _feedbackMessage.value = "Added preset item \"$name\""
        }
    }

    fun updatePresetNameAndImage(presetId: Long, newName: String, newImageUri: String?) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updatePresetNameAndImage(presetId, newName, newImageUri)
            _feedbackMessage.value = "Updated \"$newName\""
        }
    }

    fun deletePreset(presetId: Long) {
        viewModelScope.launch {
            repository.deletePreset(presetId)
            _feedbackMessage.value = "Deleted preset"
        }
    }

    fun toggleReferenceTone(mode: ReferenceToneMode) {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
        }
        toneTestPlayer.toggle(mode)
    }

    fun addAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        toneTestPlayer.stop()
        val resolver = app.contentResolver
        val newTracks = uris.map { uri ->
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val displayName = resolveDisplayName(uri)
            val mime = resolver.getType(uri) ?: "audio/*"
            QueueTrack(uri = uri, title = displayName, mimeType = mime)
        }

        val updatedQueue = _playerState.value.queue + newTracks
        val startIdx = if (_playerState.value.queue.isEmpty()) 0 else _playerState.value.queue.size
        _playerState.update { it.copy(queue = updatedQueue) }

        mediaController?.let { mc ->
            val mediaItems = newTracks.map { track ->
                MediaItem.Builder()
                    .setUri(track.uri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).build())
                    .build()
            }
            if (mc.mediaItemCount == 0) {
                mc.setMediaItems(mediaItems, startIdx, 0L)
                mc.prepare()
                mc.play()
            } else {
                mc.addMediaItems(mediaItems)
                mc.seekToDefaultPosition(startIdx)
                mc.prepare()
                mc.play()
            }
        }
        _feedbackMessage.value = "Loaded ${newTracks.size} audio file(s)"
    }

    private fun resolveDisplayName(uri: Uri): String {
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast("/") ?: "Audio Track"
    }

    fun togglePlayPause() {
        toneTestPlayer.stop()
        mediaController?.let { mc ->
            if (mc.isPlaying) mc.pause() else mc.play()
        }
    }

    fun playTrackAt(index: Int) {
        toneTestPlayer.stop()
        mediaController?.let { mc ->
            if (index in 0 until mc.mediaItemCount) {
                mc.seekToDefaultPosition(index)
                mc.prepare()
                mc.play()
            }
        }
    }

    fun skipNext() {
        mediaController?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    }

    fun skipPrevious() {
        mediaController?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun enableVisualizer(enable: Boolean) {
        if (enable) {
            audioEngine.startVisualizer(0)
        } else {
            audioEngine.stopVisualizer()
        }
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
