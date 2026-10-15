package com.vsulimov.resonance.ui.screens.serverconfig

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.ui.theme.Dimens
import com.vsulimov.resonance.ui.theme.ResonanceTheme

private val PROGRESS_INDICATOR_STROKE_WIDTH = 2.dp

/**
 * Server Configuration screen for entering server host address and testing Subsonic API connection.
 *
 * @param onContinue Callback triggered when connection test passes and user proceeds to authentication.
 * @param onBackClick Callback triggered when back navigation is requested.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel ViewModel managing server host input validation and connection testing state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    onContinue: (serverUrl: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServerConfigViewModel = viewModel(factory = ServerConfigViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var protocolDropdownExpanded by remember { mutableStateOf(value = false) }

    LaunchedEffect(uiState.status) {
        if (uiState.status is ConnectionStatus.Success) {
            focusManager.clearFocus()
        }
    }

    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(
                    start = Dimens.paddingLarge + leftInset,
                    end = Dimens.paddingLarge + rightInset
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(Dimens.paddingLarge))

                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconExtraLarge),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(Dimens.paddingMedium))

                Text(
                    text = stringResource(R.string.server_config_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Dimens.paddingSmall))

                Text(
                    text = stringResource(R.string.server_config_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Dimens.paddingExtraLarge))

                OutlinedTextField(
                    value = uiState.serverHost,
                    onValueChange = viewModel::onServerHostChanged,
                    label = {
                        Text(
                            text = stringResource(R.string.server_url_label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.server_url_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1,
                    leadingIcon = {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable { protocolDropdownExpanded = true }
                                    .padding(horizontal = Dimens.paddingMedium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.urlProtocol,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.cd_protocol_dropdown),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = protocolDropdownExpanded,
                                onDismissRequest = { protocolDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.server_config_protocol_https)) },
                                    onClick = {
                                        viewModel.onProtocolChanged(Constants.PROTOCOL_HTTPS)
                                        protocolDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.server_config_protocol_http)) },
                                    onClick = {
                                        viewModel.onProtocolChanged(Constants.PROTOCOL_HTTP)
                                        protocolDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.testConnection()
                        }
                    ),
                    trailingIcon = {
                        if (uiState.serverHost.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearServerHost() },
                                modifier = Modifier.size(Dimens.iconLarge)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.cd_clear_text),
                                    modifier = Modifier.size(Dimens.iconSmall)
                                )
                            }
                        }
                    },
                    isError = uiState.status is ConnectionStatus.Error
                )

                Spacer(modifier = Modifier.height(Dimens.paddingLarge))

                AnimatedVisibility(
                    visible = uiState.status !is ConnectionStatus.Idle,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    StatusCard(status = uiState.status)
                }

                Spacer(modifier = Modifier.height(Dimens.paddingLarge))
            }

            Spacer(modifier = Modifier.height(Dimens.paddingMedium))

            Button(
                onClick = {
                    if (uiState.status is ConnectionStatus.Success) {
                        onContinue(uiState.urlProtocol + uiState.serverHost)
                    } else {
                        viewModel.testConnection()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.paddingLarge)
                    .height(Dimens.iconExtraLarge),
                enabled = (uiState.status !is ConnectionStatus.Testing) && uiState.serverHost.isNotBlank()
            ) {
                if (uiState.status is ConnectionStatus.Testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.iconMedium),
                        strokeWidth = PROGRESS_INDICATOR_STROKE_WIDTH,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                }

                val buttonText = if (uiState.status is ConnectionStatus.Success) {
                    stringResource(R.string.btn_continue)
                } else {
                    stringResource(R.string.btn_test_connection)
                }

                Text(text = buttonText)
            }
        }
    }
}

/**
 * Status feedback banner displaying connection state, error messages, or server version on success.
 *
 * @param status Current connection status.
 */
@Composable
private fun StatusCard(status: ConnectionStatus) {
    val backgroundColor = when (status) {
        is ConnectionStatus.Success -> MaterialTheme.colorScheme.primaryContainer
        is ConnectionStatus.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (status) {
        is ConnectionStatus.Success -> MaterialTheme.colorScheme.onPrimaryContainer
        is ConnectionStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .padding(Dimens.paddingMedium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                is ConnectionStatus.Success -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = contentColor
                    )
                }

                is ConnectionStatus.Error -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = contentColor
                    )
                }

                else -> {}
            }

            if (status !is ConnectionStatus.Testing) {
                Spacer(modifier = Modifier.width(Dimens.paddingSmall))
            }

            val text = when (status) {
                is ConnectionStatus.Testing -> stringResource(R.string.status_testing)
                is ConnectionStatus.Success -> stringResource(R.string.status_success, status.serverVersion)
                is ConnectionStatus.Error -> stringResource(status.messageResId)
                ConnectionStatus.Idle -> ""
            }

            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ServerConfigScreenPreview() {
    ResonanceTheme {
        ServerConfigScreen(
            onContinue = {},
            onBackClick = {}
        )
    }
}
