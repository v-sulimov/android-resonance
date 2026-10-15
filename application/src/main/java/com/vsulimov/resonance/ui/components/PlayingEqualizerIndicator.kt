package com.vsulimov.resonance.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DEFAULT_EQUALIZER_SIZE = 16.dp
private val BAR_WIDTH = 2.5.dp
private val BAR_GAP = 2.dp

/**
 * Animated audio equalizer indicator displaying active track playback status.
 *
 * Displays vertical animated bars when [isPlaying] is true, or static rest bars when paused.
 *
 * @param isPlaying True if audio is actively playing, false to pause bar animations.
 * @param modifier The modifier to be applied to the layout.
 * @param color The color of equalizer bars.
 * @param size The bounding size of the indicator.
 */
@Composable
fun PlayingEqualizerIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = DEFAULT_EQUALIZER_SIZE
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerTransition")

    val bar1HeightFraction by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar1"
        )
    } else {
        remember { mutableFloatStateOf(0.3f) }
    }

    val bar2HeightFraction by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar2"
        )
    } else {
        remember { mutableFloatStateOf(0.5f) }
    }

    val bar3HeightFraction by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar3"
        )
    } else {
        remember { mutableFloatStateOf(0.2f) }
    }

    Row(
        modifier = modifier.size(size),
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(BAR_WIDTH)
                .fillMaxHeight(bar1HeightFraction.coerceIn(0.1f, 1f))
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(BAR_WIDTH)
                .fillMaxHeight(bar2HeightFraction.coerceIn(0.1f, 1f))
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(BAR_WIDTH)
                .fillMaxHeight(bar3HeightFraction.coerceIn(0.1f, 1f))
                .background(color, CircleShape)
        )
    }
}
