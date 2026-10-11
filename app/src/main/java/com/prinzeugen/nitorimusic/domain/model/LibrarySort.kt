package com.prinzeugen.nitorimusic.domain.model

import androidx.annotation.StringRes
import com.prinzeugen.nitorimusic.R

enum class LibraryTab(@StringRes val labelRes: Int, val label: String) {
    TRACKS(R.string.tab_tracks, "Tracks"),
    ALBUMS(R.string.tab_albums, "Albums"),
    ARTISTS(R.string.tab_artists, "Artists"),
    MOST_PLAYED(R.string.tab_most_played, "Most Played"),
    FOLDERS(R.string.tab_folders, "Folders"),
}

enum class ViewLayout {
    GRID,
    LIST,
}

enum class SortType(@StringRes val labelRes: Int, val label: String) {
    BY_NAME(R.string.sort_by_name, "By title"),
    BY_ARTIST(R.string.sort_by_artist, "By artist"),
    BY_YEAR(R.string.sort_by_year, "By year"),
    BY_DATE_ADDED(R.string.sort_by_date_added, "By date added"),
    BY_TRACK_COUNT(R.string.sort_by_track_count, "By number of tracks"),
    BY_DURATION(R.string.sort_by_duration, "By duration"),
}

data class ListOptions(
    val sortType: SortType = SortType.BY_NAME,
    val reverse: Boolean = false,
    val layout: ViewLayout = ViewLayout.GRID,
    val gridColumns: Int = 2,
)

