# Preparation & Roadmap

## 1. Ringkasan Kebutuhan

**Platform:** Android saja dulu. PC menyusul, dan MusicBee sudah memenuhi kebutuhan di PC.

**Fitur v1**
- Browse library (Albums, Tracks, Artists, Folders) dengan opsi tampilan Grid/List yang dinamis.
- Player dengan tampilan Now Playing yang rapat, gesture swipe antar track, dan audio specs decoding badge real-time (`FLAC • 16-bit • 44.1 kHz`, dll.).
- Gapless playback & Audio Focus management.
- Sleep timer.
- Room DB Caching offline-first (0ms startup) untuk seluruh metadata library dan persistensi queue antrean.
- Multi-tier Album Art Resolver & Fallback: Custom art -> MediaStore thumbnail -> `MediaMetadataRetriever` embedded picture (mengatasi cover FLAC/Blind Emotion) -> MediaStore albumart Uri -> Folder cover (`cover.jpg`/`folder.jpg`).
- Poweramp-style Album Bulk Selection mode (Select All, Counter, Queue, Play Next, Ganti Cover Art khusus 1 item terpilih).
- Poweramp-style Single Album Context Menu (Play Next, Queue, Rescan, Album Art picker, List Options).

**Fitur inti**
- Queue "Up next" yang bisa dilihat, di-reorder, di-remove, dan di-tap untuk diputar, tersimpan persisten di Room DB.
- Shuffle acak murni, ditambah opsi smart shuffle (hindari artis atau album yang sama berturutan).
- Urutan shuffle dibuat sekali, lalu bisa diedit.

**Ditunda ke v2:** Direct USB audio (`libusb` / UAC2 bit-perfect driver).
**Tidak Dibutuhkan / Ditiadakan:** Equalizer (fokus pada kemurnian audio playback / bit-perfect / uncolored output, tidak perlu fitur equalizer bawaan). Aksi playlist di bulk selection juga ditiadakan sementara.

## 2. Tech Stack

| Lapisan | Pilihan |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Audio | Media3 (ExoPlayer + `MediaSessionService`) |
| Database library | Room (SQLite) - `TrackEntity` & `SavedQueueItem` |
| Scan file dan tag | `MediaStore` + `MediaMetadataRetriever` + Folder Scanner |
| Cover art | Multi-tier Fallback (`MediaMetadataRetriever`, Custom Storage, File, MediaStore) + `ArtworkMemoryCache` (LruCache) |
| Preferensi dan state queue | DataStore, plus tabel Room untuk queue |

## 3. Status Implementasi & Roadmap
1. [x] **MVP:** Scan library, putar lagu, notifikasi dan lockscreen controls.
2. [x] **Queue:** UI Up next dengan drag-reorder, swipe-remove, tap-to-play, dan persistensi Room DB.
3. [x] **Shuffle:** Shuffle murni, smart shuffle dengan slider sebaran artis/album.
4. [x] **Audio Engine & Settings:** Gapless playback, audio focus ducking/pause, auto-pause cabut headset, real-time audio specs badge (codec, sample rate, bit depth, bitrate).
5. [x] **Library Optimization:** Room DB Cache metadata (instan 0ms load saat buka aplikasi), rescan manual & background sync.
6. [x] **Player Gestures & Polish:** Bottom bar permanent, HorizontalPager swipe gesture antar artwork dengan physics spring snapping, previous button langsung pindah lagu tanpa kembali ke 0:00 jika ditekan/swipe.
7. [x] **Metadata & Album Art:** Multi-tier embedded picture extraction untuk FLAC, folder cover art, custom image picker dari galeri perangkat, dan in-memory cache hemat RAM (`RGB_565`).
8. [x] **Poweramp Album UX:** Album bulk selection mode (Queue, Play Next, Image locked untuk >1 album) dan Single Album Context Menu.
9. [ ] **CUE Sheet & Advanced Container Parsing:** Riset & debugging parsing file CUE sheet (dijadwalkan di sesi mendatang).
10. [ ] **v2 USB Audio:** Direct USB Audio Access bit-perfect driver.