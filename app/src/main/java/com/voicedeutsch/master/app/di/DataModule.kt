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

val dataModule = module {

    // ─── Database ───────────────────────────────────────────────
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    // ─── DAOs ───────────────────────────────────────────────────
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().wordDao() }
    single { get<AppDatabase>().knowledgeDao() }
    single { get<AppDatabase>().grammarRuleDao() }
    single { get<AppDatabase>().phraseDao() }
    single { get<AppDatabase>().sessionDao() }
    single { get<AppDatabase>().progressDao() }
    single { get<AppDatabase>().bookProgressDao() }
    single { get<AppDatabase>().mistakeDao() }

    // ─── DataStore & File Reader ────────────────────────────────
    single { UserPreferencesDataStore(androidContext()) }
    single { BookFileReader(androidContext(), get()) }

    // ─── Repositories (interface → implementation) ──────────────
    // UserRepositoryImpl(userDao, knowledgeDao, preferencesDataStore, json) — 4 params
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get(), get())
    }

    // KnowledgeRepositoryImpl(wordDao, knowledgeDao, grammarRuleDao, phraseDao, progressDao, mistakeDao, json) — 7 params
    single<KnowledgeRepository> {
        KnowledgeRepositoryImpl(get(), get(), get(), get(), get(), get(), get())
    }

    // BookRepositoryImpl(bookFileReader, bookProgressDao, wordDao, grammarRuleDao, preferencesDataStore, json) — 6 params
    single<BookRepository> {
        BookRepositoryImpl(get(), get(), get(), get(), get(), get())
    }

    // SessionRepositoryImpl(sessionDao, progressDao, json) — 3 params
    single<SessionRepository> {
        SessionRepositoryImpl(get(), get(), get())
    }

    // ProgressRepositoryImpl(knowledgeDao, wordDao, grammarRuleDao, sessionDao, bookProgressDao, progressDao, userDao, json) — 8 params
    single<ProgressRepository> {
        ProgressRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get())
    }
}