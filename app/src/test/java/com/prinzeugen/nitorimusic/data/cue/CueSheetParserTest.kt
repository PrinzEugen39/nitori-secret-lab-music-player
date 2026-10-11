package com.prinzeugen.nitorimusic.data.cue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.charset.Charset

class CueSheetParserTest {

    @Test
    fun testParseStandardCue() {
        val sampleCue = """
            REM GENRE Touhou
            REM DATE 2021
            PERFORMER "Yuuhei Satellite"
            TITLE "Beyond the Rainbow"
            FILE "Beyond the Rainbow.flac" WAVE
              TRACK 01 AUDIO
                TITLE "Track One"
                PERFORMER "Artist A"
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                TITLE "Track Two"
                PERFORMER "Artist B"
                INDEX 01 03:30:00
              TRACK 03 AUDIO
                TITLE "Track Three"
                PERFORMER "Artist C"
                INDEX 01 07:15:37
        """.trimIndent()

        val cueSheet = CueSheetParser.parseBytes(sampleCue.toByteArray(Charsets.UTF_8))
        assertNotNull(cueSheet)
        cueSheet!!

        assertEquals("Beyond the Rainbow", cueSheet.albumTitle)
        assertEquals("Yuuhei Satellite", cueSheet.albumArtist)
        assertEquals("Beyond the Rainbow.flac", cueSheet.audioFileName)
        assertEquals(2021, cueSheet.year)
        assertEquals(3, cueSheet.tracks.size)

        // Track 1: 00:00:00 to 03:30:00 (210,000 ms)
        val t1 = cueSheet.tracks[0]
        assertEquals(1, t1.trackNo)
        assertEquals("Track One", t1.title)
        assertEquals("Artist A", t1.performer)
        assertEquals(0L, t1.startMs)
        assertEquals(210_000L, t1.endMs)

        // Track 2: 03:30:00 to 07:15:37
        val t2 = cueSheet.tracks[1]
        assertEquals(2, t2.trackNo)
        assertEquals(210_000L, t2.startMs)
        // 7*60*1000 + 15*1000 + 37*1000/75 = 420000 + 15000 + 493 = 435493
        val expectedT3Start = (7 * 60_000L) + (15 * 1_000L) + (37 * 1_000L / 75L)
        assertEquals(expectedT3Start, t2.endMs)

        // Track 3: endMs should be 0 until filled with parent duration
        val t3 = cueSheet.tracks[2]
        assertEquals(3, t3.trackNo)
        assertEquals(expectedT3Start, t3.startMs)
        assertEquals(0L, t3.endMs)
    }

    @Test
    fun testParseShiftJISCue() {
        // Japanese text encoded in Shift-JIS / Windows-31J
        val sampleCue = """
            PERFORMER "幽閉サテライト"
            TITLE "虹の彼方に"
            FILE "CDImage.flac" WAVE
              TRACK 01 AUDIO
                TITLE "夕月椿"
                PERFORMER "築山さえ"
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                TITLE "零れ桜"
                PERFORMER "Marcia"
                INDEX 01 04:12:00
        """.trimIndent()

        val sjisBytes = sampleCue.toByteArray(Charset.forName("Windows-31J"))
        val cueSheet = CueSheetParser.parseBytes(sjisBytes)
        assertNotNull(cueSheet)
        cueSheet!!

        assertEquals("虹の彼方に", cueSheet.albumTitle)
        assertEquals("幽閉サテライト", cueSheet.albumArtist)
        assertEquals("CDImage.flac", cueSheet.audioFileName)
        assertEquals(2, cueSheet.tracks.size)

        assertEquals("夕月椿", cueSheet.tracks[0].title)
        assertEquals("築山さえ", cueSheet.tracks[0].performer)
        assertEquals("零れ桜", cueSheet.tracks[1].title)
        assertEquals("Marcia", cueSheet.tracks[1].performer)
    }

    @Test
    fun testParseFileDirectiveAndBOM() {
        // UTF-8 with BOM and absolute path in FILE
        val sampleCue = """
            REM YEAR 2018
            PERFORMER "Alstroemeria Records"
            TITLE "Bad Apple!!"
            FILE "D:\Music\Rips\Bad Apple.flac" WAVE
              TRACK 01 AUDIO
                TITLE "Bad Apple!! feat. nomico"
                INDEX 00 00:00:00
                INDEX 01 00:00:32
        """.trimIndent()

        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val contentBytes = sampleCue.toByteArray(Charsets.UTF_8)
        val withBom = bom + contentBytes

        val cueSheet = CueSheetParser.parseBytes(withBom)
        assertNotNull(cueSheet)
        cueSheet!!

        assertEquals("Bad Apple!!", cueSheet.albumTitle)
        assertEquals("Alstroemeria Records", cueSheet.albumArtist)
        assertEquals("Bad Apple.flac", cueSheet.audioFileName) // Basename extracted
        assertEquals(2018, cueSheet.year)
        assertEquals(1, cueSheet.tracks.size)
        assertEquals(1, cueSheet.tracks[0].trackNo)
        assertEquals("Bad Apple!! feat. nomico", cueSheet.tracks[0].title)
        assertEquals("Alstroemeria Records", cueSheet.tracks[0].performer)
        // INDEX 01 was 00:00:32 => 32 * 1000 = 32000 ms
        val expectedStart = 32 * 1000L / 75L
        assertEquals(expectedStart, cueSheet.tracks[0].startMs)
    }

    @Test
    fun testParseActualYuuheiSatelliteCue() {
        val file = java.io.File("cue_sample.cue")
        if (!file.exists()) return
        val cue = CueSheetParser.parse(file)
        assertNotNull(cue)
        cue!!
        assertEquals("永遠の都で", cue.albumTitle)
        assertEquals("幽閉サテライト", cue.albumArtist)
        assertEquals("幽閉サテライト - 永遠の都で.flac", cue.audioFileName)
        assertEquals(12, cue.tracks.size)

        // Track 1
        assertEquals("永遠の都で", cue.tracks[0].title)
        assertEquals("Marcia", cue.tracks[0].performer)
        assertEquals(0L, cue.tracks[0].startMs)

        // Track 2
        assertEquals("砂の城", cue.tracks[1].title)
        assertEquals("兎明", cue.tracks[1].performer)
        // 04:56:43 = (4 * 60 + 56) * 1000 + 43 * 1000 / 75 = 296000 + 573 = 296573
        val expectedT2Start = (4 * 60_000L) + (56 * 1_000L) + (43 * 1_000L / 75L)
        assertEquals(expectedT2Start, cue.tracks[1].startMs)
        assertEquals(expectedT2Start, cue.tracks[0].endMs)

        // Track 12
        assertEquals("Crisis!! (Instrumental)", cue.tracks[11].title)
        assertEquals(12, cue.tracks[11].trackNo)
    }

    @Test
    fun testMultiFileCueDetection() {
        val multiFileCue = """
            REM GENRE Touhou
            PERFORMER "Various"
            TITLE "Spring Sounds"
            FILE "01 Track One.wav" WAVE
              TRACK 01 AUDIO
                TITLE "Track One"
                INDEX 01 00:00:00
            FILE "02 Track Two.wav" WAVE
              TRACK 02 AUDIO
                TITLE "Track Two"
                INDEX 01 00:00:00
        """.trimIndent()

        val cue = CueSheetParser.parseBytes(multiFileCue.toByteArray(Charsets.UTF_8))
        assertNotNull(cue)
        cue!!
        assertEquals(2, cue.audioFileCount)
        assertEquals(false, cue.hasCorruptTitles())
    }

    @Test
    fun testCorruptTitlesDetection() {
        val corruptCue = """
            REM GENRE Touhou
            PERFORMER "......"
            TITLE "SPRING SOUNDS"
            FILE "01.wav" WAVE
              TRACK 01 AUDIO
                TITLE "...."
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                TITLE "........."
                INDEX 01 03:00:00
              TRACK 03 AUDIO
                TITLE "Real Song"
                INDEX 01 06:00:00
        """.trimIndent()

        val cue = CueSheetParser.parseBytes(corruptCue.toByteArray(Charsets.UTF_8))
        assertNotNull(cue)
        cue!!
        assertEquals(true, cue.hasCorruptTitles())
    }
}

