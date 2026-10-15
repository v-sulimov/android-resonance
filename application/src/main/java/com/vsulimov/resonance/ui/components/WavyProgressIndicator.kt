package com.vsulimov.resonance.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.ui.theme.Dimens
import kotlin.math.PI
import kotlin.math.sin

/**
 * Expressive Material 3 animated wavy progress indicator.
 *
 * Renders an animated sinusoidal wave path indicating active download or playback progress.
 *
 * @param progress Current progress ratio between 0f and 1f.
 * @param modifier The modifier to be applied to the layout.
 * @param color The stroke color of the active wavy progress line.
 * @param trackColor The background color of the inactive progress track.
 * @param strokeWidth The stroke width of the wavy path.
 */
@Composable
fun WavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    strokeWidth: Dp = 3.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavyProgressTransition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "WavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.paddingMedium)
    ) {
        val width = size.width
        val height = size.height
        val strokePx = strokeWidth.toPx()
        val centerY = height / 2f

        drawLine(
            color = trackColor,
            start = androidx.compose.ui.geometry.Offset(0f, centerY),
            end = androidx.compose.ui.geometry.Offset(width, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        val activeWidth = (width * progress.coerceIn(0f, 1f))
        if (activeWidth > 0f) {
            val wavePath = Path()
            val waveLength = 40.dp.toPx()
            val amplitude = 4.dp.toPx()

            var x = 0f
            wavePath.moveTo(0f, centerY)

            while (x <= activeWidth) {
                val angle = (x / waveLength) * 2 * PI.toFloat() + phaseShift
                val y = centerY + sin(angle) * amplitude
                wavePath.lineTo(x, y)
                x += 2f
            }

            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}
