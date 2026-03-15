package com.vsulimov.resonance.ui.screen.onboarding.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.component.PrimaryActionButton
import com.vsulimov.resonance.ui.component.ResonanceLogo

private val ScreenHorizontalPadding = 32.dp
private val ButtonBottomPadding = 16.dp
private val ContentBottomPadding = 24.dp
private val LogoToTitleSpacing = 28.dp
private val TitleToTaglineSpacing = 8.dp
private val TaglineToPillsSpacing = 48.dp

/**
 * Welcome screen shown to unauthenticated users.
 *
 * Communicates app identity and core values (privacy, open source, self-hosted)
 * with a single CTA leading to the server login flow.
 *
 * Content is vertically centered when it fits on screen and becomes scrollable
 * in landscape or on smaller devices.
 *
 * @param onGetStartedClick Callback invoked when the user taps "Get started".
 * @param modifier Modifier applied to the root container.
 */
@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .defaultMinSize(minHeight = maxHeight)
                        .padding(
                            horizontal = ScreenHorizontalPadding,
                            vertical = ContentBottomPadding
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ResonanceLogo()

                    Spacer(modifier = Modifier.height(LogoToTitleSpacing))

                    Text(
                        text = stringResource(R.string.application_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(TitleToTaglineSpacing))

                    Text(
                        text = stringResource(R.string.welcome_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(TaglineToPillsSpacing))

                    FeaturePillColumn()
                }
            }

            PrimaryActionButton(
                text = stringResource(R.string.welcome_get_started),
                onClick = onGetStartedClick,
                modifier = Modifier.padding(
                    start = ScreenHorizontalPadding,
                    end = ScreenHorizontalPadding,
                    bottom = ButtonBottomPadding
                )
            )
        }
    }
}
