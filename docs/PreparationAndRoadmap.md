# Preparation & Roadmap

## 1. Ringkasan Kebutuhan

**Platform:** Android saja dulu. PC menyusul, dan MusicBee sudah memenuhi kebutuhan di PC.

**Fitur v1**
- Browse library
- Player
- Gapless playback
- Sleep timer
- Playlist dasar

**Fitur inti**
- Queue "Up next" yang bisa dilihat, di-reorder, di-remove, dan di-tap untuk diputar.
- Shuffle acak murni, ditambah opsi smart shuffle (hindari artis atau album yang sama berturutan).
- Urutan shuffle dibuat sekali, lalu bisa diedit.

**Ditunda ke v2:** direct USB audio.

## 2. Tech Stack

| Lapisan | Pilihan |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Audio | Media3 (ExoPlayer + `MediaSessionService`) |
| Database library | Room (SQLite) |
| Scan file dan tag | `MediaStore`, atau scan folder lewat SAF |
| Cover art | Coil |
| Preferensi dan state queue | DataStore, plus tabel Room untuk queue |
| Dependency injection | Hilt (opsional) |

### Kenapa Kotlin dan bukan React Native
- Queue yang bisa diedit sepenuhnya, gapless playback, dan background service adalah inti app ini. Semuanya paling mulus di Media3 native.
- `react-native-track-player` membatasi manipulasi queue.
- Direct USB audio nanti butuh kode native, jadi kamu akan masuk ke Kotlin atau C++ juga.
- Kompetensi React dan FE tetap berguna, karena konsep Compose mirip React (deklaratif, state-driven).

## 3. Yang Perlu Disiapkan
1. Android Studio dan HP untuk testing.
2. Dasar Kotlin: coroutines, Flow, dan data class (sekitar 1-2 minggu).
3. Pelajari Compose, Media3 `MediaSessionService`, dan Room.
4. Beberapa file FLAC untuk testing gapless.

## 4. Roadmap
1. **MVP:** scan library, putar lagu, notifikasi dan lockscreen controls.
2. **Queue:** UI Up next dengan drag-reorder, swipe-remove, dan tap-to-play.
3. **Shuffle:** shuffle murni, lalu smart shuffle dengan slider.
4. **Pelengkap:** gapless, sleep timer, playlist.
5. **v2:** riset USB audio (`libusb`, atau UAC2 lewat API Android).