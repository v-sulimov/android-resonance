package com.vsulimov.resonance.domain.model

/**
 * Sort/filter type for paginated album list requests.
 *
 * Maps to the Subsonic `getAlbumList2` `type` parameter. Each value
 * determines the ordering or filtering criteria applied by the server.
 */
enum class AlbumSortType {

    /** Albums sorted alphabetically by name. */
    ALPHABETICAL,

    /** Albums the user has played most recently. */
    RECENTLY_PLAYED,

    /** Albums the user has played most frequently. */
    MOST_PLAYED,

    /** Albums most recently added to the library. */
    RECENTLY_ADDED
}
