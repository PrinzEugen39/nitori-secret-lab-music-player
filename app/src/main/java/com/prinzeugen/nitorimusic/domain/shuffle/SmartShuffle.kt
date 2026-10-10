package com.prinzeugen.nitorimusic.domain.shuffle

import com.prinzeugen.nitorimusic.domain.model.Track
import kotlin.random.Random

/**
 * Shuffle yang menyebar lagu satu artis secara merata.
 *
 * Tiap lagu diberi skor posisi 0..1 lalu diurutkan. Skor = campuran dari
 * posisi acak murni dan posisi "merata" di dalam grup artisnya.
 * [spread] 0 = sama dengan acak murni, 1 = tiap artis tersebar merata.
 */
class SmartShuffle(
    private val spread: Float,
    private val random: Random = Random.Default,
) : ShuffleEngine {

    override fun shuffle(tracks: List<Track>, first: Track?): List<Track> {
        val s = spread.coerceIn(0f, 1f).toDouble()
        val scored = ArrayList<Pair<Double, Track>>(tracks.size)

        tracks.filter { it.id != first?.id }
            .groupBy { it.artist }
            .values
            .forEach { group ->
                val shuffled = group.shuffled(random)
                val n = shuffled.size
                val phase = random.nextDouble()
                shuffled.forEachIndexed { i, track ->
                    val even = (i + phase) / n
                    val rnd = random.nextDouble()
                    scored += (s * even + (1 - s) * rnd) to track
                }
            }

        val ordered = scored.sortedBy { it.first }.map { it.second }
        return if (first != null) listOf(first) + ordered else ordered
    }
}

