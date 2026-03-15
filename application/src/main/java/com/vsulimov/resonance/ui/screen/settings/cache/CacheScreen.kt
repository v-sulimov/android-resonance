package com.vsulimov.resonance.ui.screen.settings.cache

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.domain.model.CacheLimit

// region Dimension constants (4dp grid)

private val ContentTopPadding = 16.dp
private val ContentHorizontalPadding = 16.dp
private val UsageCardPadding = 24.dp
private val UsageCardCornerRadius = 16.dp
private val UsageCardBottomMargin = 24.dp
private val UsageNumberBottomSpacing = 4.dp
private val ProgressBarTopSpacing = 16.dp
private val ProgressBarHeight = 8.dp
private val ProgressBarCornerRadius = 4.dp
private val ProgressLabelsTopSpacing = 8.dp
private val ClearButtonTopSpacing = 24.dp
private val ClearButtonIconSize = 20.dp
private val ClearButtonIconTextGap = 8.dp
private val DividerTopSpacing = 4.dp
private val SheetContentBottomPadding = 32.dp
private val SheetTitleHorizontalPadding = 24.dp
private val SheetTitleTopPadding = 24.dp
private val SheetTitleBottomPadding = 8.dp
private val SheetOptionHorizontalPadding = 24.dp
private val SheetOptionVerticalPadding = 12.dp
private val SheetRadioTextGap = 16.dp
private val SheetDescriptionTopSpacing = 4.dp

// endregion

/**
 * Cache management screen displaying usage information and controls.
 *
 * Follows Material 3 detail screen patterns:
 * - Top app bar with back navigation and "Cache" title
 * - Usage summary card with size, progress bar, and labels
 * - Cache limit setting with bottom sheet picker
 * - Clear cache button (FilledTonalButton)
 *
 * State is scoped to [CacheViewModel] so that data survives
 * configuration changes.
 *
 * @param onBack Callback invoked when the back navigation button is tapped.
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CacheViewModel = viewModel(factory = CacheViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val screenState by stateHolder.screenState.collectAsState()

    var showLimitSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.cache_title)) },
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
        val state = screenState
        if (state == null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CacheContent(
                state = state,
                onCacheLimitClick = { showLimitSheet = true },
                onClearCache = stateHolder::clearCache,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    if (showLimitSheet) {
        val state = screenState
        if (state != null) {
            CacheLimitBottomSheet(
                selectedLimit = state.cacheLimit,
                onLimitSelected = { limit ->
                    stateHolder.setCacheLimit(limit)
                    showLimitSheet = false
                },
                onDismiss = { showLimitSheet = false }
            )
        }
    }
}

/**
 * Main cache content with usage card, limit setting, and clear button.
 */
@Composable
private fun CacheContent(
    state: CacheScreenState,
    onCacheLimitClick: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                top = ContentTopPadding,
                start = ContentHorizontalPadding,
                end = ContentHorizontalPadding
            )
    ) {
        UsageCard(state = state)

        ListItem(
            modifier = Modifier.clickable(onClick = onCacheLimitClick),
            headlineContent = { Text(text = stringResource(R.string.cache_limit)) },
            supportingContent = { Text(text = state.cacheLimitFormatted) },
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(DividerTopSpacing))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(ClearButtonTopSpacing))

        FilledTonalButton(
            onClick = onClearCache,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                modifier = Modifier
                    .height(ClearButtonIconSize)
                    .width(ClearButtonIconSize)
            )
            Spacer(modifier = Modifier.width(ClearButtonIconTextGap))
            Text(text = stringResource(R.string.cache_clear))
        }
    }
}

/**
 * Card displaying the current cache usage with a progress bar.
 *
 * Shows the total cached size as a large number, a description line,
 * a progress bar indicating usage relative to the limit, and labels
 * for used/limit amounts.
 */
@Composable
private fun UsageCard(
    state: CacheScreenState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UsageCardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(UsageCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.cacheSizeFormatted,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(UsageNumberBottomSpacing))
            Text(
                text = stringResource(R.string.cache_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ProgressBarTopSpacing))
            LinearProgressIndicator(
                progress = { state.usageFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProgressBarHeight)
                    .clip(RoundedCornerShape(ProgressBarCornerRadius))
            )
            Spacer(modifier = Modifier.height(ProgressLabelsTopSpacing))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.cache_used_format, state.cacheSizeFormatted),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = state.cacheLimitFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.End
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(UsageCardBottomMargin))
}

// region Cache Limit Bottom Sheet

/**
 * Modal bottom sheet for selecting the cache size limit.
 *
 * Uses standard M3 [RadioButton] for accessibility compliance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CacheLimitBottomSheet(
    selectedLimit: CacheLimit,
    onLimitSelected: (CacheLimit) -> Unit,
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
                text = stringResource(R.string.cache_limit),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    start = SheetTitleHorizontalPadding,
                    end = SheetTitleHorizontalPadding,
                    top = SheetTitleTopPadding,
                    bottom = SheetTitleBottomPadding
                )
            )

            CacheLimitOption(
                title = stringResource(R.string.cache_limit_256mb),
                description = stringResource(R.string.cache_limit_256mb_description),
                selected = selectedLimit == CacheLimit.MB_256,
                onClick = { onLimitSelected(CacheLimit.MB_256) }
            )
            CacheLimitOption(
                title = stringResource(R.string.cache_limit_512mb),
                selected = selectedLimit == CacheLimit.MB_512,
                onClick = { onLimitSelected(CacheLimit.MB_512) }
            )
            CacheLimitOption(
                title = stringResource(R.string.cache_limit_1gb),
                description = stringResource(R.string.cache_limit_1gb_description),
                selected = selectedLimit == CacheLimit.GB_1,
                onClick = { onLimitSelected(CacheLimit.GB_1) }
            )
            CacheLimitOption(
                title = stringResource(R.string.cache_limit_2gb),
                selected = selectedLimit == CacheLimit.GB_2,
                onClick = { onLimitSelected(CacheLimit.GB_2) }
            )
            CacheLimitOption(
                title = stringResource(R.string.cache_limit_5gb),
                description = stringResource(R.string.cache_limit_5gb_description),
                selected = selectedLimit == CacheLimit.GB_5,
                onClick = { onLimitSelected(CacheLimit.GB_5) }
            )
            CacheLimitOption(
                title = stringResource(R.string.cache_limit_unlimited),
                description = stringResource(R.string.cache_limit_unlimited_description),
                selected = selectedLimit == CacheLimit.UNLIMITED,
                onClick = { onLimitSelected(CacheLimit.UNLIMITED) }
            )
        }
    }
}

/**
 * Single radio option row for the cache limit bottom sheet.
 */
@Composable
private fun CacheLimitOption(
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
