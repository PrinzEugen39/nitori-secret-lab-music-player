# --- Android & Kotlin Coroutines ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn kotlinx.coroutines.**

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# --- Media3 / ExoPlayer ---
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Coil Image Loader ---
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# --- AndroidX Palette ---
-keep class androidx.palette.** { *; }

# --- DataStore Preferences ---
-keep class androidx.datastore.** { *; }

# --- Domain & Data Models ---
-keep class com.prinzeugen.nitorimusic.domain.model.** { *; }
-keep class com.prinzeugen.nitorimusic.data.cue.** { *; }
-keep class com.prinzeugen.nitorimusic.data.db.entity.** { *; }
