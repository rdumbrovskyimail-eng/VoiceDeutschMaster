package com.voicedeutsch.master.app.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voicedeutsch.master.data.local.database.AppDatabase
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.data.local.file.BookFileReader
import com.voicedeutsch.master.data.repository.BookRepositoryImpl
import com.voicedeutsch.master.data.repository.KnowledgeRepositoryImpl
import com.voicedeutsch.master.data.repository.ProgressRepositoryImpl
import com.voicedeutsch.master.data.repository.SecurityRepositoryImpl
import com.voicedeutsch.master.data.repository.SessionRepositoryImpl
import com.voicedeutsch.master.data.repository.UserRepositoryImpl
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.SecurityRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.voicedeutsch.master.data.local.file.AudioCacheManager
import com.voicedeutsch.master.data.local.file.ExportImportManager
import com.voicedeutsch.master.data.remote.gemini.GeminiService
import com.voicedeutsch.master.data.remote.sync.BackupManager
import com.voicedeutsch.master.data.remote.sync.CloudSyncService
import com.voicedeutsch.master.data.repository.AchievementRepositoryImpl
import com.voicedeutsch.master.data.repository.SpeechRepositoryImpl
import com.voicedeutsch.master.domain.repository.AchievementRepository
import com.voicedeutsch.master.domain.repository.SpeechRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Data layer module: Room DB, DAOs, DataStore, BookFileReader, Repository implementations.
 *
 * Repository constructor parameter order is documented inline to keep it
 * verifiable against each Impl class signature.
 */
val dataModule = module {

    // ─── Ktor HttpClient ─────────────────────────────────────────────────────
    // Shared single — используется в GeminiClient для WebSocket-соединения.
    // WebSockets plugin обязателен для Gemini Live API.
    single {
        HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                level = LogLevel.HEADERS // В release сменить на LogLevel.NONE
            }
        }
    }

    // ─── Security ────────────────────────────────────────────────────────────
    // SecurityRepository хранит API-ключ в EncryptedSharedPreferences.
    // Передаётся в VoiceCoreModule для создания GeminiConfig.
    single<SecurityRepository> {
        SecurityRepositoryImpl(androidContext())
    }

    // ─── Database ────────────────────────────────────────────────────────────

    /**
     * H5 FIX: Replaced [fallbackToDestructiveMigration] with explicit migrations.
     *
     * Current approach:
     *   - Explicit [Migration] objects are registered for every schema bump.
     *   - [fallbackToDestructiveMigrationFrom] is used ONLY for versions
     *     before the first public release (pre-release dev builds) where no
     *     real user data exists. Set [LAST_DESTRUCTIVE_VERSION] to the last
     *     pre-release schema version.
     *   - For all subsequent versions, a proper migration MUST be written.
     */
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
    // BookFileReader(context, json)
    single { BookFileReader(androidContext(), get()) }

    // ─── Repositories ────────────────────────────────────────────────────────

    // UserRepositoryImpl(userDao, knowledgeDao, wordDao, preferencesDataStore, json)
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get(), get(), get())
    }

    // KnowledgeRepositoryImpl(wordDao, knowledgeDao, grammarRuleDao, phraseDao,
    //     progressDao, mistakeDao, bookProgressDao, userDao, sessionDao, json)
    single<KnowledgeRepository> {
        KnowledgeRepositoryImpl(get(), get(), get(), get(), get(), get(), get())
    }

    // BookRepositoryImpl(bookFileReader, bookProgressDao, wordDao,
    //                    grammarRuleDao, preferencesDataStore, json)
    single<BookRepository> {
        BookRepositoryImpl(get(), get(), get(), get(), get(), get())
    }

    // SessionRepositoryImpl(sessionDao, progressDao, json)
    single<SessionRepository> {
        SessionRepositoryImpl(get(), get(), get())
    }

    // ProgressRepositoryImpl(knowledgeDao, wordDao, grammarRuleDao, sessionDao,
    //                         bookProgressDao, progressDao, userDao, json)
    single<ProgressRepository> {
        ProgressRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get())
    }

    // AchievementRepositoryImpl(achievementDao, json)
    single<AchievementRepository> {
        AchievementRepositoryImpl(get(), get())
    }

    // SpeechRepositoryImpl(knowledgeDao, json)
    single<SpeechRepository> {
        SpeechRepositoryImpl(get(), get())
    }

    // ─── File Managers ───────────────────────────────────────────────────────
    single { AudioCacheManager(androidContext()) }
    // ExportImportManager(context, json)
    single { ExportImportManager(androidContext(), get()) }
    single { BackupManager(androidContext()) }

    // ─── Remote Services ─────────────────────────────────────────────────────
    single { CloudSyncService() }
    // GeminiService(httpClient, json)
    single { GeminiService(get(), get()) }
}

// ── Migration constants ───────────────────────────────────────────────────────

/**
 * The last schema version for which destructive migration is acceptable.
 * Set this to the schema version used during internal/alpha testing BEFORE
 * the first public release. After launch, this value MUST NOT be increased.
 */
private const val LAST_DESTRUCTIVE_VERSION = 0

// ── Migration examples ────────────────────────────────────────────────────────
// Uncomment and adapt when the schema evolves.

// private val MIGRATION_1_2 = object : Migration(1, 2) {
//     override fun migrate(db: SupportSQLiteDatabase) {
//         db.execSQL("ALTER TABLE users ADD COLUMN avatar_url TEXT DEFAULT NULL")
//     }
// }

// private val MIGRATION_2_3 = object : Migration(2, 3) {
//     override fun migrate(db: SupportSQLiteDatabase) {
//         db.execSQL("""
//             CREATE TABLE IF NOT EXISTS pronunciation_records (
//                 id TEXT PRIMARY KEY NOT NULL,
//                 user_id TEXT NOT NULL,
//                 word TEXT NOT NULL,
//                 score REAL NOT NULL DEFAULT 0.0,
//                 recorded_at INTEGER NOT NULL,
//                 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
//             )
//         """)
//         db.execSQL("CREATE INDEX IF NOT EXISTS idx_pronunciation_user ON pronunciation_records(user_id)")
//     }
// }