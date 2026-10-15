package com.vsulimov.resonance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.theme.Dimens

private val EMPTY_ICON_SIZE = Dimens.artworkSizeSmall * 2

/**
 * Reusable full-screen loading state displaying a centered progress indicator.
 *
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Reusable error state view displaying an error message and a retry button.
 *
 * @param message Human-readable error description string.
 * @param onRetryClick Callback triggered when the retry button is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun ErrorState(message: String, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimens.paddingMedium))

        Button(onClick = onRetryClick) {
            Text(text = stringResource(R.string.btn_retry))
        }
    }
}

/**
 * Reusable empty state view displaying an icon, title, and optional descriptive message.
 *
 * @param icon The vector icon to display in the empty state hero position.
 * @param title Short summary title string.
 * @param modifier The modifier to be applied to the layout.
 * @param description Optional extended descriptive explanation string.
 */
@Composable
fun EmptyState(icon: ImageVector, title: String, modifier: Modifier = Modifier, description: String? = null) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(EMPTY_ICON_SIZE)
        )

        Spacer(modifier = Modifier.height(Dimens.paddingLarge))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (description != null) {
            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
