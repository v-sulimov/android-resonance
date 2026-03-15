package com.vsulimov.resonance.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.ui.theme.ResonanceTheme

private val BannerShadowElevation = 6.dp
private val BannerCornerRadius = 16.dp
private val BannerMinHeight = 72.dp
private val BannerVerticalPadding = 16.dp
private val BannerHorizontalPadding = 20.dp
private val BannerIconSize = 24.dp
private val BannerIconTextGap = 12.dp

/**
 * Type of feedback displayed by [ResultBanner].
 *
 * Each subtype carries a user-facing [message] and determines the banner's
 * visual style (icon, colors).
 */
sealed class BannerType {

    /** User-facing message displayed in the banner body. */
    abstract val message: String

    /**
     * Error feedback — uses the M3 error container colors.
     *
     * @property message Description of the error shown to the user.
     */
    data class Error(override val message: String) : BannerType()

    /**
     * Success feedback — uses the extended success container colors.
     *
     * @property message Confirmation message shown to the user.
     */
    data class Success(override val message: String) : BannerType()
}

/**
 * Animated feedback banner displaying an icon alongside a message.
 *
 * Slides in from above and fades in when [type] is non-null; fades out when
 * set to `null`. The last non-null [type] is retained during the exit animation
 * so the content remains visible while fading out.
 *
 * @param type The banner variant to display, or `null` to hide.
 * @param modifier Modifier applied to the animated container.
 */
@Composable
fun ResultBanner(
    type: BannerType?,
    modifier: Modifier = Modifier
) {
    // Retain the last non-null type so the banner content stays visible
    // throughout the exit animation instead of disappearing instantly.
    var lastNonNullType by remember { mutableStateOf(type) }
    if (type != null) {
        lastNonNullType = type
    }

    AnimatedVisibility(
        visible = type != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = fadeOut()
    ) {
        lastNonNullType?.let { banner ->
            val colors = rememberBannerColors(banner)

            Surface(
                shape = RoundedCornerShape(BannerCornerRadius),
                color = colors.background,
                shadowElevation = BannerShadowElevation,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = BannerMinHeight)
            ) {
                Row(
                    modifier = Modifier.padding(
                        vertical = BannerVerticalPadding,
                        horizontal = BannerHorizontalPadding
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = colors.icon,
                        contentDescription = null,
                        modifier = Modifier.size(BannerIconSize),
                        tint = colors.iconTint
                    )
                    Spacer(modifier = Modifier.width(BannerIconTextGap))
                    Text(
                        text = banner.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text
                    )
                }
            }
        }
    }
}

/**
 * Resolved color and icon values for a [ResultBanner].
 *
 * @property background Surface color of the banner.
 * @property text Text color inside the banner.
 * @property icon Leading icon image vector.
 * @property iconTint Tint color applied to the leading icon.
 */
private data class BannerColors(
    val background: Color,
    val text: Color,
    val icon: ImageVector,
    val iconTint: Color
)

/**
 * Resolves and remembers [BannerColors] for the given [type].
 *
 * The result is recomputed only when [type], the M3 color scheme, or the
 * extended Resonance colors change.
 *
 * @param type The banner type to resolve colors for.
 * @return Resolved [BannerColors] for the given type.
 */
@Composable
private fun rememberBannerColors(type: BannerType): BannerColors {
    val colorScheme = MaterialTheme.colorScheme
    val extended = ResonanceTheme.extendedColors
    return remember(type, colorScheme, extended) {
        when (type) {
            is BannerType.Error -> BannerColors(
                background = colorScheme.errorContainer,
                text = colorScheme.onErrorContainer,
                icon = Icons.Outlined.Error,
                iconTint = colorScheme.error
            )
            is BannerType.Success -> BannerColors(
                background = extended.successContainer,
                text = extended.onSuccessContainer,
                icon = Icons.Outlined.CheckCircle,
                iconTint = extended.successIcon
            )
        }
    }
}
