# Preparation & Roadmap

## 1. Ringkasan Kebutuhan

**Platform:** Android native (Kotlin + Jetpack Compose + Media3 ExoPlayer).

**Fokus Utama:**
- Kemurnian audio (uncolored/bit-perfect, tanpa equalizer bawaan).
- Antrean "Up next" fleksibel dan persisten.
- Pengacakan cerdas (Smart Shuffle).
- UI/UX responsif dan padat terinspirasi oleh Poweramp.

---

## 2. Tech Stack Terpasang

| Lapisan | Pustaka / Komponen | Keterangan |
|---|---|---|
| **Bahasa** | Kotlin 2.0.21 | Coroutines, Flow, StateFlow |
| **UI** | Jetpack Compose (BOM 2024.10.01, Material 3) | Deklaratif dengan fluid gesture animations |
| **Audio Engine** | Media3 1.5.0 (ExoPlayer + `MediaSessionService`) | Background service, lockscreen controls, notification, ReplayGain |
| **Database & Cache** | Room 2.6.1 (SQLite) dengan KSP | Caching metadata 0ms & antrean persisten |
| **Scan Metadata** | `MediaStore` + `MediaMetadataRetriever` | Ekstraksi native tag FLAC/ALAC/MP3 |
| **Artwork & Palette** | Multi-tier Fallback + `ArtworkMemoryCache` (`RGB_565`) + `Palette API` | Dynamic ambient dominant color & ramah RAM |
| **State Preservation** | `SaveableStateHolder` multi-layer | Menjaga posisi scroll saat navigasi layar/tab/detail |
| **Preferensi** | DataStore Preferences 1.1.1 | Gapless, Audio Focus, Headset unplug, Smart Shuffle, ReplayGain |
| **Build & Minifikasi** | R8 + ProGuard Rules | Code shrinking & resource shrinking (Release APK 4.04 MB) |

---

## 3. Status Implementasi Fitur

### ✅ Telah Selesai (v1.0 MVP):
1. **Audio Playback:** Playback background stabil, gapless playback, audio focus management, auto-pause cabut headset/TWS.
2. **Audio Specs Badge:** Deteksi codec, sample rate, bit depth, dan bitrate real-time (misal `FLAC • 24-bit • 44.1 kHz`) di Now Playing, di posisi paling bawah layar di atas navigation bar.
3. **Persistensi Antrean (Queue):** Room DB `SavedQueueItem` menyimpan urutan lagu saat aplikasi ditutup/reboot; fitur drag-reorder, swipe-delete, dan tap-jump.
4. **Smart Shuffle:** Slider sebaran (spread) untuk menghindari artis atau album berurutan, terintegrasi di halaman Settings.
5. **Caching Library (0ms Startup):** Membuka library instan dari Room DB tanpa loading berulang; background sync dengan MediaStore.
6. **Preservasi Posisi Scroll:** Posisi scroll tidak lagi ter-reset ke atas saat berpindah ke Now Playing, berpindah tab, maupun saat keluar dari detail album/artis (`SaveableStateHolder` multi-layer).
7. **Multi-tier Album Art & Custom Picker:** Mengatasi cover FLAC yang tidak terindeks oleh Android MediaStore via `MediaMetadataRetriever` embedded picture; user dapat mengganti cover album langsung dari galeri.
8. **Poweramp Bulk Selection Mode:**
   - Di tab **Albums**: Checkbox overlay, counter, Queue, Play Next, dan Image picker (khusus saat 1 album dipilih).
   - Di tab **Tracks**: Checkbox overlay, counter, Queue, dan Play Next.
9. **Single Album Context Menu:** Thumbnail, total durasi, jumlah lagu, tahun, Play Next, Queue, Rescan, dan Album Art picker.
10. **Now Playing UI Polish (Poweramp-Style):**
    - Dynamic Ambient Background dengan Material You Palette API yang beradaptasi dengan warna cover album.
    - Gesture swipe vertikal ke bawah pada cover album art untuk kembali ke Library ala Poweramp.
    - Keterangan Album & Artis tampil jelas di bawah judul lagu.
    - Top bar bersih tanpa tombol redundant (Back & Queue diakses via gestur dan persistent bottom bar).
    - Live Countdown Badge Sleep Timer (`⏳ mm:ss`) di bagian atas saat timer aktif.
    - Audio Specs Badge (Audiophile) diposisikan di paling bawah layar.
    - Sleep Timer volume fade-out otomatis 15 detik terakhir sebelum pause.
    - Gesture swipe HorizontalPager dengan spring snapping antar track.
    - Tombol previous langsung lompat ke lagu sebelumnya.
11. **Fast Alphabet Scroller (A–Z Indexer):**
    - Bar alfabet vertikal (`#`, `A`–`Z`) di sisi kanan layar untuk loncat instan ke huruf awal lagu/album/artis dengan touch & drag gestures.
    - Floating preview bubble besar di tengah-kanan saat jari menggeser indeks alfabet.
    - Terintegrasi penuh pada tab Tracks, Albums, dan Artists.
12. **Normalisasi Volume Otomatis (ReplayGain / R128):**
    - Membaca metadata ReplayGain (`REPLAYGAIN_TRACK_GAIN`, `R128_TRACK_GAIN`) dari tag ID3v2, Vorbis comment FLAC/OGG, dan MP4.
    - Menyesuaikan volume playback secara otomatis via formula `10^(gainDb / 20)` tanpa pewarnaan EQ, dengan toggle on/off di Pengaturan Audio Engine.
13. **Release Build Optimization (R8 & ProGuard):**
    - Konfigurasi `proguard-rules.pro` untuk Media3, Room, Coil, Palette, dan Coroutines.
    - Mengaktifkan `isMinifyEnabled = true` dan `isShrinkResources = true`.
    - Ukuran APK menyusut drastis dari **22.3 MB (debug)** menjadi **4.04 MB (release)** (>81% size reduction).

---

## 4. Rencana Pengembangan Selanjutnya (Next Iterations)

1. [ ] **💿 Riset & Debugging CUE Sheet Splitting:**
   - Pembacaan virtual track list dari file `.cue` + audio utuh (FLAC/WAV) tanpa memotong file fisik di disk.
2. [ ] **📜 Sinkronisasi Lirik (Synchronized LRC - Optional):**
   - Pembacaan file `.lrc` di folder lagu atau embedded tag `LYRICS` / `USLT` di metadata file audio.
3. [ ] **🎧 v2 Bit-Perfect Direct USB Audio:**
   - Akses langsung USB Audio Class 2 (UAC2) via `libusb` atau Android USB Host API untuk bypass mixer sistem Android.