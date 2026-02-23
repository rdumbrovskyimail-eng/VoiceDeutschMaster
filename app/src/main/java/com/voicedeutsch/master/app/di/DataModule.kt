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
    // Если Ktor используется только для Gemini — можно удалить целиком.
    single {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                    // pingInterval убран: WebSocket через Ktor больше не используется.
                    // Если вернёте WebSocket — раскомментируйте:
                    // pingInterval(20, TimeUnit.SECONDS)
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
    // ✅ BoM 34.x: Firebase.auth (из com.google.firebase.auth) — без -ktx суффикса.
    // Используется для:
    //   • Идентификации пользователя в Firestore Security Rules (request.auth.uid)
    //   • Анонимного входа при первом запуске
    //   • Google Sign-In (при необходимости)
    //
    // ⚠️ Firebase Auth НЕ нужен для Gemini Live API — App Check обрабатывает
    // авторизацию AI-запросов прозрачно. Auth нужен только для Firestore/Storage.
    single<FirebaseAuth> {
        Firebase.auth.apply {
            // Анонимный вход: запускается при первом использовании Firebase Auth.
            // Не блокирует — используем addOnSuccessListener.
            // Если пользователь уже вошёл (currentUser != null) — вызов игнорируется.
            if (currentUser == null) {
                signInAnonymously()
                    .addOnSuccessListener { result ->
                        android.util.Log.d("FirebaseAuth", "✅ Anonymous sign-in: uid=${result.user?.uid}")
                    }
                    .addOnFailureListener { e ->
                        // Не фатально: Firestore запросы упадут с permission-denied,
                        // но Room-локальная база продолжит работать.
                        android.util.Log.w("FirebaseAuth", "⚠️ Anonymous sign-in failed: ${e.message}")
                    }
            }
        }
    }

    // ─── Firebase Firestore ───────────────────────────────────────────────────
    // ✅ BoM 34.x: Firebase.firestore (из com.google.firebase.firestore).
    // Заменяет кастомный BackupManager + CloudSyncService на нативный real-time sync.
    //
    // Настройки кеша:
    //   PersistentCacheSettings — офлайн-кеш на диске (до 100 MB по умолчанию).
    //   Позволяет читать данные без сети и синхронизировать при восстановлении связи.
    //   Это лучше чем MemoryCacheSettings (данные живут только в рамках сессии).
    single<FirebaseFirestore> {
        Firebase.firestore.apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        // 50 MB — разумный лимит для прогресса обучения.
                        // Увеличьте до 104_857_600L (100 MB) если добавите аудио-метаданные.
                        .setSizeBytes(50_000_000L)
                        .build()
                )
                .build()
        }
    }

    // ─── Firebase Storage ─────────────────────────────────────────────────────
    // ✅ BoM 34.x: Firebase.storage (из com.google.firebase.storage).
    // Используется для:
    //   • Хранения аудио-записей пользователя (AudioCacheManager → Cloud backup)
    //   • Экспорта/импорта данных (ExportImportManager)
    //   • BackupManager: загрузка/скачивание дампов базы
    //
    // maxDownloadRetryTime / maxUploadRetryTime: SDK автоматически ретраит
    // при обрыве сети в пределах указанного времени.
    single<FirebaseStorage> {
        Firebase.storage.apply {
            maxDownloadRetryTimeMillis = 60_000L  // 60 сек на retry скачивания
            maxUploadRetryTimeMillis   = 120_000L // 120 сек на retry загрузки
        }
    }

    // ─── Database ────────────────────────────────────────────────────────────
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
            )
            .fallbackToDestructiveMigrationFrom(
                *(1..LAST_DESTRUCTIVE_VERSION).toList().toIntArray()
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
    // BackupManager: принимает FirebaseFirestore + FirebaseStorage + FirebaseAuth
    // вместо прежнего androidContext() — для полноценного облачного бекапа.
    // Обновите конструктор BackupManager.kt при следующей итерации.
    single { BackupManager(androidContext(), get<FirebaseFirestore>(), get<FirebaseStorage>(), get<FirebaseAuth>()) }

    // CloudSyncService: принимает FirebaseFirestore + FirebaseAuth.
    // Real-time sync прогресса пользователя через Firestore snapshots().
    // Обновите конструктор CloudSyncService.kt при следующей итерации.
    single { CloudSyncService(get<FirebaseFirestore>(), get<FirebaseAuth>()) }

    // ✅ EphemeralTokenService УДАЛЁН:
    // Ручное получение ephemeral-токена для Gemini больше не нужно.
    // Firebase App Check + firebase-ai SDK управляют авторизацией прозрачно.
    // См. EphemeralTokenService.DELETED.kt для полного объяснения.
}

// Версии базы данных до которых применяется destructive migration.
// Увеличьте значение при добавлении новых несовместимых миграций.
private const val LAST_DESTRUCTIVE_VERSION = 0
