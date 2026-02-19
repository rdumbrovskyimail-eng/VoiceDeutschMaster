# ── VoiceDeutschMaster ProGuard / R8 Rules ────────────────────────────────────

# ── Kotlinx Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.voicedeutsch.master.**$$serializer { *; }
-keepclassmembers class com.voicedeutsch.master.** {
    *** Companion;
}
-keepclasseswithmembers class com.voicedeutsch.master.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── Ktor ──────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Firebase AI Logic (Gemini Live API) ───────────────────────────────────────
# Заменяет устаревший блок com.google.ai.client.generativeai (SDK deprecated)
-keep class com.google.firebase.ai.** { *; }
-dontwarn com.google.firebase.ai.**

# ── Koin ──────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Domain models (used by Gemini function calls via JSON) ────────────────────
-keep class com.voicedeutsch.master.domain.model.** { *; }
-keep class com.voicedeutsch.master.voicecore.engine.GeminiResponse { *; }
-keep class com.voicedeutsch.master.voicecore.engine.GeminiFunctionCall { *; }

# ── Enums ─────────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Security ──────────────────────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ── General ───────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove Log.d and Log.v calls in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}