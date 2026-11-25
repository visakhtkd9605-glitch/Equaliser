package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RotaryKnob(
    label: String,
    valueText: String,
    fraction: Float,
    enabled: Boolean,
    accentColor: Color = ElectricCyan,
    onToggleEnabled: (() -> Unit)? = null,
    onFractionChange: (Float) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val latestFraction by rememberUpdatedState(fraction.coerceIn(0f, 1f))
    val latestOnChange by rememberUpdatedState(onFractionChange)
    var dragStartFraction by remember { mutableFloatStateOf(0f) }

    val activeColor = if (enabled) accentColor else TextMuted

    Column(
        modifier = modifier
            .semantics { contentDescription = "$label control $valueText" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .testTag(testTag)
                .then(
                    if (onToggleEnabled != null) {
                        Modifier.clickable(onClick = onToggleEnabled)
                    } else Modifier
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragStartFraction = latestFraction },
                        onVerticalDrag = { _, dragAmount ->
                            val delta = -dragAmount / 180f
                            dragStartFraction = (dragStartFraction + delta).coerceIn(0f, 1f)
                            latestOnChange(dragStartFraction)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(68.dp)) {
                val strokeW = 6.dp.toPx()
                val pad = strokeW / 2f
                val arcSize = Size(size.width - strokeW, size.height - strokeW)
                val startAngle = 135f
                val maxSweep = 270f

                // Track arc
                drawArc(
                    color = StudioSurfaceHighest,
                    startAngle = startAngle,
                    sweepAngle = maxSweep,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Active value arc
                drawArc(
                    color = activeColor,
                    startAngle = startAngle,
                    sweepAngle = maxSweep * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Inner metallic dial cap
                val radius = (size.minDimension * 0.34f)
                drawCircle(
                    color = StudioSurfaceElevated,
                    radius = radius,
                    center = center
                )

                // Dial pointer dot
                val angleRad = ((startAngle + maxSweep * fraction.coerceIn(0f, 1f)) * PI / 180.0)
                val dotRadius = radius * 0.65f
                val dotCenter = Offset(
                    x = center.x + (dotRadius * cos(angleRad)).toFloat(),
                    y = center.y + (dotRadius * sin(angleRad)).toFloat()
                )
                drawCircle(
                    color = activeColor,
                    radius = 3.5.dp.toPx(),
                    center = dotCenter
                )
            }
        }

        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium,
            color = activeColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1
        )
    }
}
