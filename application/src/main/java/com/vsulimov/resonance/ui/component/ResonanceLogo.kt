package com.vsulimov.resonance.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.ui.theme.LogoGradientDark
import com.vsulimov.resonance.ui.theme.LogoGradientLight

/** Default size for the logo. */
private val LogoDefaultSize = 120.dp

/** Corner radius for the logo's rounded square clip. */
private val LogoCornerRadius = 32.dp

/** Stroke width for the concentric rings. */
private val RingStrokeWidth = 2.5.dp

// Ring radii as fractions of the container's minimum dimension.
private const val OUTER_RING_RADIUS_FRACTION = 0.33f
private const val MIDDLE_RING_RADIUS_FRACTION = 0.24f
private const val INNER_RING_RADIUS_FRACTION = 0.15f
private const val CENTER_DOT_RADIUS_FRACTION = 0.06f

// Ring opacities (decreasing from outer to inner).
private const val OUTER_RING_ALPHA = 0.9f
private const val MIDDLE_RING_ALPHA = 0.7f
private const val INNER_RING_ALPHA = 0.5f
private const val CENTER_DOT_ALPHA = 0.95f

// Radial highlight positioning.
private const val HIGHLIGHT_CENTER_FRACTION = 0.3f
private const val HIGHLIGHT_RADIUS_FRACTION = 0.6f
private const val HIGHLIGHT_ALPHA = 0.2f

/**
 * Resonance app logo rendered as a gradient rounded square with concentric rings.
 *
 * The gradient direction is 135 degrees (top-left to bottom-right). Three concentric
 * rings are drawn at decreasing opacity, with a solid center dot.
 *
 * @param modifier Modifier applied to the canvas.
 * @param size Logo dimensions (width and height).
 */
@Composable
fun ResonanceLogo(
    modifier: Modifier = Modifier,
    size: Dp = LogoDefaultSize
) {
    val gradientColors = if (isSystemInDarkTheme()) LogoGradientDark else LogoGradientLight

    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(LogoCornerRadius))
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val minDimension = minOf(this.size.width, this.size.height)
        val strokeWidthPx = RingStrokeWidth.toPx()

        drawRect(
            brush = Brush.linearGradient(
                colors = gradientColors,
                start = Offset.Zero,
                end = Offset(this.size.width, this.size.height)
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = HIGHLIGHT_ALPHA), Color.Transparent),
                center = Offset(
                    this.size.width * HIGHLIGHT_CENTER_FRACTION,
                    this.size.height * HIGHLIGHT_CENTER_FRACTION
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
