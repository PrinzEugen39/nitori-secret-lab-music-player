package com.prinzeugen.nitorimusic.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.data.cue.CueSheet
import com.prinzeugen.nitorimusic.data.cue.CueSheetParser
import com.prinzeugen.nitorimusic.data.db.AppDatabase
import com.prinzeugen.nitorimusic.data.db.entity.TrackEntity
import com.prinzeugen.nitorimusic.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository data lagu dengan dukungan Room DB caching (offline-first & startup instan)
 * dan parsing CUE Sheet (.cue) otomatis ala Poweramp / MusicBee.
 */
class LibraryRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val trackDao = db.trackDao()

    companion object {
        private const val TAG = "CUE_SCAN"

        private val JUNK_FOLDER_KEYWORDS = listOf(
            "com.whatsapp",
            "whatsapp/media",
            "whatsapp audio",
            "whatsapp voice notes",
            "org.telegram",
            "telegram/telegram audio",
            "telegram audio",
            "/recordings",
            "/voice recorder",
            "/sound recorder",
            "/call_recordings",
            "/call recordings",
            "/notifications",
            "/ringtones",
            "/alarms",
            "/android/data",
            "/android/obb",
        )

        /**
         * Memeriksa apakah audio merupakan file sampah non-musik
         * (misal pesan suara WhatsApp, audio Telegram, rekaman telepon, nada dering).
         */
        fun isJunkAudio(folderPath: String, titleOrName: String = "", durationMs: Long = 60_000L): Boolean {
            val normFolder = folderPath.lowercase().replace('\\', '/')
            val normName = titleOrName.lowercase()

            if (JUNK_FOLDER_KEYWORDS.any { normFolder.contains(it) }) return true

            // WhatsApp Voice Notes naming format (AUD-..., PTT-...)
            if ((normName.startsWith("aud-") || normName.startsWith("ptt-")) &&
                (normFolder.contains("whatsapp") || normFolder.contains("media"))
            ) {
                return true
            }

            // Audio sangat pendek (< 10 detik) yang berada di luar folder musik
            if (durationMs in 1..9_999L && !normFolder.contains("music")) {
                return true
            }

            return false
        }
    }

    /** Mengambil daftar lagu dari cache database Room lokal (sangat cepat, 0-10ms). */
    suspend fun getCachedTracks(enabledFolders: List<String> = emptyList()): List<Track> = withContext(Dispatchers.IO) {
        trackDao.getAllTracks().map { it.toDomain() }.filter { track ->
            if (isJunkAudio(track.folderPath, track.title, track.durationMs)) return@filter false
            if (enabledFolders.isNotEmpty()) {
                val normFolder = track.folderPath.lowercase().trimEnd('/', '\\')
                enabledFolders.any { selected ->
                    val normSelected = selected.lowercase().trimEnd('/', '\\')
                    normFolder == normSelected || normFolder.startsWith("$normSelected/")
                }
            } else {
                true
            }
        }
    }

    /** Menghitung jumlah lagu tersimpan di database cache. */
    suspend fun getCachedTrackCount(): Int = withContext(Dispatchers.IO) {
        trackDao.getTrackCount()
    }

    private data class CueFileInfo(
        val path: String,
        val folder: String,
        val fileName: String,
        val contentUri: Uri?
    )

    /**
     * Memindai seluruh lagu dari sistem MediaStore Android,
     * mencari dan mem-parsing berkas CUE sheet (.cue),
     * lalu memperbarui cache database Room.
     */
    suspend fun syncFromMediaStore(
        enabledFolders: List<String> = emptyList(),
        onProgress: ((String) -> Unit)? = null
    ): List<Track> = withContext(Dispatchers.IO) {
        onProgress?.invoke(context.getString(R.string.scan_reading_audio))
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        // Simpan statistik pemutaran yang sudah ada agar tidak hilang saat re-sync
        val existingStats = trackDao.getAllTracks().associate { it.id to Pair(it.playCount, it.lastPlayedMs) }

        val tracks = mutableListOf<Track>()
        val trackFileNames = mutableMapOf<Long, String>()
        val folders = mutableSetOf<String>()

        context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
            val sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val rawPath = if (dataCol >= 0) c.getString(dataCol) ?: "" else ""
                val folder = if (rawPath.contains("/")) rawPath.substringBeforeLast("/") else ""
                val fileName = if (rawPath.contains("/")) rawPath.substringAfterLast("/") else ""
                val rawTrackNo = if (trackCol >= 0) c.getInt(trackCol) else 0
                val trackNo = if (rawTrackNo >= 1000) rawTrackNo % 1000 else rawTrackNo
                val durationMs = c.getLong(durCol)
                val sizeBytes = if (sizeCol >= 0) c.getLong(sizeCol) else 0L

                // 1. Abaikan berkas sampah non-musik (WhatsApp, Telegram, rekaman suara, dsb)
                if (isJunkAudio(folder, fileName, durationMs)) {
                    continue
                }

                // 2. Batasi hanya dalam folder terpilih (atau default folder Music jika belum diatur)
                val effectiveFolders = if (enabledFolders.isNotEmpty()) {
                    enabledFolders
                } else {
                    listOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath,
                        "/storage/emulated/0/Music",
                    )
                }

                val isInSelectedFolder = effectiveFolders.any { selected ->
                    val normSelected = selected.lowercase().trimEnd('/', '\\')
                    val normFolder = folder.lowercase().trimEnd('/', '\\')
                    normFolder == normSelected || normFolder.startsWith("$normSelected/")
                }
                if (!isInSelectedFolder) {
                    continue
                }

                val stat = existingStats[id]

                if (folder.isNotEmpty()) {
                    folders.add(folder)
                }
                if (fileName.isNotEmpty()) {
                    trackFileNames[id] = fileName
                }

                tracks += Track(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = c.getString(titleCol) ?: "Unknown",
                    artist = c.getString(artistCol) ?: "Unknown artist",
                    album = c.getString(albumCol) ?: "Unknown album",
                    albumId = c.getLong(albumIdCol),
                    durationMs = c.getLong(durCol),
                    trackNo = trackNo,
                    year = if (yearCol >= 0) c.getInt(yearCol) else 0,
                    dateAddedSec = if (dateAddedCol >= 0) c.getLong(dateAddedCol) else 0L,
                    folderPath = folder,
                    sizeBytes = sizeBytes,
                    playCount = stat?.first ?: 0,
                    lastPlayedMs = stat?.second ?: 0L,
                )
            }
        }

        // ==========================================
        // CUE SHEET DISCOVERY & VIRTUAL TRACK SPLIT
        // ==========================================
        val cueFiles = mutableListOf<CueFileInfo>()

        // Helper rekursif untuk mencari berkas .cue di direktori
        fun scanDirForCues(dir: File, currentDepth: Int = 0, maxDepth: Int = 4) {
            if (!dir.exists() || !dir.isDirectory || currentDepth > maxDepth) return
            try {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".cue", ignoreCase = true)) {
                        if (cueFiles.none { it.path.equals(file.absolutePath, ignoreCase = true) }) {
                            cueFiles.add(
                                CueFileInfo(
                                    path = file.absolutePath,
                                    folder = dir.absolutePath,
                                    fileName = file.name,
                                    contentUri = null
                                )
                            )
                            Log.d(TAG, "Ditemukan berkas CUE fisik: ${file.absolutePath}")
                        }
                    } else if (file.isDirectory && !file.name.startsWith(".")) {
                        scanDirForCues(file, currentDepth + 1, maxDepth)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal memindai direktori ${dir.absolutePath}: ${e.message}")
            }
        }

        // 1. Pindai folder-folder dari lagu yang ditemukan
        folders.forEach { folderPath ->
            if (!isJunkAudio(folderPath)) {
                scanDirForCues(File(folderPath))
                // Juga cek 1 level di atas folder (parent directory)
                File(folderPath).parentFile?.let { parent ->
                    if (!isJunkAudio(parent.absolutePath)) {
                        scanDirForCues(parent, currentDepth = 0, maxDepth = 1)
                    }
                }
            }
        }

        // 2. Pindai lokasi folder pilihan / standar musik
        val searchDirs = if (enabledFolders.isNotEmpty()) {
            enabledFolders.map { File(it) }
        } else {
            listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                File("/storage/emulated/0/Music"),
            )
        }
        searchDirs.forEach { dir ->
            if (dir.exists() && !isJunkAudio(dir.absolutePath)) {
                scanDirForCues(dir, currentDepth = 0, maxDepth = 4)
            }
        }

        // 3. Fallback: Temukan file .cue melalui MediaStore.Files (kompatibel Scoped Storage)
        try {
            val filesUri = MediaStore.Files.getContentUri("external")
            val cueProj = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
            )
            val cueSel = "${MediaStore.Files.FileColumns.DATA} LIKE '%.cue' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.cue'"
            context.contentResolver.query(filesUri, cueProj, cueSel, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val p = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    val resolvedName = name.ifEmpty { p.substringAfterLast("/") }
                    val resolvedFolder = if (p.contains("/")) p.substringBeforeLast("/") else ""

                    if (isJunkAudio(resolvedFolder, resolvedName)) continue

                    if (enabledFolders.isNotEmpty()) {
                        val isInside = enabledFolders.any { selected ->
                            val normSelected = selected.lowercase().trimEnd('/', '\\')
                            val normFolder = resolvedFolder.lowercase().trimEnd('/', '\\')
                            normFolder == normSelected || normFolder.startsWith("$normSelected/")
                        }
                        if (!isInside) continue
                    }

                    val uri = ContentUris.withAppendedId(filesUri, id)
                    if (p.isNotEmpty() || resolvedName.isNotEmpty()) {
                        if (cueFiles.none { it.path.equals(p, ignoreCase = true) }) {
                            cueFiles.add(CueFileInfo(p, resolvedFolder, resolvedName, uri))
                            Log.d(TAG, "Ditemukan berkas CUE dari MediaStore.Files: $p")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal query MediaStore.Files: ${e.message}")
        }

        Log.d(TAG, "Total file CUE ditemukan: ${cueFiles.size}")
        if (cueFiles.isNotEmpty()) {
            onProgress?.invoke(context.getString(R.string.scan_processing_cue, cueFiles.size))
        }

        val finalTracks = ArrayList(tracks)
        val replacedParentIds = mutableSetOf<Long>()

        // 4. Parse setiap CUE Sheet dan gantikan parent monolithic audio dengan virtual tracks
        cueFiles.forEach { cueInfo ->
            val bytes: ByteArray? = try {
                val f = File(cueInfo.path)
                if (f.exists() && f.canRead()) {
                    f.readBytes()
                } else if (cueInfo.contentUri != null) {
                    context.contentResolver.openInputStream(cueInfo.contentUri)?.use { it.readBytes() }
                } else null
            } catch (_: Exception) {
                try {
                    if (cueInfo.contentUri != null) {
                        context.contentResolver.openInputStream(cueInfo.contentUri)?.use { it.readBytes() }
                    } else null
                } catch (_: Exception) {
                    null
                }
            }

            if (bytes != null && bytes.isNotEmpty()) {
                val cueSheet = CueSheetParser.parseBytes(bytes)
                if (cueSheet != null && cueSheet.tracks.isNotEmpty()) {
                    Log.d(TAG, "Ditemukan CUE: ${cueInfo.fileName} (${cueSheet.albumTitle}, ${cueSheet.tracks.size} tracks, ${cueSheet.audioFileCount} files)")

                    // 1. Lewati jika CUE berformat multi-file (masing-masing lagu sudah dibuat file terpisah oleh ripper)
                    if (cueSheet.audioFileCount > 1) {
                        Log.d(TAG, "Lewati CUE '${cueInfo.fileName}': Format multi-file (${cueSheet.audioFileCount} berkas), berkas audio sudah terpisah.")
                        return@forEach
                    }

                    // 2. Lewati jika metadata judul trek rusak/tidak ada (misal hanya titik '....' hasil rip tanpa CD-Text)
                    if (cueSheet.hasCorruptTitles()) {
                        Log.w(TAG, "Lewati CUE '${cueInfo.fileName}': Metadata judul trek tidak valid/rusak (hanya titik). Mengutamakan metadata asli file audio.")
                        return@forEach
                    }

                    // Cari calon lagu di folder CUE sheet
                    val folderTracks = tracks.filter { t ->
                        t.folderPath.equals(cueInfo.folder, ignoreCase = true) ||
                                (t.folderPath.isNotEmpty() && cueInfo.folder.isNotEmpty() &&
                                        File(t.folderPath).name.equals(File(cueInfo.folder).name, ignoreCase = true))
                    }

                    // 3. Lewati jika folder sudah memiliki berkas audio individual (> 2 lagu terpisah)
                    // Poweramp & MusicBee hanya memecah CUE jika folder berisi 1 (atau maks 2) berkas monolithic image rip
                    if (folderTracks.size > 2) {
                        Log.d(TAG, "Lewati CUE '${cueInfo.fileName}': Folder sudah memiliki ${folderTracks.size} berkas audio individual.")
                        return@forEach
                    }

                    val candidates = if (folderTracks.isNotEmpty()) folderTracks else tracks.filter { t ->
                        cueSheet.albumTitle.isNotBlank() && t.album.equals(cueSheet.albumTitle, ignoreCase = true)
                    }

                    if (candidates.size > 2) {
                        Log.d(TAG, "Lewati CUE '${cueInfo.fileName}': Terdapat ${candidates.size} lagu aktif untuk album ini di library.")
                        return@forEach
                    }

                    // Algoritma Smart Scoring Matcher untuk menentukan lagu monolithic parent yang tepat
                    var bestScore = 0
                    var bestMatch: Track? = null

                    val cueAudioName = cueSheet.audioFileName
                    val cueAudioBase = File(cueAudioName).nameWithoutExtension
                    val cueBaseName = File(cueInfo.fileName).nameWithoutExtension
                    val lastCueTrackStart = cueSheet.tracks.lastOrNull()?.startMs ?: 0L

                    for (t in candidates) {
                        if (replacedParentIds.contains(t.id)) continue

                        // 4. Syarat mutlak Monolithic Image Rip:
                        // - Durasi file audio harus memuat seluruh trek di CUE sheet (durasi >= startMs trek terakhir)
                        if (lastCueTrackStart > 0L && t.durationMs < lastCueTrackStart) {
                            continue
                        }
                        // - Durasi audio album monolitik harus panjang (minimal 8 menit / 480.000 ms)
                        if (t.durationMs < 480_000L) {
                            continue
                        }

                        var score = 0
                        val tFileName = trackFileNames[t.id] ?: ""
                        val tBaseName = File(tFileName).nameWithoutExtension

                        // 1. Nama file persis atau base name sama
                        if (cueAudioName.isNotEmpty() && tFileName.equals(cueAudioName, ignoreCase = true)) {
                            score += 100
                        }
                        if (cueAudioBase.isNotEmpty() && tBaseName.equals(cueAudioBase, ignoreCase = true)) {
                            score += 90
                        }
                        if (cueBaseName.isNotEmpty() && tBaseName.equals(cueBaseName, ignoreCase = true)) {
                            score += 90
                        }

                        // 2. Substring matching (misal "幽閉サテライト - 永遠の都で" mengandung "永遠の都で")
                        if (cueBaseName.isNotEmpty() && (tBaseName.contains(cueBaseName, ignoreCase = true) || cueBaseName.contains(tBaseName, ignoreCase = true))) {
                            score += 60
                        }
                        if (cueAudioBase.isNotEmpty() && (tBaseName.contains(cueAudioBase, ignoreCase = true) || cueAudioBase.contains(tBaseName, ignoreCase = true))) {
                            score += 60
                        }

                        // 3. Album title matching
                        if (cueSheet.albumTitle.isNotBlank()) {
                            if (t.album.equals(cueSheet.albumTitle, ignoreCase = true)) {
                                score += 50
                            } else if (t.album.contains(cueSheet.albumTitle, ignoreCase = true) || cueSheet.albumTitle.contains(t.album, ignoreCase = true)) {
                                score += 30
                            }
                            if (t.title.contains(cueSheet.albumTitle, ignoreCase = true)) {
                                score += 25
                            }
                        }

                        // 4. Durasi album rip panjang (> 10 menit)
                        if (t.durationMs > 600_000L) {
                            score += 40
                        }

                        // 5. Jika satu-satunya lagu dalam folder tersebut
                        if (candidates.size == 1) {
                            score += 30
                        }

                        if (score > bestScore) {
                            bestScore = score
                            bestMatch = t
                        }
                    }

                    val parentTrack = if (bestScore >= 50) bestMatch else null

                    if (parentTrack != null && !replacedParentIds.contains(parentTrack.id)) {
                        Log.d(TAG, "Cocok! Parent '${parentTrack.title}' (id=${parentTrack.id}, score=$bestScore) digantikan ${cueSheet.tracks.size} virtual tracks")
                        replacedParentIds.add(parentTrack.id)
                        finalTracks.removeAll { it.id == parentTrack.id }

                        val cueTracks = cueSheet.tracks.map { cueTrack ->
                            val cueTrackId = -(parentTrack.id * 1000L + cueTrack.trackNo)
                            val trackEndMs = if (cueTrack.endMs > 0L) cueTrack.endMs else parentTrack.durationMs
                            val trackDuration = (trackEndMs - cueTrack.startMs).coerceAtLeast(0L)
                            val stat = existingStats[cueTrackId]

                            Track(
                                id = cueTrackId,
                                uri = parentTrack.uri,
                                title = cueTrack.title,
                                artist = cueTrack.performer.ifEmpty { parentTrack.artist },
                                album = cueSheet.albumTitle.ifEmpty { parentTrack.album },
                                albumId = parentTrack.albumId,
                                durationMs = trackDuration,
                                trackNo = cueTrack.trackNo,
                                year = if (cueSheet.year > 0) cueSheet.year else parentTrack.year,
                                dateAddedSec = parentTrack.dateAddedSec,
                                folderPath = parentTrack.folderPath,
                                sizeBytes = if (cueSheet.tracks.isNotEmpty()) parentTrack.sizeBytes / cueSheet.tracks.size else 0L,
                                playCount = stat?.first ?: 0,
                                lastPlayedMs = stat?.second ?: 0L,
                                startMs = cueTrack.startMs,
                                endMs = trackEndMs,
                                isCue = true,
                                parentTrackId = parentTrack.id,
                            )
                        }

                        finalTracks.addAll(cueTracks)
                    } else {
                        Log.w(TAG, "Tidak ditemukan lagu parent yang cocok untuk CUE ${cueInfo.fileName} (bestScore=$bestScore)")
                    }
                }
            }
        }

        // Simpan ke cache Room DB
        onProgress?.invoke(context.getString(R.string.scan_syncing_library))
        val entities = finalTracks.map { TrackEntity.fromDomain(it) }
        trackDao.replaceAll(entities)

        Log.d(TAG, "Sinkronisasi selesai: ${finalTracks.size} lagu aktif (termasuk virtual CUE tracks)")
        finalTracks
    }

    /** Menambah hitungan berapa kali lagu telah diputar selesai */
    suspend fun incrementPlayCount(trackId: Long, timestamp: Long) = withContext(Dispatchers.IO) {
        trackDao.incrementPlayCount(trackId, timestamp)
    }

    /** Mengambil lagu yang paling sering diputar */
    suspend fun getMostPlayedTracks(): List<Track> = withContext(Dispatchers.IO) {
        trackDao.getMostPlayedTracks().map { it.toDomain() }
    }

    /** Menghapus seluruh cache metadata lagu. */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        trackDao.clearAll()
    }

    /**
     * Memuat lagu dengan strategi cache-first:
     * Jika cache ada dan forceSync false, kembalikan cache langsung.
     */
    suspend fun loadTracks(forceSync: Boolean = false): List<Track> = withContext(Dispatchers.IO) {
        if (!forceSync) {
            val cached = getCachedTracks()
            if (cached.isNotEmpty()) {
                return@withContext cached
            }
        }
        syncFromMediaStore()
    }
}
