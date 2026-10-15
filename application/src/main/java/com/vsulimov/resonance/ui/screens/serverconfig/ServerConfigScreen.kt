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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.theme.Dimens
import com.vsulimov.resonance.ui.theme.ResonanceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    onContinue: (serverUrl: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServerConfigViewModel = viewModel(factory = ServerConfigViewModel.Factory)
) {
    val urlProtocol by viewModel.urlProtocol.collectAsState()
    val serverHost by viewModel.serverHost.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()
    val focusManager = LocalFocusManager.current

    var protocolDropdownExpanded by remember { mutableStateOf(false) }

    // Clear focus and hide keyboard on successful connection
    LaunchedEffect(status) {
        if (status is ConnectionStatus.Success) {
            focusManager.clearFocus()
        }
    }

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
                .padding(horizontal = Dimens.paddingLarge)
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
                    value = serverHost,
                    onValueChange = viewModel::onServerHostChanged,
                    label = { Text(stringResource(R.string.server_url_label)) },
                    placeholder = { Text(stringResource(R.string.server_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable { protocolDropdownExpanded = true }
                                    .padding(horizontal = Dimens.paddingMedium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = urlProtocol,
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
                                        viewModel.onProtocolChanged(
                                            com.vsulimov.resonance.core.Constants.PROTOCOL_HTTPS
                                        )
                                        protocolDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.server_config_protocol_http)) },
                                    onClick = {
                                        viewModel.onProtocolChanged(com.vsulimov.resonance.core.Constants.PROTOCOL_HTTP)
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
                            // Only run the connection test on Done.
                            // The LaunchedEffect will hide the keyboard automatically if successful.
                            viewModel.testConnection()
                        }
                    ),
                    trailingIcon = {
                        if (serverHost.isNotEmpty()) {
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
                    isError = status is ConnectionStatus.Error
                )

                Spacer(modifier = Modifier.height(Dimens.paddingLarge))

                AnimatedVisibility(
                    visible = status !is ConnectionStatus.Idle,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    StatusCard(status = status)
                }

                Spacer(modifier = Modifier.height(Dimens.paddingLarge))
            }

            Spacer(modifier = Modifier.height(Dimens.paddingMedium))

            Button(
                onClick = {
                    if (status is ConnectionStatus.Success) {
                        onContinue(viewModel.urlProtocol.value + viewModel.serverHost.value)
                    } else {
                        // Only run the test. If successful, LaunchedEffect clears focus/keyboard.
                        viewModel.testConnection()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.paddingLarge)
                    .height(Dimens.iconExtraLarge),
                enabled = status !is ConnectionStatus.Testing && serverHost.isNotBlank()
            ) {
                if (status is ConnectionStatus.Testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.iconMedium),
                        strokeWidth = Dimens.paddingSmall / 4,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                }

                val buttonText = if (status is ConnectionStatus.Success) {
                    stringResource(R.string.btn_continue)
                } else {
                    stringResource(R.string.btn_test_connection)
                }

                Text(text = buttonText)
            }
        }
    }
}

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
