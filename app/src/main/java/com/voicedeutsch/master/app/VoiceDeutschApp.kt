package com.voicedeutsch.master.app

import android.app.ActivityManager
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
import com.voicedeutsch.master.util.LogCollectorService
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

        // ── 1. Определяем, это главный процесс или :logcollector ─────────
        // Koin, Firebase, AppLogger и т.д. инициализируем ТОЛЬКО в главном процессе.
        // Сервис LogCollectorService живёт в :logcollector и ему это всё не нужно.
        if (!isMainProcess()) {
            Log.d(TAG, "⏭️ Skipping init — running in :logcollector process")
            return
        }

        // ── 2. Запускаем LogCollectorService в отдельном процессе ─────────
        //    Это ПЕРВОЕ что делаем — чтобы если дальше что-то крашнет,
        //    коллектор уже был запущен и ловил logcat.
        try {
            LogCollectorService.start(this)
            Log.d(TAG, "✅ LogCollectorService started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ LogCollectorService start failed", e)
        }

        // ── 3. AppLogger (in-process буфер, дополняет LogCollector) ──────
        initAppLogger()

        // ── 4. CrashLogger (перехватчик uncaught exceptions) ─────────────
        try {
            CrashLogger.init(this).apply {
                cleanOldLogs(keepCount = 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ CrashLogger init failed", e)
        }

        // ── 5. Koin DI ──────────────────────────────────────────────────
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        // ── 6. Firebase ─────────────────────────────────────────────────
        initFirebase()

        // ── 7. WorkManager ──────────────────────────────────────────────
        WorkManagerInitializer.initialize(this)
    }

    /**
     * Проверяет, запущены ли мы в главном процессе.
     * Возвращает false для :logcollector и других вторичных процессов.
     */
    private fun isMainProcess(): Boolean {
        val pid = android.os.Process.myPid()
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = am.runningAppProcesses ?: return true
        for (info in processes) {
            if (info.pid == pid) {
                return info.processName == packageName
            }
        }
        // Если не нашли — считаем главным (безопаснее)
        return true
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
