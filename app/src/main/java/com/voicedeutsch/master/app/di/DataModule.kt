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
import com.voicedeutsch.master.data.repository.SessionRepositoryImpl
import com.voicedeutsch.master.data.repository.UserRepositoryImpl
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Data layer module: Room DB, DAOs, DataStore, BookFileReader, Repository implementations.
 *
 * Repository constructor parameter order is documented inline to keep it
 * verifiable against each Impl class signature.
 */
val dataModule = module {

    // ─── Database ────────────────────────────────────────────────────────────

    /**
     * H5 FIX: Replaced [fallbackToDestructiveMigration] with explicit migrations.
     *
     * Destructive migration wipes the entire database on any schema change —
     * for a language-learning app this means losing all user progress, SRS
     * intervals, word knowledge, session history, and book progress. This is
     * catastrophic for the core value proposition.
     *
     * Current approach:
     *   - Explicit [Migration] objects are registered for every schema bump.
     *   - [fallbackToDestructiveMigrationFrom] is used ONLY for versions
     *     before the first public release (pre-release dev builds) where no
     *     real user data exists. Set [LAST_DESTRUCTIVE_VERSION] to the last
     *     pre-release schema version.
     *   - For all subsequent versions, a proper migration MUST be written.
     *
     * To add a new migration:
     *   1. Bump the version in [AppDatabase] @Database annotation
     *   2. Add a MIGRATION_X_Y val below with the required ALTER/CREATE statements
     *   3. Register it in .addMigrations(...)
     *   4. Write a test in DatabaseMigrationTest to verify the migration
     */
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .addMigrations(
                // Register all migrations here as they are created.
                // Example:
                // MIGRATION_1_2,
                // MIGRATION_2_3,
            )
            // Only allow destructive migration from pre-release versions.
            // Once the app ships to real users, increase this threshold ONLY
            // if you are absolutely sure no production user has that version.
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

    // ─── DataStore & Assets ──────────────────────────────────────────────────
    single { UserPreferencesDataStore(androidContext()) }
    // BookFileReader(context, json)
    single { BookFileReader(androidContext(), get()) }

    // ─── Repositories ────────────────────────────────────────────────────────

    // UserRepositoryImpl(userDao, knowledgeDao, wordDao, preferencesDataStore, json)
    // NOTE: wordDao added for H4 fix (totalWords now reads from dictionary, not knowledge)
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get(), get(), get())
    }

    // KnowledgeRepositoryImpl(wordDao, knowledgeDao, grammarRuleDao, phraseDao,
    //                          progressDao, mistakeDao, json)
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
}

// ── Migration constants ──────────────────────────────────────────────────────

/**
 * The last schema version for which destructive migration is acceptable.
 * Set this to the schema version used during internal/alpha testing BEFORE
 * the first public release. After launch, this value MUST NOT be increased.
 */
private const val LAST_DESTRUCTIVE_VERSION = 1

// ── Migration examples ───────────────────────────────────────────────────────
// Uncomment and adapt when the schema evolves.

// private val MIGRATION_1_2 = object : Migration(1, 2) {
//     override fun migrate(db: SupportSQLiteDatabase) {
//         // Example: add a column to the users table
//         db.execSQL("ALTER TABLE users ADD COLUMN avatar_url TEXT DEFAULT NULL")
//     }
// }

// private val MIGRATION_2_3 = object : Migration(2, 3) {
//     override fun migrate(db: SupportSQLiteDatabase) {
//         // Example: create a new table
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
