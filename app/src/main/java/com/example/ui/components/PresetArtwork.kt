package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.StudioSurfaceHighest

object BundledArtworks {
    val options: List<Pair<String, String>> = listOf(
        "drawable:img_studio_hero_1795642438125" to "Studio Console",
        "drawable:img_preset_rock_1795642437515" to "Resonance Rings",
        "drawable:img_preset_jazz_1795642438201" to "Amber Brass",
        "drawable:img_preset_electronic_1795642439228" to "Prism Spectrum",
        "drawable:ic_launcher_art_1795642437262" to "Sonic Emblem"
    )

    fun resolveDrawableRes(imageUri: String?): Int? {
        return when (imageUri) {
            "drawable:img_studio_hero_1795642438125" -> R.drawable.img_studio_hero_1795642438125
            "drawable:img_preset_rock_1795642437515" -> R.drawable.img_preset_rock_1795642437515
            "drawable:img_preset_jazz_1795642438201" -> R.drawable.img_preset_jazz_1795642438201
            "drawable:img_preset_electronic_1795642439228" -> R.drawable.img_preset_electronic_1795642439228
            "drawable:ic_launcher_art_1795642437262" -> R.drawable.ic_launcher_art_1795642437262
            else -> null
        }
    }
}

@Composable
fun PresetArtwork(
    imageUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val drawableRes = BundledArtworks.resolveDrawableRes(imageUri)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(ElectricCyanContainer, StudioSurfaceHighest)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            drawableRes != null -> {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            !imageUri.isNullOrBlank() -> {
                AsyncImage(
                    model = Uri.parse(imageUri),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = contentDescription,
                    tint = ElectricCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
