package com.vsulimov.resonance.ui.screens.library

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.theme.Dimens

private const val ACCENT_DESCRIPTION_ALPHA = 0.8f
private const val PORTRAIT_COLUMN_COUNT = 2
private const val LANDSCAPE_COLUMN_COUNT = 4

/**
 * Stateful entry composable for the Library Screen.
 *
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onCategoryClick Callback triggered when a category navigation card is selected.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel ViewModel holding and managing the screen state.
 */
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onCategoryClick = onCategoryClick,
        onOfflineModeToggle = { enabled -> viewModel.toggleOfflineMode(enabled) },
        modifier = modifier
    )
}

/**
 * Stateless content composable displaying the Library Screen navigation categories in a responsive grid.
 *
 * @param uiState Current UI state holding the category items.
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onCategoryClick Callback triggered when a category navigation card is selected.
 * @param onOfflineModeToggle Callback triggered when user toggles strict offline mode switch.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    innerPadding: PaddingValues,
    onCategoryClick: (String) -> Unit,
    onOfflineModeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columnsCount = if (isLandscape) LANDSCAPE_COLUMN_COUNT else PORTRAIT_COLUMN_COUNT

    val horizontalPadding = Dimens.paddingLarge

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsCount),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + Dimens.paddingSmall,
            bottom = innerPadding.calculateBottomPadding() + Dimens.paddingLarge,
            start = horizontalPadding + leftInset,
            end = horizontalPadding + rightInset
        )
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.topBarHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.library_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isOfflineMode) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.paddingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.iconExtraLarge)
                            .background(
                                color = if (uiState.isOfflineMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (uiState.isOfflineMode) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(Dimens.iconMedium)
                        )
                    }

                    Spacer(modifier = Modifier.width(Dimens.paddingMedium))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.library_category_offline_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.library_category_offline_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = uiState.isOfflineMode,
                        onCheckedChange = onOfflineModeToggle
                    )
                }
            }
        }

        items(
            items = uiState.categories,
            key = { category -> category.id }
        ) { category ->
            LibraryCategoryCard(
                category = category,
                onClick = { onCategoryClick(category.route) }
            )
        }
    }
}

/**
 * Reusable Material 3 Navigation Card representing a library category within the grid.
 *
 * @param category The category data model.
 * @param onClick Callback triggered when the card is clicked.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun LibraryCategoryCard(category: LibraryCategory, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (category.isAccent) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (category.isAccent) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val iconContainerColor = if (category.isAccent) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val iconTint = if (category.isAccent) {
        MaterialTheme.colorScheme.onTertiary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    val descriptionColor = if (category.isAccent) {
        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = ACCENT_DESCRIPTION_ALPHA)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.iconExtraLarge)
                        .background(
                            color = iconContainerColor,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = stringResource(R.string.cd_library_category_icon),
                        tint = iconTint,
                        modifier = Modifier.size(Dimens.iconMedium)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_navigate_to_category),
                    tint = descriptionColor,
                    modifier = Modifier.size(Dimens.iconMedium)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.paddingMedium))

            Text(
                text = stringResource(category.titleResId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimens.paddingSmall / 2))

            Text(
                text = stringResource(category.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
                color = descriptionColor,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
