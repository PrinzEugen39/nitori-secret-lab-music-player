package com.prinzeugen.nitorimusic.domain.model

enum class LibraryTab(val label: String) {
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
}

enum class ViewLayout {
    GRID,
    LIST,
}

enum class SortType(val label: String) {
    BY_NAME("By name"),
    BY_ARTIST("By artist"),
    BY_YEAR("By year"),
    BY_DATE_ADDED("By date added to Library"),
    BY_TRACK_COUNT("By number of tracks"),
    BY_DURATION("By duration"),
}

data class ListOptions(
    val sortType: SortType = SortType.BY_NAME,
    val reverse: Boolean = false,
    val layout: ViewLayout = ViewLayout.GRID,
    val gridColumns: Int = 2,
)

