package com.voicedeutsch.master.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.voicedeutsch.master.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry point.
 *
 * Responsibilities:
 *  1. Initialise Koin DI with all module graphs.
 *  2. Initialise Firebase for crash reporting and analytics.
 *
 * Registered in AndroidManifest.xml as `android:name=".app.VoiceDeutschApp"`.
 */
class VoiceDeutschApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── Koin DI ──────────────────────────────────────────────────────────
        startKoin {
            // G4 FIX: Use Level.DEBUG in debug builds so Koin injection
            // warnings and dependency resolution issues are visible in logcat.
            // Previously Level.ERROR suppressed all diagnostic output.
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        // ── Firebase ─────────────────────────────────────────────────────────
        // H6 FIX: Firebase is now initialized with a safe fallback.
        //
        // Prerequisites:
        //   - google-services.json must be placed in app/ directory
        //   - google-services and firebase-crashlytics Gradle plugins must be applied
        //
        // If google-services.json is missing (e.g. local dev build without
        // Firebase project access), initialization will fail gracefully and
        // log a warning instead of crashing the app.
        initFirebase()
    }

    /**
     * Initializes Firebase services (Crashlytics, Analytics, Performance).
     *
     * Wrapped in try/catch so builds without google-services.json don't crash.
     * In production, if this fails, you're flying blind with no crash reports —
     * the warning log should be caught during QA.
     */
    private fun initFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            // This typically means google-services.json is missing or malformed.
            // The app will still function, but crash reporting and analytics
            // will be unavailable.
            Log.w(
                TAG,
                "Firebase initialization failed — crash reporting and analytics " +
                    "are disabled. Ensure google-services.json is present in the " +
                    "app/ directory. Error: ${e.message}",
            )
        }
    }

    private companion object {
        const val TAG = "VoiceDeutschApp"
    }
}
