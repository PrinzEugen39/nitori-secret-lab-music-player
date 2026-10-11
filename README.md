# Nitori Secret Lab Music Player

Offline-first Android music player modern yang berfokus pada kemurnian audio playback (uncolored/bit-perfect audio engine tanpa equalizer bawaan), antrean "Up next" dinamis, Smart Shuffle cerdas, serta UI/UX adaptif ala Poweramp berbasis Jetpack Compose & Media3.

Target platform: Android 10+ (Dioptimasi untuk Android 14/15, API 34-35).

---

## 🚀 Fitur yang Sudah Terimplementasi

### 1. Audio Engine & Playback
- **Media3 ExoPlayer & MediaSessionService:** Background playback tahan gangguan dengan persistent system media notification dan lockscreen controls.
- **Audio Focus & Headset Management:** Auto-pause saat headset/TWS dilepas, audio ducking saat notifikasi masuk.
- **Bit-Perfect Focus:** Tanpa equalizer bawaan untuk output audio yang murni, natural, dan jernih.
- **Gapless Playback:** Transisi tanpa jeda untuk file FLAC, ALAC, WAV, MP3.
- **Live Audio Specs Badge:** Deteksi dan tampilan real-time codec, sample rate, bit depth, dan bitrate (misal: `FLAC • 24-bit • 44.1 kHz`, `MP3 • 320 kbps`) di layar Now Playing.
- **Sleep Timer dengan Live Countdown & Volume Fade-Out:** Pilihan durasi 15–90 menit dengan badge live countdown `⏳ mm:ss` dan fade-out volume perlahan 15 detik terakhir sebelum jeda.

### 2. Antrean "Up Next" & Smart Shuffle
- **Room DB Queue Persistence:** Antrean musik tersimpan permanen di database lokal, otomatis dimuat kembali saat aplikasi dibuka.
- **Manipulasi Antrean:** Drag-and-drop reorder, swipe-to-dismiss remove, tap-to-jump.
- **Smart Shuffle:** Mesin pengacak cerdas dengan slider sebaran (spread) artis/album untuk menghindari lagu dari artis atau album yang sama terputar berurutan.

### 3. Library & Caching (0ms Startup)
- **Room DB Metadata Cache:** Membuka aplikasi terasa instan (0ms) dari cache Room DB, dengan background sync ke MediaStore.
- **Browse Lengkap:** Tab Albums, Tracks, Artists, dan Folders dengan layout Grid/List dinamis serta pengaturan sorting.
- **Preservasi Posisi Scroll:** Navigasi antar halaman (ke Now Playing, Queue, Settings) atau membuka detail album/artis tidak akan mereset posisi scroll daftar ke atas (menggunakan `SaveableStateHolder`).

### 4. Metadata & Custom Album Art
- **Multi-Tier Artwork Resolver:**
  1. Custom Artwork pilihan user (`custom_album_art/${albumId}.jpg`).
  2. MediaStore thumbnail generator (`loadThumbnail`).
  3. `MediaMetadataRetriever` native extraction byte stream (membaca cover FLAC/ALAC yang gagal diindeks MediaStore).
  4. MediaStore Audio Albumart Uri.
  5. Pencarian gambar folder (`cover.jpg`, `folder.jpg`, dll.).
- **Artwork In-Memory Cache:** `LruCache` terkompresi `RGB_565` (hemat 50% RAM) untuk scrolling lancar 120 FPS.
- **Pilih Gambar Sendiri:** Opsi mengganti cover album langsung dari galeri perangkat.

### 5. UI/UX ala Poweramp
- **Album & Track Bulk Selection Mode:** Long-press pada item album atau lagu untuk masuk ke mode multi-pilih dengan checkbox overlay, tombol All, counter, tombol Queue, Play Next, dan opsi ganti cover (khusus saat 1 album dipilih).
- **Album Context Dialog:** Menampilkan thumbnail, durasi total, jumlah lagu, tahun, Play Next, Queue, Rescan, dan picker Album Art.
- **Dynamic Material You Palette:** Latar belakang layar Now Playing mengalirkan gradasi ambient halus yang beradaptasi secara otomatis dengan warna dominan cover album yang sedang diputar.
- **Swipe Gesture Playback:** Transisi geser HorizontalPager antar artwork lagu dengan animasi physics spring snapping.

---

## 🛠️ Tech Stack

| Lapisan | Pustaka / Komponen |
|---|---|
| **Bahasa** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose (BOM 2024.10.01, Material 3) |
| **Audio Engine** | AndroidX Media3 1.5.0 (ExoPlayer + `MediaSessionService`) |
| **Database & Cache** | Room 2.6.1 (SQLite) dengan KSP |
| **Image & Color** | Coil Compose 2.7.0, AndroidX Palette 1.0.0, Custom LruCache |
| **Reorder & Animasi**| Reorderable 2.4.0, Compose Animation Physics |
| **Settings / Prefs** | AndroidX DataStore Preferences 1.1.1 |

---

## 📂 Struktur Project

```
app/src/main/java/com/prinzeugen/nitorimusic/
├── data/
│   ├── db/                 # Room Database, DAO (TrackDao, QueueDao), Entities
│   ├── cue/                # CueSheetParser (eksperimental)
│   ├── ArtworkLoader.kt    # Multi-tier artwork fallback extractor
│   ├── CustomArtworkManager.kt # Manajemen cover art kustom & LruCache
│   ├── LibraryRepository.kt# Offline-first repository (Room + MediaStore)
│   └── SettingsStore.kt    # DataStore preferensi audio & UI
├── domain/
│   ├── model/              # Domain models (Track, Album, Artist, Folder, Layout)
│   └── shuffle/            # SmartShuffle & PureShuffle engine
├── playback/
│   ├── PlaybackService.kt  # MediaSessionService (Foreground audio service)
│   └── PlaybackController.kt # Wrapper MediaController + AudioSpecs + SleepTimer
└── ui/
    ├── AppRoot.kt          # Root navigation container & State preservation
    ├── MainViewModel.kt    # State management UI
    ├── components/         # ArtworkImage, PowerampBottomBar, TrackActionMenu
    ├── library/            # LibraryScreen, Albums/Tracks/Artists/Folders tabs & Dialogs
    ├── nowplaying/         # NowPlayingScreen, QueueScreen, SleepTimerDialog
    └── settings/           # SettingsScreen (Gapless, Focus, Shuffle, Cache)
```

---

## 📖 Dokumentasi Lanjutan
- [Preparation & Roadmap](docs/PreparationAndRoadmap.md)
- [Architecture & Design](docs/Architecture.md)
- [Dev Setup Guide](docs/DevSetup.md)
