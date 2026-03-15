package com.vsulimov.resonance.ui.screen.albumdetail

/**
 * Presentation-layer data class for a single track row in the album detail screen.
 *
 * Contains pre-formatted display strings so the composable layer does
 * not need to perform any formatting logic. Mapped from the domain
 * [TrackSummary][com.vsulimov.resonance.domain.model.TrackSummary]
 * by [TrackViewObjectMapper][com.vsulimov.resonance.ui.mapper.TrackViewObjectMapper].
 *
 * @param id Unique track identifier (used for list keys and future playback).
 * @param trackNumber Display string for the track position (e.g. "1"), or empty if unavailable.
 * @param title Track display title.
 * @param formattedDuration Pre-formatted duration string (e.g. "3:45"), or empty if unavailable.
 */
data class TrackViewObject(
    val id: String,
    val trackNumber: String,
    val title: String,
    val formattedDuration: String
)
