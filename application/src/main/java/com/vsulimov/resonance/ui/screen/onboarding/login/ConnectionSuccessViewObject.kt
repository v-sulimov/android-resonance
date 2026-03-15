package com.vsulimov.resonance.ui.screen.onboarding.login

/**
 * Presentation-layer view object representing a successful server connection.
 *
 * Contains pre-formatted data ready for direct display in the UI,
 * decoupling the presentation from domain model structure. As the
 * application grows, additional display fields (e.g. server type badge,
 * feature flags) can be added here without modifying domain models.
 *
 * @property serverVersion Display-ready server version string (e.g. "0.53.3").
 */
data class ConnectionSuccessViewObject(
    val serverVersion: String
)
