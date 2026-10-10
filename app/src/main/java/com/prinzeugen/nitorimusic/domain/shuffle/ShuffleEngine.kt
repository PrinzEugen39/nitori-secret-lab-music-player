package com.prinzeugen.nitorimusic.domain.shuffle

import com.prinzeugen.nitorimusic.domain.model.Track

/**
 * Menghasilkan urutan fix sekali jalan. Lagu [first] (jika ada) ditaruh di posisi 0.
 */
interface ShuffleEngine {
    fun shuffle(tracks: List<Track>, first: Track? = null): List<Track>
}

