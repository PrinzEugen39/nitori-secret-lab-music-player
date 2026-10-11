# Arsitektur Parsing CUE Sheet & Virtual Track Slicing

> Dokumen ini menjelaskan secara teknis dan mendalam bagaimana **Nitori Secret Lab Music Player** mengimplementasikan dukungan **CUE Sheet (.cue)** untuk album monolitik (*single-file CD image rips*) dan sistem **Folders Selection**, serupa dengan arsitektur internal pemutar musik legendaris seperti **Poweramp**, **MusicBee**, dan **foobar2000**.

---

## 1. Latar Belakang & Masalah Fundamental

### Mengapa 99% Music Player Android Tidak Mendukung CUE?
Pada sistem operasi Android, hampir seluruh aplikasi pemutar musik (*open-source* maupun komersial) mengandalkan **Android MediaStore API** (`MediaStore.Audio.Media`).

MediaStore dibangun oleh Google dengan asumsi kaku:
$$\text{1 Baris Database} \iff \text{1 Berkas Fisik Media}$$

MediaStore **sama sekali tidak memiliki konsep "Virtual Track"**. Jika sebuah album CD utuh di-rip menjadi satu berkas audio besar (`album.flac`, 60 menit) beserta berkas indeks (`album.cue`), MediaStore hanya akan mengindeks satu lagu berdurasi 60 menit dengan judul file tersebut. Berkas `.cue` diabaikan sepenuhnya sebagai teks biasa non-audio.

Untuk mendukung CUE sheet, pengembang tidak bisa hanya mengandalkan API bawaan Android, melainkan harus membangun:
1. **Parser byte-level** mandiri yang mendukung multi-encoding (terutama Shift-JIS untuk musik Jepang/Doujin).
2. **Virtual Track Database Layer** berbasis Room/SQLite untuk memotong satu file fisik menjadi puluhan baris entitas lagu independen.
3. **Playback Offset & Seeking Engine** agar ExoPlayer/Media3 memutar potongan lagu tersebut dengan durasi dan posisi slider yang terisolasi.
4. **Heuristik Kualifikasi Album Monolitik** agar berkas CUE sampah/rusak tidak merusak metadata file FLAC yang sudah terpisah.

---

## 2. Diagram Alur Sistem (End-to-End Pipeline)

```mermaid
flowchart TD
    A["Penyimpanan / MediaStore"] --> B["Folder Whitelist & Junk Filter"]
    B -->|Abaikan WhatsApp, Telegram, Rekaman| C["Identifikasi File Audio & Berkas .cue"]
    C --> D["CueSheetParser (Byte-level & Charset Detect)"]
    D --> E{"Kualifikasi Monolithic Rip?"}
    
    E -- "Multi-file / Judul Rusak / Folder Sudah Terpisah" --> F["Tolak CUE: Gunakan File Audio Asli"]
    E -- "Memenuhi Syarat Monolitik (>= 8 mnt, Cocok)" --> G["Slicing Virtual Tracks"]
    
    G --> H["Room DB Cache (cached_tracks)"]
    F --> H
    
    H --> I["UI Library (Tracks, Albums, Queue)"]
    I -->|Badge [CUE] & Metadata Independen| J["Playback Engine (Media3/ExoPlayer)"]
    J -->|Offset Seeking: startMs s/d endMs| K["Seamless Audio Playback"]
```

---

## 3. Komponen Parser: `CueSheetParser.kt`

Berkas implementasi: [`CueSheetParser.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/data/cue/CueSheetParser.kt)

### A. Deteksi Encoding Cerdas (Shift-JIS vs UTF-8)
Banyak CD audio Doujin / rilis Jepang di-rip menggunakan **EAC (Exact Audio Copy)** pada Windows versi Jepang, yang menghasilkan CUE sheet ber-encoding **Shift-JIS / Windows-31J** tanpa header BOM (*Byte Order Mark*). Jika di-parse sebagai UTF-8, teks kanji/kana akan berubah menjadi karakter rusak (*mojibake* seperti ``).

Parser kami mengimplementasikan pembacaan *raw bytes* dengan prioritas deteksi:
1. **UTF-8 BOM:** Jika 3 byte pertama adalah `0xEF, 0xBB, 0xBF`, decode sebagai UTF-8 (tanpa BOM).
2. **Strict UTF-8 Validator:** Menggunakan `CharsetDecoder` dengan `onMalformedInput(CodingErrorAction.REPORT)`. Jika seluruh byte valid UTF-8 dan mengandung karakter non-ASCII yang sah, gunakan UTF-8.
3. **Shift-JIS Fallback:** Jika decoding UTF-8 melempar error (*malformed byte sequence*), parser otomatis beralih ke `Charset.forName("Shift_JIS")` atau `Windows-31J`.
4. **Fallback Terakhir:** `ISO-8859-1` (Windows-1252) untuk karakter aksen Eropa.

### B. Konversi Waktu Indeks (CD Sector Frame ke Milidetik)
Standar CUE sheet menggunakan format waktu CD Red Book:
$$\text{INDEX 01 } mm:ss:ff$$
Di mana $ff$ adalah *frame sector* audio CD ($1\text{ detik} = 75\text{ frame}$).

Konversi ke milidetik dihitung secara presisi:
$$\text{startMs} = (mm \times 60 + ss) \times 1000 + \left\lfloor\frac{ff \times 1000}{75}\right\rfloor$$

### C. Kalkulasi Rentang Trek (Start & End Time)
Karena baris `TRACK` di CUE sheet hanya mencatat waktu mulai (`INDEX 01`), waktu selesai (`endMs`) tiap trek ditentukan dari `startMs` trek berikutnya:
$$\text{endMs}_n = \text{startMs}_{n+1}$$
Trek terakhir akan menggunakan durasi penuh dari berkas audio induk (*parent track*).

---

## 4. Heuristik Monolithic Rip (Rahasia Dapur Poweramp & MusicBee)

Berkas implementasi: [`LibraryRepository.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/data/LibraryRepository.kt)

Salah satu masalah terbesar saat mendukung CUE di Android adalah **berkas CUE palsu/rusak hasil ripping EAC**:
- EAC sering membuat berkas `.cue` di album yang **sebenarnya sudah terpotong menjadi 10 file FLAC terpisah**.
- Jika CD di-rip tanpa fitur *CD-Text* aktif di drive CD-ROM, CUE sheet tersebut hanya memuat judul kosong seperti `TITLE "...."` atau `TITLE "........."`.
- Jika player asal menelan berkas CUE tersebut, 10 file FLAC asli yang punya metadata tag Vorbis rapi malah akan dihapus dan diganti trek rusak bernama `....`.

Untuk mencegah hal ini, Nitori mengadopsi **5 Filter Kualifikasi Ketat**:

```kotlin
// 1. Tolak CUE multi-file (jika CUE mendefinisikan FILE berbeda untuk tiap track)
if (cueSheet.audioFileCount > 1) continue

// 2. Tolak CUE dengan judul rusak / tanpa CD-Text ("....", "_________")
if (cueSheet.hasCorruptTitles()) continue

// 3. Tolak jika dalam folder tersebut sudah terdapat > 2 file audio fisik
// (Menandakan album tersebut sudah berwujud individual tracks, bukan monolithic rip)
if (folderTracks.size > 2) continue

// 4. File audio kandidat wajib berdurasi panjang (Monolithic Rip)
// - Durasi harus >= posisi startMs track terakhir di CUE sheet
// - Durasi album monolitik minimal 8 menit (480.000 ms)
if (audioFile.durationMs < lastCueTrackStart || audioFile.durationMs < 480_000L) continue

// 5. Pencocokan skor kemiripan nama / album (Threshold >= 50 poin)
```

Jika lolos kualifikasi:
1. File audio induk (misal `幽閉サテライト - 永遠の都で.flac`, 55 menit) dihilangkan dari daftar lagu aktif.
2. File induk tersebut dipecah menjadi 12 lagu virtual independen dengan badge `[CUE]`.

---

## 5. Arsitektur Playback & Virtual Track Seeking

Berkas implementasi: [`PlaybackController.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/playback/PlaybackController.kt) & [`PlaybackService.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/playback/PlaybackService.kt)

### A. Virtual Media ID
Setiap trek virtual CUE memiliki identitas unik:
```
Media ID: "cue_${parentTrackId}_${trackNo}"
```
URI audio yang dikirim ke ExoPlayer tetap menunjuk ke URI file fisik induk (`content://media/external/audio/media/12345`), namun metadata domain `Track` memuat:
- `isCue = true`
- `startMs = 296574L`
- `endMs = 560254L`
- `durationMs = endMs - startMs` (durasi lagu virtual, bukan durasi file induk)

### B. Isolasi Slider & Offset Playback
Pada pemutar musik standar, slider *Now Playing* mengambil posisi langsung dari `player.currentPosition`.
Untuk virtual track CUE, posisi slider dihitung secara relatif:

$$\text{virtualPositionMs} = \max(0, \text{player.currentPosition} - \text{track.startMs})$$
$$\text{virtualDurationMs} = \text{track.endMs} - \text{track.startMs}$$

Ketika pengguna melakukan *seeking* pada slider ke menit `01:30`, perintah yang dikirim ke ExoPlayer adalah:
$$\text{seekTarget} = \text{track.startMs} + 90000\text{ ms}$$

### C. Auto-Next Boundary Transition
Ketika playback mencapai `track.endMs`:
- Player tidak membiarkan audio terus berjalan ke trek berikutnya di file fisik.
- Sistem transisi antrean secara otomatis memicu pergantian ke item antrean berikutnya (*Up Next*), memelihara konsistensi *Shuffle*, *Repeat*, dan *Queue Order*.

---

## 6. Isolasi Folder & Auto-Blacklist (Folders Selection)

Berkas implementasi: [`FolderSelectionDialog.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/ui/library/FolderSelectionDialog.kt)

### Mengapa Pemindaian Awal Lambat dan Bagaimana Solusinya?
Tanpa seleksi folder, MediaStore memindai semua file audio di perangkat (termasuk ribuan rekaman pesan suara WhatsApp, cache game, dan ringtone). Selain itu, pencarian CUE harus menelusuri seluruh direktori penyimpanan secara rekursif.

**Solusi yang Diimplementasikan:**
1. **Auto-Blacklist Regex:**
   Secara mutlak mengecualikan path yang memuat kata kunci:
   - `com.whatsapp`, `WhatsApp/Media`, `WhatsApp Audio`, `WhatsApp Voice Notes`
   - `org.telegram`, `Telegram Audio`
   - `Recordings`, `Voice Recorder`, `Call_Recordings`
   - `Notifications`, `Ringtones`, `Alarms`
   - Pola nama berkas pesan suara: `AUD-*`, `PTT-*`
2. **Folders Selection (Storage Access Framework):**
   - Pengguna menentukan folder spesifik koleksi musik (misalnya `/storage/emulated/0/Music`).
   - Scanner hanya mencari file audio dan berkas `.cue` di dalam direktori yang diizinkan.
   - Durasi scanning terpangkas drastis dari **~15 detik menjadi < 1 detik** (kecepatan setara Poweramp).
3. **Validasi Wajib Minimal 1 Folder:**
   Tombol *Save and Scan* dinonaktifkan jika tidak ada folder aktif terpilih, mencegah pemindaian kosong atau pemindaian seluruh penyimpanan tanpa sengaja.

---

## 7. Referensi Kode Terkait

| Komponen | Berkas Sumber |
|---|---|
| **CUE Parser & Charset** | [`CueSheetParser.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/data/cue/CueSheetParser.kt) |
| **Unit Test Parser & Charset** | [`CueSheetParserTest.kt`](file:///d:/04%20-%20Music%20Project/app/src/test/java/com/prinzeugen/nitorimusic/data/cue/CueSheetParserTest.kt) |
| **Monolithic Matching & Slicing** | [`LibraryRepository.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/data/LibraryRepository.kt) |
| **Filter Audio Sampah Test** | [`LibraryFilterTest.kt`](file:///d:/04%20-%20Music%20Project/app/src/test/java/com/prinzeugen/nitorimusic/data/LibraryFilterTest.kt) |
| **Folders Selection Dialog** | [`FolderSelectionDialog.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/ui/library/FolderSelectionDialog.kt) |
| **Playback Offset & Virtual Seeking**| [`PlaybackController.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/playback/PlaybackController.kt) |
| **Visual CUE Badge** | [`CueBadge.kt`](file:///d:/04%20-%20Music%20Project/app/src/main/java/com/prinzeugen/nitorimusic/ui/components/CueBadge.kt) |

---
*Dibuat untuk dokumentasi arsitektur Nitori Secret Lab Music Player.*
