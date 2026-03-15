package com.vsulimov.resonance.ui.screen.onboarding.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.R

private val FieldSpacing = 16.dp

/**
 * Groups the three login form fields and wires them to the [stateHolder].
 *
 * @param stateHolder State holder managing field values and validation.
 * @param enabled Whether the fields are interactive (false during loading).
 * @param modifier Modifier applied to the outer column.
 */
@Composable
fun LoginFormFields(
    stateHolder: ServerLoginStateHolder,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val serverUrl by stateHolder.serverUrl.collectAsState()
    val username by stateHolder.username.collectAsState()
    val password by stateHolder.password.collectAsState()
    val urlValidationFailed by stateHolder.urlValidationFailed.collectAsState()
    val passwordVisible by stateHolder.passwordVisible.collectAsState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FieldSpacing)
    ) {
        ServerUrlField(
            value = serverUrl,
            onValueChange = stateHolder::onServerUrlChanged,
            onFocusLost = { stateHolder.validateUrl() },
            isError = urlValidationFailed,
            enabled = enabled
        )
        CredentialField(
            value = username,
            onValueChange = stateHolder::onUsernameChanged,
            label = stringResource(R.string.login_field_username),
            placeholder = stringResource(R.string.login_field_username),
            enabled = enabled
        )
        CredentialField(
            value = password,
            onValueChange = stateHolder::onPasswordChanged,
            label = stringResource(R.string.login_field_password),
            placeholder = stringResource(R.string.login_field_password),
            enabled = enabled,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = stateHolder::togglePasswordVisibility
        )
    }
}

/**
 * Server URL input with inline validation error display.
 *
 * Validates the URL format when the field loses focus.
 *
 * @param value Current text value.
 * @param onValueChange Callback for text changes.
 * @param onFocusLost Callback invoked when the field loses focus, used to trigger validation.
 * @param isError Whether to show the validation error state.
 * @param enabled Whether the field is interactive.
 * @param modifier Modifier applied to the text field.
 */
@Composable
fun ServerUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (!state.isFocused && value.isNotEmpty()) {
                    onFocusLost()
                }
            },
        enabled = enabled,
        label = { Text(stringResource(R.string.login_field_server_url)) },
        placeholder = { Text(stringResource(R.string.login_field_server_url_placeholder)) },
        supportingText = if (isError) {
            { Text(stringResource(R.string.login_url_validation_error)) }
        } else {
            null
        },
        isError = isError,
        trailingIcon = if (isError) {
            {
                Icon(
                    imageVector = Icons.Outlined.Error,
                    contentDescription = stringResource(R.string.cd_validation_error),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
}

/**
 * Text field for username or password entry.
 *
 * When [isPassword] is true, the field uses a password visual transformation
 * and shows a visibility toggle icon.
 *
 * @param value Current text value.
 * @param onValueChange Callback for text changes.
 * @param label Label displayed above the field.
 * @param placeholder Placeholder text shown when empty.
 * @param enabled Whether the field is interactive.
 * @param modifier Modifier applied to the text field.
 * @param keyboardType Keyboard type to use.
 * @param isPassword Whether this field is a password input.
 * @param passwordVisible Whether the password text is currently visible.
 * @param onTogglePasswordVisibility Callback to toggle password visibility.
 */
@Composable
fun CredentialField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null
) {
    val showPasswordText = stringResource(R.string.cd_show_password)
    val hidePasswordText = stringResource(R.string.cd_hide_password)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
            {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (passwordVisible) hidePasswordText else showPasswordText
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}
