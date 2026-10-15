package com.vsulimov.resonance.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.R
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.theme.Dimens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reusable M3 component displaying current download status indicator.
 *
 * @param status Current [DownloadStatus] state.
 * @param onDownloadClick Callback triggered when user taps to download or retry.
 * @param onRemoveClick Callback triggered when user taps downloaded icon to remove download.
 * @param modifier The modifier to be applied to the layout.
 * @param progressProvider Optional byte progress ratio provider between 0f and 1f when status is DOWNLOADING.
 * @param isQueuePaused True if the download queue is currently paused.
 */
@Composable
fun DownloadStateIndicator(
    status: DownloadStatus,
    onDownloadClick: (() -> Unit)? = null,
    onRemoveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    progressProvider: (() -> Float)? = null,
    isQueuePaused: Boolean = false
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isQueuePaused && (status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING) -> {
                Icon(
                    imageVector = Icons.Outlined.PauseCircle,
                    contentDescription = stringResource(R.string.cd_downloads_paused),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconMedium)
                )
            }

            status == DownloadStatus.NOT_DOWNLOADED -> {
                if (onDownloadClick != null) {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(Dimens.iconLarge)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = stringResource(R.string.cd_download),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            status == DownloadStatus.QUEUED -> {
                CircularWavyProgressIndicator(
                    progressProvider = null,
                    modifier = Modifier.size(Dimens.iconMedium)
                )
            }

            status == DownloadStatus.DOWNLOADING -> {
                CircularWavyProgressIndicator(
                    progressProvider = progressProvider,
                    modifier = Modifier.size(Dimens.iconMedium)
                )
            }

            status == DownloadStatus.DOWNLOADED -> {
                if (onRemoveClick != null) {
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.size(Dimens.iconLarge)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.cd_downloaded),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.cd_downloaded),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.iconMedium)
                    )
                }
            }

            status == DownloadStatus.FAILED -> {
                if (onDownloadClick != null) {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(Dimens.iconLarge)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = stringResource(R.string.cd_download_failed),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = stringResource(R.string.cd_download_failed),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimens.iconMedium)
                    )
                }
            }
        }
    }
}

/**
 * Smooth animated circular wavy progress indicator for active downloading state.
 *
 * @param progressProvider Optional progress ratio provider between 0f and 1f, or null for indeterminate spinning wavy animation.
 * @param modifier The modifier to be applied to the layout.
 * @param color The stroke color of the active wavy progress line.
 * @param trackColor The background color of the inactive progress track.
 * @param strokeWidth The stroke width of the wavy path.
 */
@Composable
fun CircularWavyProgressIndicator(
    progressProvider: (() -> Float)? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer,
    strokeWidth: Dp = 2.dp
) {
    val isIndeterminate = progressProvider == null

    val infiniteTransition = rememberInfiniteTransition(label = "WavyCircularTransition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing)
        ),
        label = "WavePhase"
    )

    val rotationAngle by if (isIndeterminate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing)
            ),
            label = "WaveRotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier.size(Dimens.iconMedium)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokePx = strokeWidth.toPx()
        val radius = (size.minDimension - strokePx) / 2f
        val currentProgress = progressProvider?.invoke() ?: 1f

        if (!isIndeterminate) {
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )
        }

        val sweepAngle = if (isIndeterminate) 300f else currentProgress * 360f
        val startAngle = -90f + rotationAngle
        val waveCount = 5
        val waveAmplitude = 1.0.dp.toPx()

        val path = Path()
        val steps = 80
        var isFirst = true

        for (i in 0..steps) {
            val fraction = i.toFloat() / steps
            val angleDeg = startAngle + fraction * sweepAngle
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val waveOffset = (sin(fraction * waveCount * 2 * PI + phaseShift) * waveAmplitude).toFloat()
            val r = radius + waveOffset

            val x = (center.x + r * cos(angleRad)).toFloat()
            val y = (center.y + r * sin(angleRad)).toFloat()

            if (isFirst) {
                path.moveTo(x, y)
                isFirst = false
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}
