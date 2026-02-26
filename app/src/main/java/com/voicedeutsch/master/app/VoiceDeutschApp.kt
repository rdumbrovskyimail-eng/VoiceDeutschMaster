package com.voicedeutsch.master.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.analytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.crashlytics
import com.voicedeutsch.master.BuildConfig
import com.voicedeutsch.master.app.di.appModules
import com.voicedeutsch.master.app.worker.WorkManagerInitializer
import com.voicedeutsch.master.util.AppLogger
import com.voicedeutsch.master.util.CrashLogger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry point.
 *
 * Порядок инициализации (критически важен):
 *  1. [CrashLogger]        — в attachBaseContext(), ДО super(). Ловит краши с самого старта.
 *  2. [AppLogger]          — первым в onCreate(), до super(). Кольцевой буфер logcat.
 *  3. super.onCreate()
 *  4. Koin DI              — до Firebase: модули могут предоставлять Firebase-зависимости.
 *  5. Firebase             — СТРОГИЙ порядок внутри: App → AppCheck → Crashlytics → Analytics.
 *                            App Check должен быть установлен ДО первого обращения к любому
 *                            Firebase-сервису (Firestore, Storage, firebase-ai и т.д.).
 *  6. WorkManager
 */
class VoiceDeutschApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            CrashLogger.init(base).apply {
                cleanOldLogs(keepCount = 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ CrashLogger init failed", e)
        }
    }

    override fun onCreate() {
        initAppLogger()
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        initFirebase()
        WorkManagerInitializer.initialize(this)
        CrashLogger.getInstance()?.copyLatestCrashToDownloads(this)
    }

    private fun initAppLogger() {
        try {
            AppLogger.init(this).start()
            Log.d(TAG, "✅ AppLogger started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ AppLogger failed", e)
        }
    }

    /**
     * Инициализация Firebase в строгом порядке:
     *
     * 1. [FirebaseApp.initializeApp] — обязателен первым.
     * 2. [FirebaseAppCheck] — СРАЗУ после initializeApp, ДО любых обращений к Firestore,
     *    Storage, firebase-ai и т.д.
     * 3. [FirebaseCrashlytics] — настройка кастомных ключей до первого крэша.
     * 4. [FirebaseAnalytics] — в debug отключаем сбор.
     */
    private fun initFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ FirebaseApp initialized")

            initAppCheck()
            initCrashlytics()
            initAnalytics()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase init failed: ${e.message}", e)
        }
    }

    /**
     * ✅ FIX: Используем FirebaseAppCheck.getInstance() вместо KTX-свойства Firebase.appCheck,
     * которое могло не резолвиться в некоторых конфигурациях.
     *
     * Debug-сборка:  DebugAppCheckProviderFactory → UUID-токен в Logcat.
     *   → Добавьте токен в Firebase Console → App Check → Debug tokens.
     * Release-сборка: PlayIntegrityAppCheckProviderFactory → Google Play Integrity API.
     *   → SHA-256 fingerprint release-ключа обязателен в Firebase Console → Project settings.
     */
    private fun initAppCheck() {
        try {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                if (BuildConfig.DEBUG) {
                    com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                } else {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }
            )
            Log.d(TAG, "✅ App Check initialized [${if (BuildConfig.DEBUG) "DEBUG" else "PLAY_INTEGRITY"}]")
        } catch (e: Exception) {
            Log.e(TAG, "❌ App Check init failed: ${e.message}", e)
        }
    }

    private fun initCrashlytics() {
        try {
            Firebase.crashlytics.apply {
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
                setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
            }
            Log.d(TAG, "✅ Crashlytics initialized [collection=${!BuildConfig.DEBUG}]")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Crashlytics init failed: ${e.message}", e)
        }
    }

    private fun initAnalytics() {
        try {
            Firebase.analytics.apply {
                setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
            }
            Log.d(TAG, "✅ Analytics initialized [collection=${!BuildConfig.DEBUG}]")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Analytics init failed: ${e.message}", e)
        }
    }

    private companion object {
        const val TAG = "VoiceDeutschApp"
    }
}