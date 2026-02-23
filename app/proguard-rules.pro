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

# ── Ktor (вспомогательные REST-запросы) ───────────────────────────────────────
# Основной AI-транспорт — firebase-ai SDK, не Ktor.
# Если Ktor полностью удалён из проекта — удалите этот блок тоже.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
# Примечание: kotlinx.coroutines правила не нужны здесь —
# библиотека публикует собственные consumer-rules.pro, R8 подхватывает автоматически.

# ── Firebase AI Logic (Gemini Live API) ───────────────────────────────────────
# Заменяет устаревший com.google.ai.client.generativeai (SDK deprecated).
# Покрывается нижним блоком com.google.firebase.**, оставлен явно для читаемости.
-keep class com.google.firebase.ai.** { *; }
-dontwarn com.google.firebase.ai.**

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Koin ──────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**

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