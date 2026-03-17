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
import com.voicedeutsch.master.util.LogCollectorProcess
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class VoiceDeutschApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()

        // ── 1. Запускаем LogCollectorProcess (shell-скрипт, отдельный UNIX-процесс) ──
        //    Первым — чтобы если дальше что-то крашнет, логи уже пишутся в файл.
        //    НЕ Android Service, НЕ отдельный android:process — просто fork через sh.
        try {
            LogCollectorProcess.start(this)
        } catch (e: Exception) {
            Log.e(TAG, "❌ LogCollectorProcess start failed", e)
        }

        // ── 2. AppLogger (in-process буфер, дополняет LogCollector) ──────
        initAppLogger()

        // ── 3. CrashLogger (перехватчик uncaught exceptions) ─────────────
        try {
            CrashLogger.init(this).apply {
                cleanOldLogs(keepCount = 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ CrashLogger init failed", e)
        }

        // ── 4. Koin DI ──────────────────────────────────────────────────
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        // ── 5. Firebase ─────────────────────────────────────────────────
        initFirebase()

        // ✅ FIX ANR: Прогреваем тяжёлые Firebase singletons в фоне.
        // При первом вызове FirebaseFirestore и FirebaseAuth делают сетевой I/O
        // (AppCheck token exchange, gRPC channel init). Если это происходит
        // на main thread при создании SessionViewModel через Koin — ANR 5+ сек.
        // Прогрев здесь гарантирует, что к моменту нажатия "Начать занятие"
        // singletons уже инициализированы.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
            runCatching { com.google.firebase.auth.FirebaseAuth.getInstance() }
        }

        // ── 6. WorkManager ──────────────────────────────────────────────
        WorkManagerInitializer.initialize(this)
    }

    private fun initAppLogger() {
        try {
            AppLogger.init(this).start()
            Log.d(TAG, "✅ AppLogger started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ AppLogger failed", e)
        }
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "✅ FirebaseApp initialized manually")
            } else {
                Log.d(TAG, "✅ FirebaseApp auto-initialized by provider")
            }

            initAppCheck()
            initCrashlytics()
            initAnalytics()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase init failed: ${e.message}", e)
        }
    }

    private fun initAppCheck() {
        try {
            if (BuildConfig.USE_DEBUG_APP_CHECK) {
                val token = BuildConfig.APP_CHECK_DEBUG_TOKEN
                if (token.isNotEmpty()) {
                    FirebaseAppCheck.getInstance()
                        .installAppCheckProviderFactory(
                            StaticDebugAppCheckProviderFactory(token)
                        )
                    Log.d(TAG, "✅ App Check initialized [STATIC_DEBUG_TOKEN]")
                } else {
                    FirebaseAppCheck.getInstance()
                        .installAppCheckProviderFactory(
                            com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                        )
                    Log.d(TAG, "✅ App Check initialized [DEBUG_PROVIDER]")
                }
            } else {
                FirebaseAppCheck.getInstance()
                    .installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                Log.d(TAG, "✅ App Check initialized [PLAY_INTEGRITY]")
            }
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
