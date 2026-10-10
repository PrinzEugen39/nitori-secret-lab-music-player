package com.prinzeugen.nitorimusic.data.cue

import java.io.File
import java.nio.charset.Charset

data class CueTrack(
    val trackNo: Int,
    val title: String,
    val performer: String,
    val startMs: Long,
)

data class CueSheet(
    val albumTitle: String,
    val albumArtist: String,
    val audioFileName: String,
    val tracks: List<CueTrack>,
)

object CueSheetParser {

    fun parse(file: File): CueSheet? {
        if (!file.exists() || !file.canRead()) return null

        // Coba baca dengan UTF-8 terlebih dahulu, fallback ke Shift-JIS / Windows-31J jika banyak huruf Jepang
        val text = try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                file.readText(Charset.forName("Shift_JIS"))
            } catch (e2: Exception) {
                return null
            }
        }

        var albumTitle = ""
        var albumArtist = ""
        var audioFileName = ""
        val tracks = mutableListOf<CueTrack>()

        var currentTrackNo: Int? = null
        var currentTrackTitle = ""
        var currentTrackArtist = ""
        var currentTrackStartMs = 0L

        fun flushTrack() {
            currentTrackNo?.let { no ->
                tracks += CueTrack(
                    trackNo = no,
                    title = currentTrackTitle.ifEmpty { "Track %02d".format(no) },
                    performer = currentTrackArtist.ifEmpty { albumArtist },
                    startMs = currentTrackStartMs,
                )
            }
            currentTrackNo = null
            currentTrackTitle = ""
            currentTrackArtist = ""
            currentTrackStartMs = 0L
        }

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("PERFORMER", ignoreCase = true) -> {
                    val value = line.substringAfter("PERFORMER").trim().removeSurrounding("\"")
                    if (currentTrackNo == null) albumArtist = value
                    else currentTrackArtist = value
                }
                line.startsWith("TITLE", ignoreCase = true) -> {
                    val value = line.substringAfter("TITLE").trim().removeSurrounding("\"")
                    if (currentTrackNo == null) albumTitle = value
                    else currentTrackTitle = value
                }
                line.startsWith("FILE", ignoreCase = true) -> {
                    audioFileName = line.substringAfter("FILE").trim()
                        .substringBeforeLast("\"")
                        .removePrefix("\"")
                }
                line.startsWith("TRACK", ignoreCase = true) -> {
                    flushTrack()
                    val parts = line.split("\\s+".toRegex())
                    currentTrackNo = parts.getOrNull(1)?.toIntOrNull()
                }
                line.startsWith("INDEX 01", ignoreCase = true) -> {
                    val timeStr = line.substringAfter("INDEX 01").trim()
                    currentTrackStartMs = parseCueTime(timeStr)
                }
            }
        }
        flushTrack()

        return if (tracks.isNotEmpty()) {
            CueSheet(
                albumTitle = albumTitle,
                albumArtist = albumArtist,
                audioFileName = audioFileName,
                tracks = tracks,
            )
        } else null
    }

    private fun parseCueTime(time: String): Long {
        val parts = time.split(":")
        if (parts.size < 3) return 0L
        val min = parts[0].toLongOrNull() ?: 0L
        val sec = parts[1].toLongOrNull() ?: 0L
        val frame = parts[2].toLongOrNull() ?: 0L // 75 frames per second
        return (min * 60 * 1000) + (sec * 1000) + (frame * 1000 / 75)
    }
}
