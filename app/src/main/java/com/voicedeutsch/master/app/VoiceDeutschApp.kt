package com.voicedeutsch.master.app

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
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
import java.io.File

class VoiceDeutschApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()

        // ── 1. Определяем процесс ───────────────────────────────────────
        // :logcollector процесс НЕ должен инициализировать Koin/Firebase/AppLogger.
        // Используем /proc/self/cmdline — самый надёжный способ (ActivityManager ненадёжен).
        if (!isMainProcess()) {
            Log.d(TAG, "⏭️ Skipping init — running in secondary process: ${getProcessName()}")
            return
        }

        // ── 2. AppLogger (in-process буфер) ─────────────────────────────
        initAppLogger()

        // ── 3. CrashLogger (перехватчик uncaught exceptions) ────────────
        try {
            CrashLogger.init(this).apply {
                cleanOldLogs(keepCount = 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ CrashLogger init failed", e)
        }

        // ── 4. Koin DI ─────────────────────────────────────────────────
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        // ── 5. Firebase ────────────────────────────────────────────────
        initFirebase()

        // ── 6. WorkManager ─────────────────────────────────────────────
        WorkManagerInitializer.initialize(this)

        // ── 7. LogCollectorService — запускаем ПОСЛЕДНИМ с задержкой ────
        //    Задержка 3 сек чтобы не мешать стартовой инициализации
        //    и не конфликтовать с другими foreground services.
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                LogCollectorService.start(this)
                Log.d(TAG, "✅ LogCollectorService started")
            } catch (e: Exception) {
                Log.e(TAG, "❌ LogCollectorService start failed", e)
            }
        }, 3_000L)
    }

    /**
     * Надёжная проверка процесса через /proc/self/cmdline.
     * НЕ зависит от ActivityManager (который может вернуть null на Samsung/Xiaomi).
     */
    private fun isMainProcess(): Boolean {
        val processName = getProcessName()
        return processName == packageName
    }

    /**
     * Читает имя текущего процесса из /proc/self/cmdline.
     * Это самый надёжный способ — работает на всех Android 5+.
     */
    private fun getProcessName(): String {
        return try {
            File("/proc/self/cmdline").readText().trim('\u0000', ' ', '\n')
        } catch (_: Exception) {
            // Fallback — считаем главным процессом (безопаснее чем пропустить init)
            packageName
        }
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
