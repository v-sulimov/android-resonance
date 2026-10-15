package com.vsulimov.resonance.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * Core design tokens for animations and motion in compliance with Material Design 3 guidelines.
 */
object Motion {
    /** Short duration for small UI component animations (200ms). */
    const val DURATION_SHORT = 200

    /** Standard duration for screen and bar transitions (350ms). */
    const val DURATION_MEDIUM = 350

    /** Extended duration for complex layout transformations (450ms). */
    const val DURATION_LONG = 450

    /** M3 Emphasized easing curve for fluid, organic motion. */
    val EASING_EMPHASIZED: Easing = FastOutSlowInEasing

    /** M3 Easing curve for elements entering the screen. */
    val EASING_ENTER: Easing = LinearOutSlowInEasing

    /** M3 Easing curve for elements exiting the screen. */
    val EASING_EXIT: Easing = FastOutLinearInEasing
}
