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
import com.voicedeutsch.master.domain.usecase.knowledge.FlushKnowledgeSyncUseCase
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
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient         = true
            encodeDefaults    = true
        }
    }

    // ─── Ktor HttpClient ─────────────────────────────────────────────────────
    single {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
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
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
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

    // ─── Remote Services ─────────────────────────────────────────────────────
    single { BackupManager(androidContext(), get<AppDatabase>(), get<FirebaseFirestore>(), get<FirebaseStorage>(), get<FirebaseAuth>()) }
    single { CloudSyncService(get<FirebaseFirestore>(), get<FirebaseAuth>()) }

    // ─── Repositories ────────────────────────────────────────────────────────
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get(), get(), get())
    }

    // ✅ ИЗМЕНЕНО: добавлен get<CloudSyncService>() восьмым аргументом.
    // KnowledgeRepositoryImpl теперь принимает CloudSyncService для батч-синхронизации.
    // Было: KnowledgeRepositoryImpl(get(), get(), get(), get(), get(), get(), get())
    // Стало: + get<CloudSyncService>()
    single<KnowledgeRepository> {
        KnowledgeRepositoryImpl(
            wordDao         = get(),
            knowledgeDao    = get(),
            grammarRuleDao  = get(),
            phraseDao       = get(),
            progressDao     = get(),
            mistakeDao      = get(),
            json            = get(),
            cloudSync       = get(),   // ✅ CloudSyncService для батчинга
        )
    }

    single<BookRepository> {
        BookRepositoryImpl(androidContext(), get(), get(), get(), get(), get(), get())
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

    // ─── Use Cases: Knowledge Sync ───────────────────────────────────────────
    // ✅ ДОБАВЛЕНО: FlushKnowledgeSyncUseCase — вызывается в VoiceCoreEngineImpl.endSession().
    // Определяем здесь, а не в useCaseModule, потому что зависит от KnowledgeRepository
    // который уже объявлен в dataModule. Если у вас есть отдельный useCaseModule —
    // перенесите эту строку туда.
    single { FlushKnowledgeSyncUseCase(get<KnowledgeRepository>()) }
}
