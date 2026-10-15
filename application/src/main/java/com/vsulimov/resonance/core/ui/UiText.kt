package com.vsulimov.resonance.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Sealed class representing UI text that can be either a localized string resource ID or a dynamic string.
 */
sealed class UiText {

    /** Represented by a string resource ID with optional arguments. */
    data class StringResource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText()

    /** Represented by a raw dynamic string value. */
    data class DynamicString(val value: String) : UiText()

    /**
     * Resolves the string value within a Composable context using Android resources.
     *
     * @return Resolved string value.
     */
    @Composable
    fun asString(): String = when (this) {
        is StringResource -> stringResource(resId, *args.toTypedArray())
        is DynamicString -> value
    }
}
