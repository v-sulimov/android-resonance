package com.vsulimov.resonance.ui.screen.onboarding.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.component.ActionButtonState
import com.vsulimov.resonance.ui.component.BannerType
import com.vsulimov.resonance.ui.component.PrimaryActionButton
import com.vsulimov.resonance.ui.component.ResultBanner

private val ScreenHorizontalPadding = 24.dp
private val ScreenBottomPadding = 16.dp
private val TopSpacing = 24.dp
private val TitleToSubtitleSpacing = 8.dp
private val SubtitleToFormSpacing = 32.dp
private val FormToBannerSpacing = 16.dp
private val ContentBottomPadding = 24.dp

/**
 * Server login screen for connecting to a Navidrome server.
 *
 * Presents a form with server URL, username, and password fields.
 * Validates the URL format locally, then authenticates with the server
 * via a ping request. Displays error or success banners based on the
 * connection result. The system back gesture handles navigation to
 * the previous screen.
 *
 * State is scoped to a [ServerLoginViewModel] so that entered values
 * and connection state survive configuration changes such as rotation.
 * The ViewModel is created using [ServerLoginViewModel.Factory] which
 * resolves the [ConnectToServerUseCase][com.vsulimov.resonance.domain.usecase.ConnectToServerUseCase]
 * from the application's DI container.
 *
 * @param onLoginSuccess Callback invoked after a successful connection when
 *   the user taps "Continue".
 * @param modifier Modifier applied to the root scaffold.
 */
@Composable
fun ServerLoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ServerLoginViewModel = viewModel(factory = ServerLoginViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val connectionState by stateHolder.connectionState.collectAsState()
    val isLoading = connectionState is ConnectionState.Loading

    val unexpectedError = stringResource(R.string.login_error_unexpected)
    val bannerType = when (val state = connectionState) {
        is ConnectionState.Error -> BannerType.Error(state.message.ifEmpty { unexpectedError })
        is ConnectionState.Success -> BannerType.Success(
            stringResource(R.string.login_success_message, state.viewObject.serverVersion)
        )
        else -> null
    }

    val buttonState = when (connectionState) {
        is ConnectionState.Loading -> ActionButtonState.LOADING
        is ConnectionState.Success -> ActionButtonState.SUCCESS
        else -> ActionButtonState.DEFAULT
    }

    val buttonText = when (buttonState) {
        ActionButtonState.LOADING -> stringResource(R.string.login_button_connecting)
        ActionButtonState.SUCCESS -> stringResource(R.string.login_button_continue)
        ActionButtonState.DEFAULT -> stringResource(R.string.login_button_connect)
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding)
                    .padding(bottom = ContentBottomPadding)
            ) {
                Spacer(modifier = Modifier.height(TopSpacing))

                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(TitleToSubtitleSpacing))

                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(SubtitleToFormSpacing))

                LoginFormFields(
                    stateHolder = stateHolder,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(FormToBannerSpacing))

                ResultBanner(type = bannerType)
            }

            PrimaryActionButton(
                text = buttonText,
                onClick = {
                    if (buttonState == ActionButtonState.SUCCESS) {
                        onLoginSuccess()
                    } else {
                        viewModel.connect()
                    }
                },
                modifier = Modifier.padding(
                    start = ScreenHorizontalPadding,
                    end = ScreenHorizontalPadding,
                    bottom = ScreenBottomPadding
                ),
                state = buttonState
            )
        }
    }
}
