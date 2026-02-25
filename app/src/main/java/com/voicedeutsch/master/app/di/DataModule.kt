package com.voicedeutsch.master.app.di

import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.voicedeutsch.master.data.local.database.AppDatabase
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.data.local.file.AudioCacheManager
import com.voicedeutsch.master.data.local.file.BookFileReader
import com.voicedeutsch.master.data.local.file.ExportImportManager
import com.voicedeutsch.master.data.remote.sync.BackupManager
import com.voicedeutsch.master.data.remote.sync.CloudSyncService
import com.voicedeutsch.master.data.repository.AchievementRepositoryImpl
import com.voicedeutsch.master.data.repository.BookRepositoryImpl
import com.voicedeutsch.master.data.repository.KnowledgeRepositoryImpl
import com.voicedeutsch.master.data.repository.ProgressRepositoryImpl
import com.voicedeutsch.master.data.repository.SessionRepositoryImpl
import com.voicedeutsch.master.data.repository.SpeechRepositoryImpl
import com.voicedeutsch.master.data.repository.UserRepositoryImpl
import com.voicedeutsch.master.domain.repository.AchievementRepository
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.SpeechRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val dataModule = module {

    // ─── JSON ────────────────────────────────────────────────────────────────
    // Единственный Json-инстанс на всё приложение.
    // ignoreUnknownKeys: безопасно для эволюции API (новые поля не ломают парсинг).
    // isLenient: принимает JSON с одинарными кавычками (некоторые Firebase-ответы).
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient         = true
            encodeDefaults    = true
        }
    }

    // ─── Ktor HttpClient ─────────────────────────────────────────────────────
    // ℹ️ HttpClient остаётся для вспомогательных HTTP-запросов (не Gemini).
    // Основной AI-транспорт (Gemini Live API) — firebase-ai SDK (GeminiClient.kt).
    // WebSockets плагин УДАЛЁН — WebSocket с Gemini теперь управляет SDK.
    single {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }

            // ContentNegotiation: нужен для response.body<T>() без рефлексии.
            install(ContentNegotiation) {
                json(get<Json>())
            }

            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("KtorNetwork", message)
                    }
                }
            }
        }
    }

    // ─── Firebase Auth ────────────────────────────────────────────────────────
    // ✅ BoM 34.x: Firebase.auth — без -ktx суффикса.
    single<FirebaseAuth> {
        Firebase.auth.apply {
            if (currentUser == null) {
                signInAnonymously()
                    .addOnSuccessListener { result ->
                        android.util.Log.d("FirebaseAuth", "✅ Anonymous sign-in: uid=${result.user?.uid}")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.w("FirebaseAuth", "⚠️ Anonymous sign-in failed: ${e.message}")
                    }
            }
        }
    }

    // ─── Firebase Firestore ───────────────────────────────────────────────────
    // ✅ BoM 34.x: PersistentCacheSettings — офлайн-кеш на диске.
    single<FirebaseFirestore> {
        Firebase.firestore.apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(50_000_000L)
                        .build()
                )
                .build()
        }
    }

    // ─── Firebase Storage ─────────────────────────────────────────────────────
    single<FirebaseStorage> {
        Firebase.storage.apply {
            maxDownloadRetryTimeMillis = 60_000L
            maxUploadRetryTimeMillis   = 120_000L
        }
    }

    // ─── Database ────────────────────────────────────────────────────────────
    // ✅ ПРОДАКШЕН 2026: fallbackToDestructiveMigrationFrom УДАЛЁН.
    //
    // Почему это критично:
    //   fallbackToDestructiveMigration() стирает ВСЮ базу если Room не находит
    //   нужную миграцию. Для пользователя это потеря прогресса → удаление приложения.
    //
    // Правило: каждый новый version в @Database требует явного MIGRATION_X_Y.
    // Даже если схема не изменилась — пишем пустую миграцию:
    //
    //   val MIGRATION_2_3 = object : Migration(2, 3) {
    //       override fun migrate(db: SupportSQLiteDatabase) {
    //           // no-op: version bump без изменений схемы
    //       }
    //   }
    //
    // И добавляем её здесь через .addMigrations(AppDatabase.MIGRATION_2_3).
    //
    // Текущая версия БД: 2. Все переходы покрыты:
    //   1 → 2: MIGRATION_1_2 (таблицы achievements + user_achievements)
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                // При добавлении новых сущностей — добавляй MIGRATION_2_3 и т.д.
                // НИКОГДА не используй fallbackToDestructiveMigration в продакшене.
            )
            .build()
    }

    // ─── DAOs ────────────────────────────────────────────────────────────────
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().wordDao() }
    single { get<AppDatabase>().knowledgeDao() }
    single { get<AppDatabase>().grammarRuleDao() }
    single { get<AppDatabase>().phraseDao() }
    single { get<AppDatabase>().sessionDao() }
    single { get<AppDatabase>().progressDao() }
    single { get<AppDatabase>().bookProgressDao() }
    single { get<AppDatabase>().mistakeDao() }
    single { get<AppDatabase>().achievementDao() }

    // ─── DataStore & Assets ──────────────────────────────────────────────────
    single { UserPreferencesDataStore(androidContext()) }
    single { BookFileReader(androidContext(), get()) }

    // ─── Repositories ────────────────────────────────────────────────────────
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get(), get(), get())
    }

    single<KnowledgeRepository> {
        KnowledgeRepositoryImpl(get(), get(), get(), get(), get(), get(), get())
    }

    single<BookRepository> {
        BookRepositoryImpl(get(), get(), get(), get(), get(), get())
    }

    single<SessionRepository> {
        SessionRepositoryImpl(get(), get(), get())
    }

    single<ProgressRepository> {
        ProgressRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get())
    }

    single<AchievementRepository> {
        AchievementRepositoryImpl(get(), get())
    }

    single<SpeechRepository> {
        SpeechRepositoryImpl(get(), get())
    }

    // ─── File Managers ───────────────────────────────────────────────────────
    single { AudioCacheManager(androidContext()) }
    single { ExportImportManager(androidContext(), get()) }

    // ─── Remote Services (Firebase-based) ────────────────────────────────────
    single { BackupManager(androidContext(), get<FirebaseFirestore>(), get<FirebaseStorage>(), get<FirebaseAuth>()) }
    single { CloudSyncService(get<FirebaseFirestore>(), get<FirebaseAuth>()) }

    // ✅ EphemeralTokenService УДАЛЁН:
    // Firebase App Check + firebase-ai SDK управляют авторизацией прозрачно.
}
