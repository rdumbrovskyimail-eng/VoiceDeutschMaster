package com.voicedeutsch.master.app.di

import androidx.room.Room
import com.voicedeutsch.master.data.local.database.AppDatabase
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.data.local.file.BookFileReader
import com.voicedeutsch.master.data.repository.BookRepositoryImpl
import com.voicedeutsch.master.data.repository.KnowledgeRepositoryImpl
import com.voicedeutsch.master.data.repository.ProgressRepositoryImpl
import com.voicedeutsch.master.data.repository.SessionRepositoryImpl
import com.voicedeutsch.master.data.repository.UserRepositoryImpl
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import com.voicedeutsch.master.data.remote.gemini.EphemeralTokenService
import com.voicedeutsch.master.data.local.file.AudioCacheManager
import com.voicedeutsch.master.data.local.file.ExportImportManager
import com.voicedeutsch.master.data.remote.sync.BackupManager
import com.voicedeutsch.master.data.remote.sync.CloudSyncService
import com.voicedeutsch.master.data.repository.AchievementRepositoryImpl
import com.voicedeutsch.master.data.repository.SpeechRepositoryImpl
import com.voicedeutsch.master.domain.repository.AchievementRepository
import com.voicedeutsch.master.domain.repository.SpeechRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {

    // ─── Ktor HttpClient ─────────────────────────────────────────────────────
    single {
        HttpClient(OkHttp) {
            engine {
                config {
                    pingInterval(20, TimeUnit.SECONDS)
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000L
                maxFrameSize = Long.MAX_VALUE
            }
            install(io.ktor.client.plugins.logging.Logging) {
                level = io.ktor.client.plugins.logging.LogLevel.INFO
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        android.util.Log.d("KtorNetwork", message)
                    }
                }
            }
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
    single { BackupManager(androidContext()) }

    // ─── Remote Services ─────────────────────────────────────────────────────
    single { CloudSyncService() }
    single { EphemeralTokenService(get(), get()) }
}

private const val LAST_DESTRUCTIVE_VERSION = 0