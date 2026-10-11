package com.prinzeugen.nitorimusic.data.cue

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

data class CueTrack(
    val trackNo: Int,
    val title: String,
    val performer: String,
    val startMs: Long,
    val endMs: Long = 0L,
)

data class CueSheet(
    val albumTitle: String,
    val albumArtist: String,
    val audioFileName: String,
    val year: Int = 0,
    val tracks: List<CueTrack>,
    val audioFileCount: Int = 1,
) {
    /**
     * Memeriksa apakah metadata trek di CUE sheet rusak / kosong (misal hasil EAC rip tanpa CD-Text: "....", ".........").
     */
    fun hasCorruptTitles(): Boolean {
        if (tracks.isEmpty()) return true
        val corruptCount = tracks.count { t ->
            val clean = t.title.trim()
            clean.isEmpty() || clean.all { it == '.' || it == ' ' || it == '_' || it == '?' }
        }
        return corruptCount >= (tracks.size + 1) / 2
    }
}

object CueSheetParser {

    /**
     * Decode byte array dengan auto-detection charset:
     * 1. Cek UTF-8 BOM
     * 2. Strict UTF-8 decoding (akan error jika byte adalah Shift-JIS)
     * 3. Fallback ke Windows-31J / Shift-JIS (standar album Doujin/Touhou Jepang)
     * 4. Fallback ke ISO-8859-1 jika encoding lain gagal
     */
    fun decodeBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        val utf8Decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val byteBuffer = ByteBuffer.wrap(bytes)
        return try {
            utf8Decoder.decode(byteBuffer).toString()
        } catch (_: Exception) {
            try {
                String(bytes, Charset.forName("Windows-31J"))
            } catch (_: Exception) {
                try {
                    String(bytes, Charset.forName("Shift_JIS"))
                } catch (_: Exception) {
                    String(bytes, Charsets.ISO_8859_1)
                }
            }
        }
    }

    fun parse(file: File): CueSheet? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            val bytes = file.readBytes()
            parseBytes(bytes)
        } catch (_: Exception) {
            null
        }
    }

    fun parseBytes(bytes: ByteArray): CueSheet? {
        val text = decodeBytes(bytes)
        if (text.isBlank()) return null

        var albumTitle = ""
        var albumArtist = ""
        var audioFileName = ""
        var albumYear = 0
        val audioFiles = mutableSetOf<String>()
        val rawTracks = mutableListOf<CueTrack>()

        var currentTrackNo: Int? = null
        var currentTrackTitle = ""
        var currentTrackArtist = ""
        var currentTrackStartMs = 0L

        val fileRegex = Regex("""FILE\s+"([^"]+)"|FILE\s+([^\s]+)""", RegexOption.IGNORE_CASE)
        val dateRegex = Regex("""REM\s+(?:DATE|YEAR)\s+(\d{4})""", RegexOption.IGNORE_CASE)
        val trackRegex = Regex("""TRACK\s+(\d+)""", RegexOption.IGNORE_CASE)
        val index01Regex = Regex("""INDEX\s+01\s+(\d+):(\d+):(\d+)""", RegexOption.IGNORE_CASE)
        val index00Regex = Regex("""INDEX\s+00\s+(\d+):(\d+):(\d+)""", RegexOption.IGNORE_CASE)

        fun flushTrack() {
            currentTrackNo?.let { no ->
                rawTracks += CueTrack(
                    trackNo = no,
                    title = currentTrackTitle.ifEmpty { "Track %02d".format(no) },
                    performer = currentTrackArtist.ifEmpty { albumArtist },
                    startMs = currentTrackStartMs,
                    endMs = 0L,
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
                    fileRegex.find(line)?.let { match ->
                        val extracted = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim()
                        val baseName = File(extracted).name
                        audioFiles.add(baseName)
                        if (audioFileName.isEmpty()) {
                            audioFileName = baseName
                        }
                    }
                }
                line.startsWith("REM", ignoreCase = true) -> {
                    dateRegex.find(line)?.let { match ->
                        albumYear = match.groupValues[1].toIntOrNull() ?: albumYear
                    }
                }
                line.startsWith("TRACK", ignoreCase = true) -> {
                    flushTrack()
                    trackRegex.find(line)?.let { match ->
                        currentTrackNo = match.groupValues[1].toIntOrNull()
                    }
                }
                line.startsWith("INDEX 01", ignoreCase = true) -> {
                    index01Regex.find(line)?.let { match ->
                        currentTrackStartMs = parseCueTime(
                            match.groupValues[1],
                            match.groupValues[2],
                            match.groupValues[3]
                        )
                    }
                }
                line.startsWith("INDEX 00", ignoreCase = true) && currentTrackStartMs == 0L -> {
                    index00Regex.find(line)?.let { match ->
                        currentTrackStartMs = parseCueTime(
                            match.groupValues[1],
                            match.groupValues[2],
                            match.groupValues[3]
                        )
                    }
                }
            }
        }
        flushTrack()

        if (rawTracks.isEmpty()) return null

        // Hitung batas endMs antar lagu secara berurutan
        val resolvedTracks = rawTracks.mapIndexed { index, track ->
            val nextStart = rawTracks.getOrNull(index + 1)?.startMs ?: 0L
            track.copy(endMs = nextStart)
        }

        return CueSheet(
            albumTitle = albumTitle,
            albumArtist = albumArtist,
            audioFileName = audioFileName,
            year = albumYear,
            tracks = resolvedTracks,
            audioFileCount = audioFiles.size.coerceAtLeast(1),
        )
    }

    private fun parseCueTime(minStr: String, secStr: String, frameStr: String): Long {
        val min = minStr.toLongOrNull() ?: 0L
        val sec = secStr.toLongOrNull() ?: 0L
        val frame = frameStr.toLongOrNull() ?: 0L // 75 frames per second
        return (min * 60_000L) + (sec * 1_000L) + (frame * 1_000L / 75L)
    }
}
