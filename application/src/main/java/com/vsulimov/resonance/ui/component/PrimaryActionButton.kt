package com.vsulimov.resonance.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val ButtonHeight = 56.dp
private val ButtonCornerRadius = 28.dp
private val SpinnerSize = 24.dp
private val SpinnerStrokeWidth = 2.5.dp
private val SpinnerTextGap = 8.dp

/** Mutually exclusive states for [PrimaryActionButton]. */
enum class ActionButtonState {
    DEFAULT,
    LOADING,
    SUCCESS
}

/**
 * Full-width filled button with stadium shape used for primary actions.
 *
 * Supports a loading state that shows a spinner alongside the text
 * and disables interaction.
 *
 * @param text Label displayed inside the button.
 * @param onClick Callback invoked on click (ignored while loading).
 * @param modifier Modifier applied to the button.
 * @param state Current visual state of the button.
 * @param enabled Whether the button is interactive. Forced to `false` while loading.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ActionButtonState = ActionButtonState.DEFAULT,
    enabled: Boolean = true
) {
    val isLoading = state == ActionButtonState.LOADING

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(ButtonCornerRadius)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpinnerSize),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = SpinnerStrokeWidth
            )
            Spacer(modifier = Modifier.width(SpinnerTextGap))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
