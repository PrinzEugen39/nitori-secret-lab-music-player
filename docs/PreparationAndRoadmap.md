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
| **Audio Engine** | Media3 1.5.0 (ExoPlayer + `MediaSessionService`) | Background service, lockscreen controls, notification |
| **Database & Cache** | Room 2.6.1 (SQLite) dengan KSP | Caching metadata 0ms & antrean persisten |
| **Scan Metadata** | `MediaStore` + `MediaMetadataRetriever` | Ekstraksi native tag FLAC/ALAC/MP3 |
| **Artwork & Palette** | Multi-tier Fallback + `ArtworkMemoryCache` (`RGB_565`) + `Palette API` | Dynamic ambient dominant color & ramah RAM |
| **State Preservation** | `SaveableStateHolder` multi-layer | Menjaga posisi scroll saat navigasi layar/tab/detail |
| **Preferensi** | DataStore Preferences 1.1.1 | Gapless, Audio Focus, Headset unplug, Smart Shuffle |

---

## 3. Status Implementasi Fitur

### ✅ Telah Selesai (v1.0 MVP):
1. **Audio Playback:** Playback background stabil, gapless playback, audio focus management, auto-pause cabut headset/TWS.
2. **Audio Specs Badge:** Deteksi codec, sample rate, bit depth, dan bitrate real-time (misal `FLAC • 24-bit • 44.1 kHz`) di Now Playing.
3. **Persistensi Antrean (Queue):** Room DB `SavedQueueItem` menyimpan urutan lagu saat aplikasi ditutup/reboot; fitur drag-reorder, swipe-delete, dan tap-jump.
4. **Smart Shuffle:** Slider sebaran (spread) untuk menghindari artis atau album berurutan, terintegrasi di halaman Settings.
5. **Caching Library (0ms Startup):** Membuka library instan dari Room DB tanpa loading berulang; background sync dengan MediaStore.
6. **Preservasi Posisi Scroll:** Posisi scroll tidak lagi ter-reset ke atas saat berpindah ke Now Playing, berpindah tab, maupun saat keluar dari detail album/artis.
7. **Multi-tier Album Art & Custom Picker:** Mengatasi cover FLAC yang tidak terindeks oleh Android MediaStore via `MediaMetadataRetriever` embedded picture; user dapat mengganti cover album langsung dari galeri.
8. **Poweramp Bulk Selection Mode:**
   - Di tab **Albums**: Checkbox overlay, counter, Queue, Play Next, dan Image picker (khusus saat 1 album dipilih).
   - Di tab **Tracks**: Checkbox overlay, counter, Queue, dan Play Next.
9. **Single Album Context Menu:** Thumbnail, total durasi, jumlah lagu, tahun, Play Next, Queue, Rescan, dan Album Art picker.
10. **Now Playing UI Polish:**
    - Dynamic Ambient Background dengan Material You Palette API yang beradaptasi dengan warna cover album.
    - Live Countdown Badge Sleep Timer (`⏳ mm:ss`) di top bar.
    - Sleep Timer volume fade-out otomatis 15 detik terakhir sebelum pause.
    - Gesture swipe HorizontalPager dengan spring snapping antar track.
    - Tombol previous langsung lompat ke lagu sebelumnya.
    - Tata letak rapat (*tight layout*) menghemat ruang vertikal.

---

## 4. Rencana Pengembangan Selanjutnya (Next Iterations)

1. [ ] **🔤 Fast Alphabet Scroller (A–Z Indexer):**
   - Bar alfabet vertikal di sisi kanan layar untuk loncat instan ke huruf awal album/lagu (sangat krusial untuk library dengan ratusan/ribuan file).
2. [ ] **📜 Sinkronisasi Lirik (Synchronized LRC):**
   - Pembacaan file `.lrc` di folder lagu atau embedded tag `LYRICS` / `USLT` di metadata file audio.
   - Tampilan lirik scrolling karaoke-style per baris mengikuti detik pemutaran.
3. [ ] **🔊 ReplayGain (Normalisasi Volume Otomatis):**
   - Membaca tag `REPLAYGAIN_TRACK_GAIN` / `R128` untuk menyamakan kenyaringan suara antar file audio secara bit-perfect tanpa pewarnaan EQ.
4. [ ] **💿 Riset & Debugging CUE Sheet Splitting:**
   - Pembacaan virtual track list dari file `.cue` + audio utuh (FLAC/WAV) tanpa memotong file fisik di disk.
5. [ ] **🚀 Release Build Optimization:**
   - Konfigurasi ProGuard/R8 code shrinking & resource shrinking untuk memangkas ukuran APK dan mempercepat cold-boot.
6. [ ] **🎧 v2 Bit-Perfect Direct USB Audio:**
   - Akses langsung USB Audio Class 2 (UAC2) via `libusb` atau Android USB Host API untuk bypass mixer sistem Android.