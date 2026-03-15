package com.vsulimov.resonance.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.R

private val IconSize = 48.dp
private val IconToTitleSpacing = 16.dp
private val TitleToSubtitleSpacing = 4.dp
private val SubtitleToButtonSpacing = 24.dp
private val TitleOnlyToButtonSpacing = 24.dp
private val HorizontalPadding = 48.dp

/**
 * Shared error state composable used across Mix, Library, and Album Detail screens.
 *
 * Displays a centered layout with a cloud-off icon, a screen-specific title,
 * an optional subtitle for server error details, and a filled tonal retry button.
 *
 * Callers provide [fillMaxSize][androidx.compose.foundation.layout.fillMaxSize]
 * and scaffold padding via [modifier]. This component handles internal layout
 * and horizontal padding for readable text wrapping.
 *
 * @param title Screen-specific error title (e.g. "Could not load content").
 * @param onRetry Callback invoked when the retry button is tapped.
 * @param modifier Modifier applied to the root layout.
 * @param subtitle Optional server error detail message.
 */
@Composable
fun ErrorContent(
    title: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier.padding(horizontal = HorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = stringResource(R.string.cd_error_icon),
            modifier = Modifier.size(IconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(IconToTitleSpacing))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(TitleToSubtitleSpacing))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(SubtitleToButtonSpacing))
        } else {
            Spacer(modifier = Modifier.height(TitleOnlyToButtonSpacing))
        }
        FilledTonalButton(onClick = onRetry) {
            Text(text = stringResource(R.string.error_retry))
        }
    }
}
