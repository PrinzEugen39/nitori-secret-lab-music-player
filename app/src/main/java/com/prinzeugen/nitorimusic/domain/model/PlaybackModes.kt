package com.prinzeugen.nitorimusic.domain.model

enum class ShuffleMode {
    OFF,
    ALL,
    SONGS,
    CATEGORIES,
    SONGS_AND_CATEGORIES;

    val isActive: Boolean get() = this != OFF
}

enum class PlaybackRepeatMode {
    PLAY_SINGLE_SONG,
    PLAY_CATEGORY,
    PLAY_ALL_CATEGORIES,
    REPEAT_SONG,
    REPEAT_CATEGORY,
    REPEAT_ALL_CATEGORIES;

    val isRepeating: Boolean
        get() = this == REPEAT_SONG || this == REPEAT_CATEGORY || this == REPEAT_ALL_CATEGORIES
}

