package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.audio.CanonicalEqBands
import com.example.data.db.GenreCategoryEntity
import com.example.data.db.PresetEntity
import com.example.ui.EqualizerUiState
import com.example.ui.components.BundledArtworks
import com.example.ui.components.FrequencyResponseGraph
import com.example.ui.components.PresetArtwork
import com.example.ui.components.RotaryKnob
import com.example.ui.components.VerticalEqFader
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    uiState: EqualizerUiState,
    onToggleEq: () -> Unit,
    onToggleTenBand: (Boolean) -> Unit,
    onTenBandChange: (Int, Float) -> Unit,
    onFiveBandChange: (Int, Float) -> Unit,
    onToggleBassBoost: () -> Unit,
    onBassStrengthChange: (Int) -> Unit,
    onToggleVirtualizer: () -> Unit,
    onVirtualizerStrengthChange: (Int) -> Unit,
    onToggleLoudness: () -> Unit,
    onLoudnessGainChange: (Int) -> Unit,
    onPreampChange: (Float) -> Unit,
    onApplyPreset: (PresetEntity) -> Unit,
    onResetFlat: () -> Unit,
    onSavePreset: (String, GenreCategoryEntity, String?) -> Unit,
    onEnableVisualizer: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = uiState.settings
    val engine = uiState.engineStatus
    val bandGains = settings.parsedBandGainsDb()
    val activePreset = uiState.presets.find { it.id == settings.activePresetId }

    var showSaveDialog by remember { mutableStateOf(false) }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onEnableVisualizer(granted)
    }

    val powerColor by animateColorAsState(
        targetValue = if (settings.eqEnabled) ElectricCyan else TextMuted,
        label = "powerColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Studio Console Header + Master Power Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SONICEQ STUDIO",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (settings.eqEnabled) StudioMint else StudioCoral)
                        )
                        Text(
                            text = engine.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (settings.eqEnabled) ElectricCyanContainer else StudioSurfaceElevated,
                    border = BorderStroke(1.5.dp, powerColor),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("power_toggle_button")
                        .clickable { onToggleEq() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = if (settings.eqEnabled) "Bypass equalizer" else "Enable equalizer",
                            tint = powerColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Active Preset & Engine Mode Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioSurfaceElevated,
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = (activePreset?.name ?: "Custom Curve") +
                                if (uiState.isCustomModified) " • Edited" else "",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                FilterChip(
                    selected = settings.useTenBandEngine,
                    onClick = { onToggleTenBand(!settings.useTenBandEngine) },
                    label = {
                        Text(
                            text = if (settings.useTenBandEngine) "10-Band DSP" else "5-Band Classic",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyanContainer,
                        selectedLabelColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("band_mode_chip")
                )

                FilterChip(
                    selected = engine.visualizerActive,
                    onClick = {
                        if (engine.visualizerActive) {
                            onEnableVisualizer(false)
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onEnableVisualizer(true)
                            } else {
                                audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AutoGraph,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (engine.visualizerActive) "Spectrum ON" else "Live Spectrum",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.testTag("visualizer_toggle_chip")
                )
            }

            // Live Frequency Response Spline Graph
            FrequencyResponseGraph(
                bandGainsDb = bandGains,
                spectrumBins = uiState.spectrumBins,
                eqEnabled = settings.eqEnabled
            )

            // Equalizer Fader Deck Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (settings.useTenBandEngine) {
                                "10-BAND PARAMETRIC CONSOLE (±12 dB)"
                            } else {
                                "5-BAND GRAPHIC CONSOLE (±12 dB)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onResetFlat,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reset_flat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = "Reset bands to 0 dB flat",
                                    tint = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("save_preset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdd,
                                    contentDescription = "Save custom preset",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }

                    if (settings.useTenBandEngine) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(CanonicalEqBands.bands10, key = { it.index }) { spec ->
                                VerticalEqFader(
                                    freqLabel = spec.label,
                                    gainDb = bandGains.getOrElse(spec.index) { 0f },
                                    enabled = settings.eqEnabled,
                                    onGainChange = { newDb -> onTenBandChange(spec.index, newDb) }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CanonicalEqBands.bands5Labels.forEachIndexed { idx, label ->
                                val avgGain = (
                                    bandGains.getOrElse(idx * 2) { 0f } +
                                        bandGains.getOrElse(idx * 2 + 1) { 0f }
                                    ) * 0.5f
                                VerticalEqFader(
                                    freqLabel = label,
                                    gainDb = avgGain,
                                    enabled = settings.eqEnabled,
                                    onGainChange = { newDb -> onFiveBandChange(idx, newDb) },
                                    faderWidth = 56.dp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Genre Preset Carousel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GENRE PRESETS",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.presets, key = { it.id }) { preset ->
                        val selected = preset.id == settings.activePresetId && !uiState.isCustomModified
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) ElectricCyanContainer else StudioSurface,
                            border = BorderStroke(
                                1.2.dp,
                                if (selected) ElectricCyan else StudioBorder
                            ),
                            modifier = Modifier
                                .testTag("quick_preset_${preset.id}")
                                .clickable { onApplyPreset(preset) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PresetArtwork(
                                    imageUri = preset.imageUri,
                                    contentDescription = preset.name,
                                    modifier = Modifier.size(36.dp),
                                    cornerRadius = 8.dp
                                )
                                Column {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.genreName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hardware DSP Effects Rack (Rotary Knobs)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SurroundSound,
                            contentDescription = null,
                            tint = StudioAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ACOUSTIC ENHANCEMENT RACK (TAP TO TOGGLE • DRAG TO DIAL)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RotaryKnob(
                            label = "Bass Boost",
                            valueText = if (settings.bassBoostEnabled) "${settings.liveBassStrength / 10}%" else "OFF",
                            fraction = settings.liveBassStrength / 1000f,
                            enabled = settings.eqEnabled && settings.bassBoostEnabled,
                            accentColor = StudioAmber,
                            onToggleEnabled = onToggleBassBoost,
                            onFractionChange = { onBassStrengthChange((it * 1000f).roundToInt()) },
                            testTag = "bass_knob"
                        )
                        RotaryKnob(
                            label = "Spatial 3D",
                            valueText = if (settings.virtualizerEnabled) "${settings.liveVirtualizerStrength / 10}%" else "OFF",
                            fraction = settings.liveVirtualizerStrength / 1000f,
                            enabled = settings.eqEnabled && settings.virtualizerEnabled,
                            accentColor = ElectricCyan,
                            onToggleEnabled = onToggleVirtualizer,
                            onFractionChange = { onVirtualizerStrengthChange((it * 1000f).roundToInt()) },
                            testTag = "virtualizer_knob"
                        )
                        RotaryKnob(
                            label = "Loudness",
                            valueText = if (settings.loudnessEnabled) "+${"%.1f".format(settings.liveLoudnessMb / 100f)}dB" else "OFF",
                            fraction = settings.liveLoudnessMb / 1500f,
                            enabled = settings.eqEnabled && settings.loudnessEnabled,
                            accentColor = StudioCoral,
                            onToggleEnabled = onToggleLoudness,
                            onFractionChange = { onLoudnessGainChange((it * 1500f).roundToInt()) },
                            testTag = "loudness_knob"
                        )
                        RotaryKnob(
                            label = "Preamp",
                            valueText = "${if (settings.livePreampDb >= 0) "+" else ""}${"%.1f".format(settings.livePreampDb)}dB",
                            fraction = (settings.livePreampDb + 12f) / 24f,
                            enabled = settings.eqEnabled,
                            accentColor = StudioMint,
                            onFractionChange = { onPreampChange((it * 24f) - 12f) },
                            testTag = "preamp_knob"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            categories = uiState.categories,
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, category, imageUri ->
                onSavePreset(name, category, imageUri)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun SavePresetDialog(
    categories: List<GenreCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, GenreCategoryEntity, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var selectedImageUri by remember { mutableStateOf<String?>(BundledArtworks.options.first().first) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = {
            Text(
                text = "Save Custom EQ Preset",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g., Late Night Techno") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_preset_name_input")
                )

                Text(
                    text = "Genre Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategory?.id == cat.id,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Text(
                    text = "Preset Artwork",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetArtwork(
                        imageUri = selectedImageUri,
                        contentDescription = "Selected artwork",
                        modifier = Modifier.size(48.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Pick Image")
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BundledArtworks.options, key = { it.first }) { (uri, label) ->
                        PresetArtwork(
                            imageUri = uri,
                            contentDescription = label,
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = if (selectedImageUri == uri) 2.dp else 1.dp,
                                    color = if (selectedImageUri == uri) ElectricCyan else StudioBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedImageUri = uri },
                            cornerRadius = 10.dp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cat = selectedCategory
                    if (name.isNotBlank() && cat != null) {
                        onConfirm(name.trim(), cat, selectedImageUri)
                    }
                },
                enabled = name.isNotBlank() && selectedCategory != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = StudioObsidian
                ),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text("Save Preset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
