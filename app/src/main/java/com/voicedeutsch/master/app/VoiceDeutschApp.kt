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

class VoiceDeutschApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // 🔥 КРИТИЧЕСКИ ВАЖНО: Устанавливаем токен ЗДЕСЬ!
        // Это происходит ДО инициализации FirebaseInitProvider
        try {
            if (BuildConfig.USE_DEBUG_APP_CHECK) {
                val token = BuildConfig.APP_CHECK_DEBUG_TOKEN
                if (token.isNotEmpty()) {
                    System.setProperty("firebase.app-check.debug-token", token)
                    Log.d("VoiceDeutschApp", "✅ App Check debug token set in attachBaseContext")
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceDeutschApp", "Failed to set debug token", e)
        }
    }

    override fun onCreate() {
        initAppLogger()
        super.onCreate()

        try {
            CrashLogger.init(this).apply {
                cleanOldLogs(keepCount = 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ CrashLogger init failed", e)
        }

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

    private fun initFirebase() {
        try {
            // Проверяем, инициализировал ли уже провайдер Firebase
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
                val debugProviderFactory = com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(debugProviderFactory)
                Log.d(TAG, "✅ App Check initialized [DEBUG_PROVIDER]")
            } else {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
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