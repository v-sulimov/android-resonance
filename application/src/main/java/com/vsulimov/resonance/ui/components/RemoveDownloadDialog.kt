package com.vsulimov.resonance.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vsulimov.resonance.R

/**
 * Reusable Material 3 confirmation AlertDialog for deleting downloaded offline content.
 *
 * @param itemName Name of the track, album, or playlist to remove.
 * @param onConfirm Callback triggered when user confirms deletion.
 * @param onDismiss Callback triggered when user cancels or dismisses dialog.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun RemoveDownloadDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_remove_download_title)) },
        text = { Text(text = stringResource(R.string.dialog_remove_download_message, itemName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_remove_download_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_remove_download_cancel))
            }
        },
        modifier = modifier
    )
}
