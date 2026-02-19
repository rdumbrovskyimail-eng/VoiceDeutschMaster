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
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            // Production: replace with explicit migrations before release.
            .fallbackToDestructiveMigration()
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
    // UserRepositoryImpl(userDao, knowledgeDao, preferencesDataStore, json)
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get(), get())
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