package com.vsulimov.resonance.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.theme.Dimens

/**
 * Interface or wrapper class representing a selectable sorting criterion with a human-readable title resource.
 *
 * @param id Unique identifier or enum value for this sorting option.
 * @param labelResId String resource ID for the sorting option title.
 */
data class SortOption<T>(val id: T, val labelResId: Int)

/**
 * Reusable Material 3 Modal Bottom Sheet for selecting sorting criteria and direction.
 *
 * @param options List of supported sorting criteria options.
 * @param selectedOption Active sorting option.
 * @param isAscending Active sorting direction flag (true for ascending, false for descending).
 * @param onOptionSelect Callback triggered when a sorting option is chosen.
 * @param onAscendingToggle Callback triggered when sorting direction is toggled.
 * @param onDismissRequest Callback triggered when closing the bottom sheet.
 * @param modifier The modifier to be applied to the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SortBottomSheet(
    options: List<SortOption<T>>,
    selectedOption: SortOption<T>,
    isAscending: Boolean,
    onOptionSelect: (SortOption<T>) -> Unit,
    onAscendingToggle: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeDrawingInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeDrawingInsets.calculateRightPadding(LayoutDirection.Ltr)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = leftInset,
                    end = rightInset,
                    bottom = Dimens.paddingSmall
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.iconExtraLarge)
                    .padding(horizontal = Dimens.paddingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sort_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onAscendingToggle) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(start = Dimens.paddingSmall / 2))
                    Text(
                        text = stringResource(if (isAscending) R.string.sort_ascending else R.string.sort_descending),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))
            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.paddingMedium))
            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOptionSelect(option)
                            onDismissRequest()
                        }
                        .padding(
                            start = Dimens.paddingSmall / 2,
                            end = Dimens.paddingMedium,
                            top = Dimens.paddingSmall,
                            bottom = Dimens.paddingSmall
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = option.id == selectedOption.id,
                        onClick = {
                            onOptionSelect(option)
                            onDismissRequest()
                        }
                    )
                    Text(
                        text = stringResource(option.labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
