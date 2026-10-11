package com.prinzeugen.nitorimusic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterTest {

    @Test
    fun testWhatsAppAudioIsRecognizedAsJunk() {
        val waPath1 = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio"
        val waName1 = "AUD-20260607-WA0003.opus"
        assertTrue(LibraryRepository.isJunkAudio(waPath1, waName1, 45_000L))

        val waPath2 = "/storage/emulated/0/WhatsApp/Media/WhatsApp Audio"
        val waName2 = "AUD-20230101-WA0001.mp3"
        assertTrue(LibraryRepository.isJunkAudio(waPath2, waName2, 120_000L))

        val waBusiness = "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Audio"
        assertTrue(LibraryRepository.isJunkAudio(waBusiness, "AUD-test.m4a", 60_000L))
    }

    @Test
    fun testTelegramAndRecordingsAreRecognizedAsJunk() {
        val telegram = "/storage/emulated/0/Telegram/Telegram Audio"
        assertTrue(LibraryRepository.isJunkAudio(telegram, "audio_2024.mp3", 60_000L))

        val recorder = "/storage/emulated/0/Recordings/Call"
        assertTrue(LibraryRepository.isJunkAudio(recorder, "call_rec.m4a", 180_000L))

        val ringtones = "/storage/emulated/0/Ringtones"
        assertTrue(LibraryRepository.isJunkAudio(ringtones, "ring.mp3", 30_000L))
    }

    @Test
    fun testGenuineMusicFilesAreNotJunk() {
        val musicPath = "/storage/emulated/0/Music/chocofan - SPRING SOUNDS"
        val musicName = "01. Spring Sounds.flac"
        assertFalse(LibraryRepository.isJunkAudio(musicPath, musicName, 240_000L))

        val monolithPath = "/storage/emulated/0/Music/幽閉サテライト - 永遠の都で"
        val monolithName = "幽閉サテライト - 永遠の都で.flac"
        assertFalse(LibraryRepository.isJunkAudio(monolithPath, monolithName, 3_000_000L))
    }
}

