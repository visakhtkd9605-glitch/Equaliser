package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.GenreCategoryEntity
import com.example.data.db.PresetEntity
import com.example.ui.EqualizerUiState
import com.example.ui.components.BundledArtworks
import com.example.ui.components.PresetArtwork
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    uiState: EqualizerUiState,
    onToggleGlobalMix: (Boolean) -> Unit,
    onToggleAutoAttach: (Boolean) -> Unit,
    onToggleTenBand: (Boolean) -> Unit,
    onToggleLimiter: (Boolean) -> Unit,
    onTogglePersistentNotification: (Boolean) -> Unit,
    onAddCategory: (String, String, Long) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onAddPresetItem: (String, GenreCategoryEntity, String?, Boolean) -> Unit,
    onUpdateItemNameAndImage: (Long, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val engine = uiState.engineStatus
    var editingPreset by remember { mutableStateOf<PresetEntity?>(null) }

    // Left column state: Add Preset Item
    var newItemName by remember { mutableStateOf("") }
    var newItemCategory by remember(uiState.categories) {
        mutableStateOf(uiState.categories.firstOrNull())
    }
    var newItemImageUri by remember { mutableStateOf<String?>(BundledArtworks.options.first().first) }
    val newItemPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) newItemImageUri = uri.toString()
    }

    // Right column state: Add Genre Category
    var newCatName by remember { mutableStateOf("") }
    var newCatDesc by remember { mutableStateOf("") }
    var newCatColor by remember { mutableLongStateOf(0xFF22D3EE) }
    val colorOptions = listOf(0xFF22D3EE, 0xFFFFB020, 0xFFFF5A79, 0xFF34D399, 0xFFA78BFA)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 860.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ENGINE & CATALOG SETTINGS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // 1. Global Equalizer Service & Audio Routing
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
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Text(
                            text = "GLOBAL AUDIO ROUTING & DSP",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                    }

                    SettingToggleRow(
                        title = "System-Wide Output Mix (Session 0)",
                        subtitle = "Apply equalizer globally to device media & system audio",
                        checked = settings.globalOutputMixEnabled,
                        onCheckedChange = onToggleGlobalMix,
                        testTag = "global_mix_switch"
                    )
                    HorizontalDivider(color = StudioBorder)

                    SettingToggleRow(
                        title = "Auto-Attach Streaming & Player Apps",
                        subtitle = "Intercept AudioEffect control sessions from external players",
                        checked = settings.autoAttachExternalSessions,
                        onCheckedChange = onToggleAutoAttach,
                        testTag = "auto_attach_switch"
                    )
                    HorizontalDivider(color = StudioBorder)

                    SettingToggleRow(
                        title = "10-Band Precision DynamicsProcessing",
                        subtitle = if (engine.tenBandSupported) {
                            "Use 10-band studio post-EQ (API 28+) instead of 5-band"
                        } else {
                            "Device uses 5-band hardware EQ fallback"
                        },
                        checked = settings.useTenBandEngine,
                        onCheckedChange = onToggleTenBand,
                        testTag = "ten_band_switch"
                    )
                    HorizontalDivider(color = StudioBorder)

                    SettingToggleRow(
                        title = "Master Limiter Protection",
                        subtitle = "Prevent digital clipping when boosting sub-bass & loudness",
                        checked = settings.limiterEnabled,
                        onCheckedChange = onToggleLimiter,
                        testTag = "limiter_switch"
                    )
                    HorizontalDivider(color = StudioBorder)

                    SettingToggleRow(
                        title = "Persistent Equalizer Service Notification",
                        subtitle = "Keep foreground service active with quick Bypass toggle",
                        checked = settings.persistentNotificationEnabled,
                        onCheckedChange = onTogglePersistentNotification,
                        testTag = "notification_switch"
                    )
                }
            }

            // 2. Live Attached Sessions & System Streams Diagnostics
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = StudioAmber
                        )
                        Text(
                            text = "ACTIVE SYSTEM AUDIO STREAMS",
                            style = MaterialTheme.typography.labelLarge,
                            color = StudioAmber
                        )
                    }

                    Text(
                        text = "Hardware Bands: ${engine.hardwareBandCount} • Global Mix: ${if (engine.globalMixAttached) "Attached" else "Standby"} • External Sessions: ${engine.externalSessions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    if (engine.activeSystemStreams.isNotEmpty()) {
                        engine.activeSystemStreams.forEach { stream ->
                            Text(
                                text = "• ${stream.usageLabel} (${stream.contentTypeLabel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioMint
                            )
                        }
                    } else {
                        Text(
                            text = "No external playback active right now. Start any streaming app or Universal Player track to see live stream routing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 3. Separate Column Section in Settings to Add Items & Categories
            Text(
                text = "ADD ITEMS & CATEGORIES",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_items_and_categories_columns"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Separate Column 1: Add Preset Item
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_preset_item_column")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Add Item",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text("Item Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_item_name_input")
                        )

                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(uiState.categories, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = newItemCategory?.id == cat.id,
                                    onClick = { newItemCategory = cat },
                                    label = { Text(cat.name, maxLines = 1) }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetArtwork(
                                imageUri = newItemImageUri,
                                contentDescription = "New item image",
                                modifier = Modifier.size(38.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    newItemPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Image", maxLines = 1)
                            }
                        }

                        Button(
                            onClick = {
                                val cat = newItemCategory ?: uiState.categories.firstOrNull()
                                if (newItemName.isNotBlank() && cat != null) {
                                    onAddPresetItem(newItemName.trim(), cat, newItemImageUri, true)
                                    newItemName = ""
                                }
                            },
                            enabled = newItemName.isNotBlank() && uiState.categories.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = StudioObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("add_preset_item_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Add Item")
                        }
                    }
                }

                // Separate Column 2: Add Genre Category
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_category_column")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = null,
                                tint = StudioAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Add Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedTextField(
                            value = newCatName,
                            onValueChange = { newCatName = it },
                            label = { Text("Category Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_category_name_input")
                        )

                        OutlinedTextField(
                            value = newCatDesc,
                            onValueChange = { newCatDesc = it },
                            label = { Text("Tone Notes") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_category_desc_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorOptions.forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (newCatColor == hex) 2.5.dp else 1.dp,
                                            color = if (newCatColor == hex) Color.White else StudioBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { newCatColor = hex }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (newCatName.isNotBlank()) {
                                    onAddCategory(newCatName.trim(), newCatDesc.trim(), newCatColor)
                                    newCatName = ""
                                    newCatDesc = ""
                                }
                            },
                            enabled = newCatName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioAmber,
                                contentColor = StudioObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("add_category_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Add Genre")
                        }
                    }
                }
            }

            // Custom Categories list (with delete for custom categories)
            val customCategories = uiState.categories.filter { !it.isBuiltIn }
            if (customCategories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customCategories, key = { it.id }) { cat ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioSurfaceElevated,
                            border = BorderStroke(1.dp, Color(cat.accentColorHex).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                IconButton(
                                    onClick = { onDeleteCategory(cat.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete category ${cat.name}",
                                        tint = StudioCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Edit Option in Settings to Update Items Name and Images
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "EDIT ITEMS (NAME & IMAGE)",
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioViolet
                    )
                    Text(
                        text = "Update any preset item's display name or cover artwork.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    uiState.presets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioSurfaceElevated)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PresetArtwork(
                                imageUri = preset.imageUri,
                                contentDescription = preset.name,
                                modifier = Modifier.size(44.dp),
                                cornerRadius = 10.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = preset.genreName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = { editingPreset = preset },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("edit_item_button_${preset.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Edit")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    editingPreset?.let { target ->
        EditPresetItemDialog(
            preset = target,
            onDismiss = { editingPreset = null },
            onSave = { newName, newImageUri ->
                onUpdateItemNameAndImage(target.id, newName, newImageUri)
                editingPreset = null
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StudioObsidian,
                checkedTrackColor = ElectricCyan
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun EditPresetItemDialog(
    preset: PresetEntity,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var editedName by remember(preset.id) { mutableStateOf(preset.name) }
    var editedImageUri by remember(preset.id) { mutableStateOf(preset.imageUri) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            editedImageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = {
            Text(
                text = "Edit Item Name & Image",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_name_input")
                )

                Text(
                    text = "Item Image",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PresetArtwork(
                        imageUri = editedImageUri,
                        contentDescription = "Edited item preview",
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 12.dp
                    )
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_item_pick_image_button")
                    ) {
                        Text("Choose Photo")
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
                                    width = if (editedImageUri == uri) 2.dp else 1.dp,
                                    color = if (editedImageUri == uri) ElectricCyan else StudioBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { editedImageUri = uri },
                            cornerRadius = 10.dp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editedName.isNotBlank()) {
                        onSave(editedName.trim(), editedImageUri)
                    }
                },
                enabled = editedName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = StudioObsidian
                ),
                modifier = Modifier.testTag("confirm_edit_item_button")
            ) {
                Text("Update Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
