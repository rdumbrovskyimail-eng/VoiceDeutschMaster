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

    // \u2500\u2500\u2500 JSON \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500
    // \u0415\u0434\u0438\u043d\u0441\u0442\u0432\u0435\u043d\u043d\u044b\u0439 Json-\u0438\u043d\u0441\u0442\u0430\u043d\u0441 \u043d\u0430 \u0432\u0441\u0451 \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u0435.
    // ignoreUnknownKeys: \u0431\u0435\u0437\u043e\u043f\u0430\u0441\u043d\u043e \u0434\u043b\u044f \u044d\u0432\u043e\u043b\u044e\u0446\u0438\u0438 API (\u043d\u043e\u0432\u044b\u0435 \u043f\u043e\u043b\u044f \u043d\u0435 \u043b\u043e\u043c\u0430\u044e\u0442 \u043f\u0430\u0440\u0441\u0438\u043d\u0433).
    // isLenient: \u043f\u0440\u0438\u043d\u0438\u043c\u0430\u0435\u0442 JSON \u0441 \u043e\u0434\u0438\u043d\u0430\u0440\u043d\u044b\u043c\u0438 \u043a\u0430\u0432\u044b\u0447\u043a\u0430\u043c\u0438 (\u043d\u0435\u043a\u043e\u0442\u043e\u0440\u044b\u0435 Firebase-\u043e\u0442\u0432\u0435\u0442\u044b).
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient         = true
            encodeDefaults    = true
        }
    }

    // \u2500\u2500\u2500 Ktor HttpClient \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500
    // \u2139\ufe0f HttpClient \u043e\u0441\u0442\u0430\u0451\u0442\u0441\u044f \u0434\u043b\u044f \u0432\u0441\u043f\u043e\u043c\u043e\u0433\u0430\u0442\u0435\u043b\u044c\u043d\u044b\u0445 HTTP-\u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 (\u043d\u0435 Gemini).
    // \u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 AI-\u0442\u0440\u0430\u043d\u0441\u043f\u043e\u0440\u0442 (Gemini Live API) \u2014 firebase-ai SDK (GeminiClient.kt).
    // WebSockets \u043f\u043b\u0430\u0433\u0438\u043d \u0423\u0414\u0410\u041b\u0401\u041d \u2014 WebSocket \u0441 Gemini \u0442\u0435\u043f\u0435\u0440\u044c \u0443\u043f\u0440\u0430\u0432\u043b\u044f\u0435\u0442 SDK.
    single {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }

            // ContentNegotiation: \u043d\u0443\u0436\u0435\u043d \u0434\u043b\u044f response.body<T>() \u0431\u0435\u0437 \u0440