package com.vsulimov.resonance.core.utils

/**
 * Sanitizes a string coming from an external source (like an API) for UI display.
 * Replaces newlines with spaces and returns the [fallback] if the string is null or entirely whitespace.
 *
 * @param fallback The string to return if the current string is null or blank.
 * @return A sanitized, single-line string ready for UI display.
 */
fun String?.sanitizeForUi(fallback: String): String = this?.replace("\n", " ")?.takeIf { it.isNotBlank() } ?: fallback
