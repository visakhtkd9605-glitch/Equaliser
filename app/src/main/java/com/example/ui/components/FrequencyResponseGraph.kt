package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface

@Composable
fun FrequencyResponseGraph(
    bandGainsDb: List<Float>,
    spectrumBins: List<Float>,
    eqEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = if (eqEnabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
    val secondaryColor = if (eqEnabled) StudioAmber else Color.Gray

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(StudioObsidian, StudioSurface)
                )
            )
            .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h * 0.5f

            // Reference dB horizontal lines (+12, +6, 0, -6, -12)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            listOf(0.15f, 0.325f, 0.5f, 0.675f, 0.85f).forEachIndexed { idx, frac ->
                val y = h * frac
                drawLine(
                    color = if (idx == 2) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (idx == 2) 1.5f else 1f,
                    pathEffect = if (idx == 2) null else dashEffect
                )
            }

            // Logarithmic vertical grid lines for 10 bands
            val bandCount = bandGainsDb.size.coerceAtLeast(2)
            for (i in 0 until bandCount) {
                val x = (i.toFloat() / (bandCount - 1)) * w
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }

            // Live spectrum analyzer bars behind the EQ curve
            if (spectrumBins.isNotEmpty()) {
                val barSlot = w / spectrumBins.size
                val barW = (barSlot * 0.58f).coerceAtLeast(2f)
                spectrumBins.forEachIndexed { index, amp ->
                    val clamped = amp.coerceIn(0f, 1f)
                    if (clamped > 0.02f) {
                        val barH = clamped * (h * 0.85f)
                        val x = index * barSlot + (barSlot - barW) * 0.5f
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    secondaryColor.copy(alpha = 0.42f),
                                    activeColor.copy(alpha = 0.14f)
                                ),
                                startY = h - barH,
                                endY = h
                            ),
                            topLeft = Offset(x, h - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            // Compute smooth Catmull-Rom / cubic curve points
            val points = bandGainsDb.mapIndexed { index, db ->
                val x = (index.toFloat() / (bandCount - 1)) * w
                val norm = (db.coerceIn(-12f, 12f) / 12f)
                val y = midY - norm * (h * 0.38f)
                Offset(x, y)
            }

            if (points.size >= 2) {
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.lastIndex) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) * 0.5f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = if (eqEnabled) 0.30f else 0.10f),
                            activeColor.copy(alpha = 0.02f)
                        )
                    )
                )

                drawPath(
                    path = linePath,
                    color = activeColor,
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )

                // Draw glowing node markers at each band
                points.forEach { pt ->
                    drawCircle(
                        color = activeColor.copy(alpha = 0.25f),
                        radius = 10f,
                        center = pt
                    )
                    drawCircle(
                        color = activeColor,
                        radius = 4.5f,
                        center = pt
                    )
                }
            }
        }
    }
}
