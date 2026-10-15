package com.vsulimov.resonance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.theme.Dimens

/**
 * Reusable category list header displaying total item count badge and optional "Shuffle All" action button.
 *
 * @param itemCount Total number of items in the list.
 * @param modifier The modifier to be applied to the layout.
 * @param onShuffleClick Optional callback triggered when tapping the Shuffle button.
 */
@Composable
fun CategoryHeaderActions(itemCount: Int, modifier: Modifier = Modifier, onShuffleClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.paddingMedium,
                vertical = Dimens.paddingSmall
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.item_count_format, itemCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (onShuffleClick != null && itemCount > 0) {
            FilledTonalButton(
                onClick = onShuffleClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Dimens.paddingMedium,
                    vertical = Dimens.paddingSmall
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSmall)
                )
                Text(
                    text = stringResource(R.string.btn_shuffle),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = Dimens.paddingSmall)
                )
            }
        }
    }
}
