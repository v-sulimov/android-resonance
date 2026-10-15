package com.vsulimov.resonance.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.core.utils.FileUtils
import com.vsulimov.resonance.data.settings.AppThemeMode
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Stateful entry composable for the Settings & Profile Screen.
 *
 * @param innerPadding Padding values provided by root Scaffold.
 * @param onBackClick Callback triggered when back navigation button is tapped.
 * @param onLoggedOut Callback triggered upon confirmed user logout to reset app router.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel ViewModel holding and managing screen state.
 */
@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isDownloadLibraryDialogOpen by remember { mutableStateOf(false) }
    val appContainer = getAppContainer()
    val coroutineScope = rememberCoroutineScope()

    val userMessage = uiState.userMessage
    if (userMessage != null) {
        val messageString = userMessage.asString()
        LaunchedEffect(userMessage) {
            snackbarHostState.showSnackbar(messageString)
            viewModel.onUserMessageShown()
        }
    }

    SettingsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onToggleThemeBottomSheet = viewModel::toggleThemeBottomSheet,
        onThemeModeChange = viewModel::onThemeModeChanged,
        onDynamicColorToggle = viewModel::onDynamicColorToggled,
        onClearCacheClick = viewModel::onClearCacheClick,
        onDownloadLibraryClick = { isDownloadLibraryDialogOpen = true },
        onSignOutClick = viewModel::onSignOutClick,
        onDismissSignOutDialog = viewModel::onDismissSignOutDialog,
        onConfirmSignOut = { viewModel.onConfirmSignOut(onLoggedOut) },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    if (isDownloadLibraryDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDownloadLibraryDialogOpen = false },
            title = { Text(stringResource(R.string.dialog_download_library_title)) },
            text = { Text(stringResource(R.string.dialog_download_library_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        isDownloadLibraryDialogOpen = false
                        coroutineScope.launch {
                            appContainer.downloadRepository.enqueueEntireLibrary()
                        }
                    }
                ) {
                    Text(stringResource(R.string.dialog_download_library_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDownloadLibraryDialogOpen = false }) {
                    Text(stringResource(R.string.dialog_download_library_cancel))
                }
            }
        )
    }

    if (uiState.isThemeBottomSheetOpen) {
        ThemeBottomSheet(
            selectedThemeMode = uiState.themeMode,
            onThemeModeSelect = viewModel::onThemeModeChanged,
            onDismissRequest = viewModel::toggleThemeBottomSheet
        )
    }

    if (uiState.isSignOutDialogVisible) {
        SignOutConfirmDialog(
            onConfirm = { viewModel.onConfirmSignOut(onLoggedOut) },
            onDismiss = viewModel::onDismissSignOutDialog
        )
    }
}

/**
 * Stateless content composable for Material 3 grouped Settings list items and controls.
 *
 * @param uiState Current UI state containing preferences and user info.
 * @param innerPadding Padding values provided by root Scaffold.
 * @param snackbarHostState State for displaying transient snackbars.
 * @param onBackClick Callback triggered on back arrow click.
 * @param onToggleThemeBottomSheet Callback triggered to open theme selection sheet.
 * @param onThemeModeChange Callback triggered when a new theme mode option is selected.
 * @param onDynamicColorToggle Callback triggered when Material You toggle is switched.
 * @param onClearCacheClick Callback triggered on clearing cover art cache.
 * @param onSignOutClick Callback triggered on tapping sign out button.
 * @param onDismissSignOutDialog Callback triggered on dismissing logout confirmation modal.
 * @param onConfirmSignOut Callback triggered on confirming logout.
 * @param modifier The modifier to be applied to the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onToggleThemeBottomSheet: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onClearCacheClick: () -> Unit,
    onDownloadLibraryClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDismissSignOutDialog: () -> Unit,
    onConfirmSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    top = Dimens.paddingSmall,
                    bottom = innerPadding.calculateBottomPadding() + Dimens.paddingLarge,
                    start = leftInset,
                    end = rightInset
                )
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings_account_user_prefix, uiState.username),
                        fontWeight = FontWeight.Bold
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.settings_account_server_prefix, uiState.serverUrl),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconLarge),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.paddingSmall))

            SectionHeader(title = stringResource(R.string.settings_section_appearance))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme_title)) },
                supportingContent = { Text(stringResource(uiState.themeMode.titleResId)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onToggleThemeBottomSheet)
            )

            if (uiState.isDynamicColorSupported) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_color_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_color_desc)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.isDynamicColorEnabled,
                            onCheckedChange = onDynamicColorToggle
                        )
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.paddingSmall))

            SectionHeader(title = stringResource(R.string.settings_section_storage))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_cache_title)) },
                supportingContent = {
                    val formattedSize = FileUtils.formatFileSize(uiState.cacheSizeBytes)
                    Text(stringResource(R.string.settings_cache_desc, formattedSize))
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    OutlinedButton(onClick = onClearCacheClick) {
                        Text(stringResource(R.string.settings_btn_clear))
                    }
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.downloads_hero_title)) },
                supportingContent = { Text(stringResource(R.string.downloads_hero_desc)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    OutlinedButton(onClick = onDownloadLibraryClick) {
                        Text(stringResource(R.string.download_option_download))
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.paddingSmall))

            SectionHeader(title = stringResource(R.string.settings_section_about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_app_name)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_app_version_format, uiState.appVersion))
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.paddingSmall))

            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings_sign_out_title),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.settings_sign_out_desc),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable(onClick = onSignOutClick)
            )
        }
    }
}

/**
 * Section header text label for grouped settings categories.
 *
 * @param title Section header title text.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            horizontal = Dimens.paddingLarge,
            vertical = Dimens.paddingSmall
        )
    )
}

/**
 * Modal bottom sheet for choosing the application display theme mode.
 *
 * @param selectedThemeMode Currently selected theme mode option.
 * @param onThemeModeSelect Callback triggered when a theme mode option is chosen.
 * @param onDismissRequest Callback triggered to dismiss the bottom sheet.
 * @param modifier The modifier to be applied to the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeBottomSheet(
    selectedThemeMode: AppThemeMode,
    onThemeModeSelect: (AppThemeMode) -> Unit,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_theme_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))
            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.paddingMedium))
            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            AppThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeModeSelect(mode) }
                        .padding(
                            start = Dimens.paddingSmall / 2,
                            end = Dimens.paddingMedium,
                            top = Dimens.paddingSmall,
                            bottom = Dimens.paddingSmall
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedThemeMode == mode,
                        onClick = { onThemeModeSelect(mode) }
                    )
                    Text(
                        text = stringResource(mode.titleResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Confirmation dialog shown before logging out the current user.
 *
 * @param onConfirm Callback triggered when the user confirms sign out action.
 * @param onDismiss Callback triggered when the dialog is dismissed or cancelled.
 */
@Composable
private fun SignOutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_sign_out_dialog_title)) },
        text = { Text(stringResource(R.string.settings_sign_out_dialog_desc)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.settings_sign_out_title))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_btn_cancel))
            }
        }
    )
}
