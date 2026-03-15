package com.vsulimov.resonance

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.drawToBitmap
import com.vsulimov.resonance.domain.model.ThemePreference
import com.vsulimov.resonance.ui.navigation.AppNavigation
import com.vsulimov.resonance.ui.theme.ResonanceTheme
import kotlin.math.hypot

private const val REVEAL_ANIMATION_DURATION_MS = 500

/**
 * Single-activity entry point for the Resonance application.
 *
 * Sets up edge-to-edge display and hosts the Compose UI tree.
 * Observes the user's [ThemePreference] from the preferences repository
 * to apply the correct color scheme before the first frame renders.
 *
 * Theme changes are animated with a circular reveal effect: the current
 * screen is captured as a bitmap, the new theme is applied underneath,
 * and a growing circular hole reveals the new colors from the screen center.
 *
 * [AppNavigation] handles routing between the onboarding flow
 * (for unauthenticated users) and the main application shell
 * (for authenticated users), including global session expiry handling.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val application = applicationContext as ResonanceApplication
            val themePreference by application.appContainer
                .preferencesRepository.themePreference.collectAsState()

            val darkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            var appliedDarkTheme by remember { mutableStateOf(darkTheme) }
            var overlayBitmap by remember { mutableStateOf<Bitmap?>(null) }

            val view = LocalView.current

            LaunchedEffect(darkTheme) {
                if (darkTheme != appliedDarkTheme) {
                    overlayBitmap = try {
                        view.drawToBitmap()
                    } catch (_: IllegalStateException) {
                        null
                    }
                    appliedDarkTheme = darkTheme
                }
            }

            if (!view.isInEditMode) {
                SideEffect {
                    val insetsController = WindowCompat.getInsetsController(
                        window,
                        view
                    )
                    insetsController.isAppearanceLightStatusBars = !appliedDarkTheme
                    insetsController.isAppearanceLightNavigationBars = !appliedDarkTheme
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                ResonanceTheme(darkTheme = appliedDarkTheme) {
                    AppNavigation()
                }

                overlayBitmap?.let { bitmap ->
                    CircularRevealOverlay(
                        bitmap = bitmap,
                        onAnimationEnd = {
                            overlayBitmap = null
                        }
                    )
                }
            }
        }
    }
}

/**
 * Overlay that displays a captured [bitmap] and animates a growing circular
 * hole from the screen center to reveal the new theme underneath.
 *
 * Uses [CompositingStrategy.Offscreen] with [android.graphics.PorterDuff.Mode.CLEAR]
 * to punch a transparent circle into the bitmap. The radius animates from 0
 * to the diagonal of the screen so that all corners are revealed.
 *
 * @param bitmap The captured screen bitmap to overlay.
 * @param onAnimationEnd Callback invoked when the reveal animation completes.
 */
@Composable
private fun CircularRevealOverlay(
    bitmap: Bitmap,
    onAnimationEnd: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(bitmap) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = REVEAL_ANIMATION_DURATION_MS)
        )
        onAnimationEnd()
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = hypot(centerX, centerY)
        val currentRadius = maxRadius * progress.value

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmap(
                bitmap,
                0f,
                0f,
                null
            )
        }

        drawCircle(
            color = Color.Black,
            radius = currentRadius,
            center = Offset(centerX, centerY),
            blendMode = BlendMode.Clear
        )
    }
}
