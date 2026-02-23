package com.voicedeutsch.master.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    // ✅ attachBaseContext вызывается ДО onCreate — самая ранняя точка входа.
    // CrashLogger здесь перехватит даже крэш в Koin init или Firebase init.
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
        // AppLogger стартует ДО super() — чтобы не пропустить системные события onCreate.
        initAppLogger()

        super.onCreate()

        // Koin — ДО Firebase, чтобы DI-граф был готов раньше Firebase-колбэков.
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        // Firebase: строгий порядок — App → AppCheck → Crashlytics → Analytics.
        initFirebase()

        WorkManagerInitializer.initialize(this)

        // Копируем последний краш в Downloads (если есть) — после полной инициализации.
        CrashLogger.getInstance()?.copyLatestCrashToDownloads(this)
    }

    // ─────────────────────────────────────────────────────────────────────────

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
     * 1. [FirebaseApp.initializeApp] — обязателен первым. Читает google-services.json,
     *    создаёт singleton FirebaseApp. Без него все Firebase вызовы бросят IllegalStateException.
     *
     * 2. [FirebaseAppCheck] — СРАЗУ после initializeApp, ДО любых обращений к Firestore,
     *    Storage, firebase-ai и т.д. App Check токен автоматически прикрепляется ко всем
     *    Firebase-запросам — но только если провайдер установлен до первого запроса.
     *
     *    Debug-сборка: [DebugAppCheckProviderFactory] — генерирует UUID-токен в Logcat.
     *      → Токен нужно вручную добавить в Firebase Console → App Check → Debug tokens.
     *    Release-сборка: [PlayIntegrityAppCheckProviderFactory] — валидирует через Google Play.
     *      → SHA-256 fingerprint release-ключа обязателен в Firebase Console → Project settings.
     *
     * 3. [FirebaseCrashlytics] — настройка кастомных ключей до первого крэша.
     *    В debug-сборках сбор крэшей отключён (не засоряем production дашборд).
     *
     * 4. [FirebaseAnalytics] — аналогично, в debug отключаем сбор.
     */
    private fun initFirebase() {
        try {
            // 1. Инициализация FirebaseApp
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ FirebaseApp initialized")

            // 2. App Check — устанавливаем провайдер ДО первого Firebase-запроса
            initAppCheck()

            // 3. Crashlytics
            initCrashlytics()

            // 4. Analytics
            initAnalytics()

        } catch (e: Exception) {
            // Firebase не обязателен для базовой работы приложения.
            // Логируем ошибку, но не крашим приложение.
            Log.e(TAG, "❌ Firebase init failed: ${e.message}", e)
        }
    }

    private fun initAppCheck() {
        try {
            val providerFactory = if (BuildConfig.DEBUG) {
                // Debug: UUID-токен генерируется автоматически и выводится в Logcat.
                // Ищите строку: "DebugAppCheckProvider: Enter this debug secret into the
                // allow list in the Firebase Console for your project: <UUID>"
                DebugAppCheckProviderFactory.getInstance()
            } else {
                // Release: Play Integrity API — верификация через Google Play.
                // Требования:
                //   • APK подписан release-ключом
                //   • SHA-256 fingerprint добавлен в Firebase Console
                //   • Google Play Integrity API включён в Google Cloud Console
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)
            Log.d(TAG, "✅ App Check initialized [${if (BuildConfig.DEBUG) "DEBUG" else "PLAY_INTEGRITY"}]")

        } catch (e: Exception) {
            // App Check не инициализирован — Firebase-запросы пойдут без токена.
            // В режиме enforcement это приведёт к отклонению запросов сервером.
            Log.e(TAG, "❌ App Check init failed: ${e.message}", e)
        }
    }

    private fun initCrashlytics() {
        try {
            Firebase.crashlytics.apply {
                // В debug-сборках отключаем отправку крэшей — не засоряем дашборд.
                // Локальные крэши всё равно пишутся в CrashLogger (файловый лог).
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

                // Кастомные ключи: помогают фильтровать крэши в Firebase Console.
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
                // В debug-сборках отключаем сбор — не засоряем production данные.
                // Для отладки событий используйте: adb shell setprop debug.firebase.analytics.app <packageName>
                setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
            }
            Log.d(TAG, "✅ Analytics initialized [collection=${!BuildConfig.DEBUG}]")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Analytics init failed: ${e.message}", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private companion object {
        const val TAG = "VoiceDeutschApp"
    }
}
