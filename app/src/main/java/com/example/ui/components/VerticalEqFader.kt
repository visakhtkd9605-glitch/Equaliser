package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun VerticalEqFader(
    freqLabel: String,
    gainDb: Float,
    enabled: Boolean,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    faderHeight: Dp = 176.dp,
    faderWidth: Dp = 48.dp
) {
    val latestGain by rememberUpdatedState(gainDb)
    val latestOnChange by rememberUpdatedState(onGainChange)
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val activeAccent = when {
        !enabled -> TextMuted
        gainDb > 6f -> StudioAmber
        else -> ElectricCyan
    }

    Column(
        modifier = modifier.width(faderWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (gainDb >= 0f) "+${"%.1f".format(gainDb)}" else "%.1f".format(gainDb),
            style = MaterialTheme.typography.labelMedium,
            color = if (abs(gainDb) < 0.1f) TextSecondary else activeAccent,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Canvas(
            modifier = Modifier
                .width(faderWidth)
                .height(faderHeight)
                .padding(vertical = 8.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { latestOnChange(0f) },
                        onTap = { offset ->
                            val usableH = size.height.toFloat().coerceAtLeast(1f)
                            val frac = 1f - (offset.y / usableH).coerceIn(0f, 1f)
                            val newDb = (frac * 24f) - 12f
                            latestOnChange(newDb.coerceIn(-12f, 12f))
                        }
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragAccumulator = latestGain
                        },
                        onVerticalDrag = { _, dragAmount ->
                            val usableH = size.height.toFloat().coerceAtLeast(1f)
                            val deltaDb = (-dragAmount / usableH) * 24f
                            dragAccumulator = (dragAccumulator + deltaDb).coerceIn(-12f, 12f)
                            latestOnChange(dragAccumulator)
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val midY = h * 0.5f

            // Subtle scale tick marks on sides
            for (step in -2..2) {
                val y = midY - (step / 2f) * (h * 0.46f)
                drawLine(
                    color = Color.White.copy(alpha = if (step == 0) 0.25f else 0.10f),
                    start = Offset(cx - 14f, y),
                    end = Offset(cx + 14f, y),
                    strokeWidth = if (step == 0) 2f else 1f
                )
            }

            // Track background
            val trackW = 6.dp.toPx()
            drawRoundRect(
                color = StudioSurfaceHighest,
                topLeft = Offset(cx - trackW * 0.5f, 0f),
                size = Size(trackW, h),
                cornerRadius = CornerRadius(trackW, trackW)
            )

            // Active fill from 0dB center to current thumb
            val norm = (gainDb.coerceIn(-12f, 12f) / 12f)
            val thumbY = (midY - norm * (h * 0.46f)).coerceIn(8f, h - 8f)
            val fillTop = minOf(midY, thumbY)
            val fillBottom = maxOf(midY, thumbY)

            if (fillBottom - fillTop > 1f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(activeAccent, activeAccent.copy(alpha = 0.55f))
                    ),
                    topLeft = Offset(cx - trackW * 0.5f, fillTop),
                    size = Size(trackW, fillBottom - fillTop),
                    cornerRadius = CornerRadius(trackW, trackW)
                )
            }

            // Thumb glow + studio fader cap
            drawCircle(
                color = activeAccent.copy(alpha = if (enabled) 0.25f else 0.08f),
                radius = 15.dp.toPx(),
                center = Offset(cx, thumbY)
            )
            drawRoundRect(
                color = if (enabled) Color(0xFFF8FAFC) else Color(0xFF64748B),
                topLeft = Offset(cx - 13.dp.toPx(), thumbY - 7.dp.toPx()),
                size = Size(26.dp.toPx(), 14.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawLine(
                color = activeAccent,
                start = Offset(cx - 8.dp.toPx(), thumbY),
                end = Offset(cx + 8.dp.toPx(), thumbY),
                strokeWidth = 2.dp.toPx()
            )
        }

        Text(
            text = freqLabel,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
    }
}
