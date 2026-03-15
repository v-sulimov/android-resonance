package com.vsulimov.resonance.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.domain.model.AudioQuality
import com.vsulimov.resonance.domain.model.ThemePreference

// region Dimension constants (4dp grid)

private val ServerInfoCardPadding = 20.dp
private val ServerInfoCardMarginHorizontal = 16.dp
private val ServerInfoCardMarginTop = 16.dp
private val ServerInfoCardMarginBottom = 16.dp
private val ServerInfoCardCornerRadius = 16.dp
private val ServerInfoAvatarSize = 48.dp
private val ServerInfoGap = 16.dp
private val ServerInfoLineSpacing = 4.dp
private val SectionHeaderHorizontalPadding = 16.dp
private val SectionHeaderTopPadding = 16.dp
private val SectionHeaderBottomPadding = 4.dp
private val DisconnectTopPadding = 8.dp
private val DisconnectIconSize = 20.dp
private val DisconnectIconTextGap = 12.dp
private val SheetContentBottomPadding = 32.dp
private val SheetTitlePadding = 24.dp
private val SheetTitleTopPadding = 24.dp
private val SheetTitleBottomPadding = 8.dp
private val SheetOptionHorizontalPadding = 24.dp
private val SheetOptionVerticalPadding = 12.dp
private val SheetRadioTextGap = 16.dp
private val SheetDescriptionTopSpacing = 4.dp

// endregion

/**
 * Settings screen displaying server info, preferences, and account actions.
 *
 * Follows Material 3 settings screen patterns:
 * - Top app bar with back navigation and "Settings" title
 * - Server info card at the top
 * - Categorized settings sections (Playback, Storage, Appearance, About)
 * - Bottom sheets for theme and audio quality selection
 * - Disconnect action with confirmation dialog
 *
 * State is scoped to [SettingsViewModel] so that data survives
 * configuration changes.
 *
 * @param onBack Callback invoked when the back navigation button is tapped.
 * @param onNavigateToCache Callback invoked to navigate to the cache screen.
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val screenState by stateHolder.screenState.collectAsState()

    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    var showAudioQualitySheet by rememberSaveable { mutableStateOf(false) }
    var showDisconnectDialog by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                stateHolder.refreshCacheSize()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = screenState) {
            is SettingsScreenState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is SettingsScreenState.Content -> {
                SettingsContent(
                    state = state,
                    onThemeClick = { showThemeSheet = true },
                    onAudioQualityClick = { showAudioQualitySheet = true },
                    onCacheClick = onNavigateToCache,
                    onDisconnectClick = { showDisconnectDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (showThemeSheet) {
        val state = screenState
        if (state is SettingsScreenState.Content) {
            ThemeBottomSheet(
                selectedTheme = state.themePreference,
                onThemeSelected = { preference ->
                    stateHolder.setThemePreference(preference)
                    showThemeSheet = false
                },
                onDismiss = { showThemeSheet = false }
            )
        }
    }

    if (showAudioQualitySheet) {
        val state = screenState
        if (state is SettingsScreenState.Content) {
            AudioQualityBottomSheet(
                selectedQuality = state.audioQuality,
                onQualitySelected = { quality ->
                    stateHolder.setAudioQuality(quality)
                    showAudioQualitySheet = false
                },
                onDismiss = { showAudioQualitySheet = false }
            )
        }
    }

    if (showDisconnectDialog) {
        val state = screenState
        if (state is SettingsScreenState.Content) {
            DisconnectDialog(
                serverUrl = state.serverUrl,
                onConfirm = {
                    showDisconnectDialog = false
                    stateHolder.disconnect()
                },
                onDismiss = { showDisconnectDialog = false }
            )
        }
    }
}

/**
 * Main settings content with scrollable sections.
 */
@Composable
private fun SettingsContent(
    state: SettingsScreenState.Content,
    onThemeClick: () -> Unit,
    onAudioQualityClick: () -> Unit,
    onCacheClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        ServerInfoCard(
            username = state.username,
            serverUrl = state.serverUrl,
            serverVersion = state.serverVersion,
            serverType = state.serverType
        )

        SectionHeader(text = stringResource(R.string.settings_section_playback))
        SettingsItem(
            headline = stringResource(R.string.settings_audio_quality),
            supporting = audioQualitySummaryResource(state.audioQuality),
            onClick = onAudioQualityClick
        )

        SectionHeader(text = stringResource(R.string.settings_section_storage))
        SettingsItem(
            headline = stringResource(R.string.settings_cache),
            supporting = stringResource(R.string.settings_cache_summary, state.cacheSizeFormatted),
            onClick = onCacheClick
        )

        SectionHeader(text = stringResource(R.string.settings_section_appearance))
        SettingsItem(
            headline = stringResource(R.string.settings_theme),
            supporting = themeSummaryResource(state.themePreference),
            onClick = onThemeClick
        )

        SectionHeader(text = stringResource(R.string.settings_section_about))
        ListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_version)) },
            supportingContent = {
                Text(text = stringResource(R.string.settings_version_value, state.appVersion))
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        SettingsItem(
            headline = stringResource(R.string.settings_licenses),
            onClick = { /* TODO: Open licenses screen */ }
        )
        ListItem(
            modifier = Modifier.clickable { /* TODO: Open source code URL */ },
            headlineContent = { Text(text = stringResource(R.string.settings_source_code)) },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = stringResource(R.string.cd_open_external),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        DisconnectButton(
            onClick = onDisconnectClick,
            modifier = Modifier.padding(
                horizontal = SectionHeaderHorizontalPadding,
                vertical = DisconnectTopPadding
            )
        )
    }
}

/**
 * Card displaying the connected server and user information.
 *
 * Uses [surfaceContainerLow] as the background per M3 card patterns,
 * with a circular avatar showing the user's initial.
 */
@Composable
private fun ServerInfoCard(
    username: String,
    serverUrl: String,
    serverVersion: String?,
    serverType: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(
            start = ServerInfoCardMarginHorizontal,
            end = ServerInfoCardMarginHorizontal,
            top = ServerInfoCardMarginTop
        ),
        shape = RoundedCornerShape(ServerInfoCardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ServerInfoCardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(ServerInfoAvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(ServerInfoGap))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(ServerInfoLineSpacing))
                Text(
                    text = serverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (serverVersion != null && serverType != null) {
                    Spacer(modifier = Modifier.height(ServerInfoLineSpacing))
                    Text(
                        text = stringResource(
                            R.string.settings_server_version,
                            serverType.replaceFirstChar { it.titlecase() },
                            serverVersion
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(ServerInfoCardMarginBottom))
}

/**
 * Section header label using [primary] color per M3 preference patterns.
 */
@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = SectionHeaderHorizontalPadding,
            end = SectionHeaderHorizontalPadding,
            top = SectionHeaderTopPadding,
            bottom = SectionHeaderBottomPadding
        )
    )
}

/**
 * Standard settings list item with headline, optional supporting text,
 * and a chevron trailing icon indicating navigation.
 */
@Composable
private fun SettingsItem(
    headline: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(text = headline) },
        supportingContent = supporting?.let { { Text(text = it) } },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * Disconnect action styled as an error-colored text button with icon.
 */
@Composable
private fun DisconnectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SectionHeaderTopPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            modifier = Modifier.size(DisconnectIconSize),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(DisconnectIconTextGap))
        Text(
            text = stringResource(R.string.settings_disconnect),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// region Bottom Sheets

/**
 * Modal bottom sheet for selecting the application theme.
 *
 * Uses standard M3 [RadioButton] for accessibility compliance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeBottomSheet(
    selectedTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = SheetContentBottomPadding)
        ) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    start = SheetTitlePadding,
                    end = SheetTitlePadding,
                    top = SheetTitleTopPadding,
                    bottom = SheetTitleBottomPadding
                )
            )

            SheetRadioOption(
                title = stringResource(R.string.settings_theme_system),
                description = stringResource(R.string.settings_theme_system_description),
                selected = selectedTheme == ThemePreference.SYSTEM,
                onClick = { onThemeSelected(ThemePreference.SYSTEM) }
            )
            SheetRadioOption(
                title = stringResource(R.string.settings_theme_light),
                selected = selectedTheme == ThemePreference.LIGHT,
                onClick = { onThemeSelected(ThemePreference.LIGHT) }
            )
            SheetRadioOption(
                title = stringResource(R.string.settings_theme_dark),
                selected = selectedTheme == ThemePreference.DARK,
                onClick = { onThemeSelected(ThemePreference.DARK) }
            )
        }
    }
}

/**
 * Modal bottom sheet for selecting audio streaming quality.
 *
 * Uses standard M3 [RadioButton] for accessibility compliance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioQualityBottomSheet(
    selectedQuality: AudioQuality,
    onQualitySelected: (AudioQuality) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = SheetContentBottomPadding)
        ) {
            Text(
                text = stringResource(R.string.settings_audio_quality),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    start = SheetTitlePadding,
                    end = SheetTitlePadding,
                    top = SheetTitleTopPadding,
                    bottom = SheetTitleBottomPadding
                )
            )

            SheetRadioOption(
                title = stringResource(R.string.settings_audio_quality_summary_original),
                description = stringResource(R.string.settings_audio_quality_description_original),
                selected = selectedQuality == AudioQuality.ORIGINAL,
                onClick = { onQualitySelected(AudioQuality.ORIGINAL) }
            )
            SheetRadioOption(
                title = stringResource(R.string.settings_audio_quality_summary_high),
                description = stringResource(R.string.settings_audio_quality_description_high),
                selected = selectedQuality == AudioQuality.HIGH,
                onClick = { onQualitySelected(AudioQuality.HIGH) }
            )
            SheetRadioOption(
                title = stringResource(R.string.settings_audio_quality_summary_medium),
                description = stringResource(R.string.settings_audio_quality_description_medium),
                selected = selectedQuality == AudioQuality.MEDIUM,
                onClick = { onQualitySelected(AudioQuality.MEDIUM) }
            )
            SheetRadioOption(
                title = stringResource(R.string.settings_audio_quality_summary_low),
                description = stringResource(R.string.settings_audio_quality_description_low),
                selected = selectedQuality == AudioQuality.LOW,
                onClick = { onQualitySelected(AudioQuality.LOW) }
            )
        }
    }
}

/**
 * Single radio option row for bottom sheet selection lists.
 *
 * @param title Primary label text.
 * @param description Optional secondary text below the title.
 * @param selected Whether this option is currently selected.
 * @param onClick Callback invoked when this option is tapped.
 */
@Composable
private fun SheetRadioOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = SheetOptionHorizontalPadding,
                vertical = SheetOptionVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(SheetRadioTextGap))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(SheetDescriptionTopSpacing))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// endregion

// region Disconnect Dialog

/**
 * Confirmation dialog for disconnecting from the server.
 *
 * Uses M3 [AlertDialog] with error-colored confirm button.
 */
@Composable
private fun DisconnectDialog(
    serverUrl: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.disconnect_title)) },
        text = {
            Text(text = stringResource(R.string.disconnect_message, serverUrl))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.disconnect_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.disconnect_cancel))
            }
        }
    )
}

// endregion

// region Helper functions

/**
 * Returns the localized summary string for the given audio quality.
 */
@Composable
private fun audioQualitySummaryResource(quality: AudioQuality): String = when (quality) {
    AudioQuality.ORIGINAL -> stringResource(R.string.settings_audio_quality_summary_original)
    AudioQuality.HIGH -> stringResource(R.string.settings_audio_quality_summary_high)
    AudioQuality.MEDIUM -> stringResource(R.string.settings_audio_quality_summary_medium)
    AudioQuality.LOW -> stringResource(R.string.settings_audio_quality_summary_low)
}

/**
 * Returns the localized summary string for the given theme preference.
 */
@Composable
private fun themeSummaryResource(preference: ThemePreference): String = when (preference) {
    ThemePreference.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemePreference.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemePreference.DARK -> stringResource(R.string.settings_theme_dark)
}

// endregion
