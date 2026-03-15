package com.vsulimov.resonance.ui.screen.main.mix

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.R

private val CardCornerRadius = 24.dp
private val CardPadding = 24.dp
private val TitleToSubtitleSpacing = 8.dp
private val SubtitleToButtonSpacing = 16.dp
private val ButtonIconSize = 20.dp
private val ButtonIconSpacing = 8.dp
private val RingStrokeWidth = 2f
private const val RingAlpha = 0.15f
private const val RingRadiusLarge = 0.7f
private const val RingRadiusMedium = 0.5f
private const val RingRadiusSmall = 0.3f
private const val RingOffsetXFraction = 0.85f
private const val RingOffsetYFraction = -0.1f

/**
 * Hero card promoting library shuffle at the top of the Mix tab.
 *
 * Displays a title, subtitle, and "Shuffle all" action button on a
 * [primaryContainer] surface with decorative concentric rings drawn
 * as a background overlay.
 *
 * @param onShuffleClick Callback invoked when the "Shuffle all" button is tapped.
 * @param modifier Modifier applied to the root [Surface].
 */
@Composable
fun ShuffleHeroCard(
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .drawBehind {
                    val cx = size.width * RingOffsetXFraction
                    val cy = size.height * RingOffsetYFraction
                    val base = size.height
                    val radii = listOf(
                        base * RingRadiusLarge,
                        base * RingRadiusMedium,
                        base * RingRadiusSmall
                    )
                    radii.forEach { radius ->
                        drawCircle(
                            color = ringColor.copy(alpha = RingAlpha),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = RingStrokeWidth)
                        )
                    }
                }
                .padding(CardPadding)
        ) {
            Text(
                text = stringResource(R.string.mix_hero_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(TitleToSubtitleSpacing))
            Text(
                text = stringResource(R.string.mix_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(SubtitleToButtonSpacing))
            Button(onClick = onShuffleClick) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonIconSize)
                )
                Spacer(modifier = Modifier.size(ButtonIconSpacing))
                Text(text = stringResource(R.string.mix_hero_shuffle_all))
            }
        }
    }
}
