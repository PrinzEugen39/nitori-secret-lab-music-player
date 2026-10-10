# Dev Setup

## Jawaban singkat: apakah Kotlin harus pakai Android Studio?
Secara teknis tidak wajib, karena build berjalan lewat Gradle dan bisa dari terminal. Secara praktis **Android Studio tetap sangat disarankan**:

| Kebutuhan | Android Studio | VS Code / Antigravity saja |
|---|---|---|
| Install SDK, emulator, JDK | Otomatis lewat SDK Manager | Manual |
| Compose Preview | Ada | Tidak ada |
| Autocomplete dan refactor Kotlin | Sangat baik | Terbatas |
| Logcat, debugger, profiler | Lengkap | Hanya `adb logcat` di terminal |
| Layout dan Compose inspector | Ada | Tidak ada |

Alur kerja yang disarankan: **Android Studio untuk build, run, dan debug. Antigravity untuk menulis dokumen, membantu kode, dan Git.** Keduanya bisa membuka folder yang sama.

## Perangkat uji
Gunakan **HP asli (Infinix, Android 14/15)** sebagai target utama, bukan emulator. Alasannya:
- Emulator tidak mewakili perilaku audio dan gapless dengan baik.
- Library musik asli ada di HP.
- Perilaku OEM yang mematikan background service hanya terlihat di HP asli.

Emulator hanya dipakai untuk mengecek tampilan di ukuran layar lain.

## Langkah setup
1. Install Android Studio (versi terbaru yang stabil) dari https://developer.android.com/studio
2. Saat first run, biarkan SDK Platform 35 dan Build-Tools terpasang.
3. Open folder repo ini di Android Studio, lalu tunggu Gradle sync selesai.
   - Gradle memakai JDK bawaan Android Studio (JBR 17+). Java 8 di PC tidak dipakai.
4. Di HP: Settings, About phone, ketuk Build number 7 kali. Lalu Developer options, aktifkan USB debugging (atau Wireless debugging).
5. Sambungkan HP, izinkan debugging, lalu pilih HP di toolbar dan tekan Run.
6. Di Infinix, aktifkan juga:
   - Autostart untuk app ini (XOS Phone Master / Settings, App management)
   - Battery usage: Unrestricted / jangan dioptimasi

## Verifikasi
Terminal (opsional, setelah SDK terpasang):
```
.\gradlew.bat assembleDebug
adb devices
```
