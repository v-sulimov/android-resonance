package com.vsulimov.resonance.ui.screen.onboarding.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.R

private val PillColumnMaxWidth = 300.dp
private val PillColumnSpacing = 12.dp
private val PillCornerRadius = 16.dp
private val PillVerticalPadding = 12.dp
private val PillHorizontalPadding = 16.dp
private val PillIconContainerSize = 40.dp
private val PillIconContainerCornerRadius = 12.dp
private val PillIconSize = 24.dp
private val PillIconPadding = 8.dp
private val PillIconTextGap = 12.dp

/**
 * Vertical list of feature pills describing Resonance's core values.
 *
 * Displays three pills (privacy, open source, self-hosted) centered
 * in a column with a maximum width constraint.
 *
 * @param modifier Modifier applied to the outer column.
 */
@Composable
fun FeaturePillColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.widthIn(max = PillColumnMaxWidth),
        verticalArrangement = Arrangement.spacedBy(PillColumnSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FeaturePill(
            icon = Icons.Outlined.Shield,
            text = stringResource(R.string.welcome_pill_privacy)
        )
        FeaturePill(
            icon = Icons.Outlined.Code,
            text = stringResource(R.string.welcome_pill_open_source)
        )
        FeaturePill(
            icon = Icons.Outlined.Dns,
            text = stringResource(R.string.welcome_pill_self_hosted)
        )
    }
}

/**
 * Single feature pill with a tinted icon container and descriptive text.
 *
 * Rendered as a rounded surface with [surfaceContainerLow] background,
 * containing an icon in a [primaryContainer] badge followed by text.
 *
 * @param icon Icon displayed in the leading badge.
 * @param text Descriptive label for the feature.
 * @param modifier Modifier applied to the pill surface.
 */
@Composable
private fun FeaturePill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(PillCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(
                vertical = PillVerticalPadding,
                horizontal = PillHorizontalPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(PillIconContainerSize),
                shape = RoundedCornerShape(PillIconContainerCornerRadius),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(PillIconPadding)
                        .size(PillIconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(PillIconTextGap))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
