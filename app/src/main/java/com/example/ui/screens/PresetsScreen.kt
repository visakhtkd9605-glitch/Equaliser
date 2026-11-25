package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.PresetEntity
import com.example.ui.EqualizerUiState
import com.example.ui.components.PresetArtwork
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary

@Composable
fun PresetsScreen(
    uiState: EqualizerUiState,
    onApplyPreset: (PresetEntity) -> Unit,
    onDeletePreset: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    val filteredPresets = remember(uiState.presets, selectedCategoryId) {
        if (selectedCategoryId == null) {
            uiState.presets
        } else {
            uiState.presets.filter { it.genreCategoryId == selectedCategoryId }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GENRE PRESET VAULT",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tap any acoustic curve to apply it globally across streaming and local audio.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("All Genres (${uiState.presets.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyanContainer,
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("filter_genre_all")
                    )
                }
                items(uiState.categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyanContainer,
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("filter_genre_${cat.id}")
                    )
                }
            }

            if (filteredPresets.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No presets in this genre yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Save your custom EQ curve from the Studio tab or add new preset items in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredPresets, key = { it.id }) { preset ->
                        val isActive = preset.id == uiState.settings.activePresetId && !uiState.isCustomModified
                        PresetVaultCard(
                            preset = preset,
                            isActive = isActive,
                            onApply = { onApplyPreset(preset) },
                            onDelete = { onDeletePreset(preset.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetVaultCard(
    preset: PresetEntity,
    isActive: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val gains = remember(preset.bandLevelsCsv) { preset.parsedBandGainsDb() }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) StudioSurfaceElevated else StudioSurface
        ),
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) ElectricCyan else StudioBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preset_card_${preset.id}")
            .clickable { onApply() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PresetArtwork(
                imageUri = preset.imageUri,
                contentDescription = "${preset.name} artwork",
                modifier = Modifier.size(58.dp),
                cornerRadius = 12.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) ElectricCyan else MaterialTheme.colorScheme.onSurface
                    )
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Active preset",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "${preset.genreName} • Bass ${preset.bassBoostStrength / 10}% • Spatial ${preset.virtualizerStrength / 10}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Mini EQ curve sparkline
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(top = 2.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val midY = h * 0.5f
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1f
                    )
                    val path = Path()
                    gains.forEachIndexed { idx, db ->
                        val x = (idx.toFloat() / (gains.size - 1).coerceAtLeast(1)) * w
                        val y = midY - (db.coerceIn(-12f, 12f) / 12f) * (h * 0.42f)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = if (isActive) ElectricCyan else StudioAmber,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
            }

            if (!preset.isBuiltIn) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_preset_${preset.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete preset ${preset.name}",
                        tint = StudioCoral
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}
