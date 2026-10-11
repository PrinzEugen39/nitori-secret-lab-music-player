package com.prinzeugen.nitorimusic.ui

fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val df = java.text.DecimalFormat("#.##")
    return when {
        bytes >= gb -> "${df.format(bytes / gb)} GB"
        bytes >= mb -> "${df.format(bytes / mb)} MB"
        bytes >= kb -> "${df.format(bytes / kb)} KB"
        else -> "$bytes B"
    }
}

