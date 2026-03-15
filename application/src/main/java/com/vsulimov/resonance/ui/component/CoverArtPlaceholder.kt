package com.vsulimov.resonance.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.ui.theme.LogoGradientDark
import com.vsulimov.resonance.ui.theme.LogoGradientLight

/** Stroke width for the concentric rings. */
private val RingStrokeWidth = 2.dp

/** Ring radii as fractions of the container's minimum dimension. */
private const val OUTER_RING_RADIUS_FRACTION = 0.30f
private const val MIDDLE_RING_RADIUS_FRACTION = 0.22f
private const val INNER_RING_RADIUS_FRACTION = 0.14f
private const val CENTER_DOT_RADIUS_FRACTION = 0.05f

/** Ring opacities. */
private const val OUTER_RING_ALPHA = 0.35f
private const val MIDDLE_RING_ALPHA = 0.25f
private const val INNER_RING_ALPHA = 0.20f
private const val CENTER_DOT_ALPHA = 0.40f

/** Radial highlight for subtle depth. */
private const val HIGHLIGHT_CENTER_FRACTION = 0.3f
private const val HIGHLIGHT_RADIUS_FRACTION = 0.6f
private const val HIGHLIGHT_ALPHA = 0.15f

/**
 * Cover art placeholder inspired by the Resonance logo.
 *
 * Renders a gradient background matching the app's brand colors with
 * concentric rings and a center dot. Designed to be used inside a
 * clipped container (the caller provides the shape and size).
 *
 * @param modifier Modifier applied to the canvas. Typically includes
 *   [fillMaxSize][Modifier.fillMaxSize] so the placeholder fills its container.
 */
@Composable
fun CoverArtPlaceholder(modifier: Modifier = Modifier) {
    val gradientColors = if (isSystemInDarkTheme()) LogoGradientDark else LogoGradientLight

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val minDimension = minOf(size.width, size.height)
        val strokeWidthPx = RingStrokeWidth.toPx()

        drawRect(
            brush = Brush.linearGradient(
                colors = gradientColors,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = HIGHLIGHT_ALPHA), Color.Transparent),
                center = Offset(
                    size.width * HIGHLIGHT_CENTER_FRACTION,
                    size.height * HIGHLIGHT_CENTER_FRACTION
                ),
                radius = minDimension * HIGHLIGHT_RADIUS_FRACTION
            )
        )

        drawCircle(
            color = Color.White.copy(alpha = OUTER_RING_ALPHA),
            radius = minDimension * OUTER_RING_RADIUS_FRACTION,
            center = center,
            style = Stroke(width = strokeWidthPx)
        )

        drawCircle(
            color = Color.White.copy(alpha = MIDDLE_RING_ALPHA),
            radius = minDimension * MIDDLE_RING_RADIUS_FRACTION,
            center = center,
            style = Stroke(width = strokeWidthPx)
        )

        drawCircle(
            color = Color.White.copy(alpha = INNER_RING_ALPHA),
            radius = minDimension * INNER_RING_RADIUS_FRACTION,
            center = center,
            style = Stroke(width = strokeWidthPx)
        )

        drawCircle(
            color = Color.White.copy(alpha = CENTER_DOT_ALPHA),
            radius = minDimension * CENTER_DOT_RADIUS_FRACTION,
            center = center
        )
    }
}
