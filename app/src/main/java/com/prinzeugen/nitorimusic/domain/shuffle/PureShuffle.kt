package com.prinzeugen.nitorimusic.domain.shuffle

import com.prinzeugen.nitorimusic.domain.model.Track

/** Fisher-Yates lewat Collections.shuffle: acak seragam. */
class PureShuffle : ShuffleEngine {
    override fun shuffle(tracks: List<Track>, first: Track?): List<Track> {
        val rest = tracks.filter { it.id != first?.id }.shuffled()
        return if (first != null) listOf(first) + rest else rest
    }
}

