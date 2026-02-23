[plugins]
# ⚠️ AGP 9.0: kotlin-android плагин DEPRECATED при использовании AGP 9.0+.
# Kotlin поддержка теперь встроена в AGP. Не применяйте kotlin-android
# в модулях android-application / android-library.
# kotlin-android оставлен здесь ТОЛЬКО для чистых JVM/KMP модулей,
# где AGP не применяется (напр. отдельный :domain или :data модуль).
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }

# KSP 2.3.5: версия больше НЕ должна совпадать с версией Kotlin.
# KSP2 по умолчанию с начала 2025. KSP1 deprecated.
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

room = { id = "androidx.room", version.ref = "room" }
google-services = { id = "com.google.gms.google-services", version.ref = "google-services" }

# Crashlytics Gradle Plugin 3.0.0 — major release с breaking changes:
# - Требует Gradle 7+ и AGP 7.4+.
# - Удалены устаревшие поля mappingFile и strippedNativeLibsDir.
# - symbolGenerator заменён на symbolGeneratorType + breakpadBinary.
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-plugin" }
app/build.gradle.kts
// app/build.gradle.kts
// Last verified: 2026-02-23
//
// MIGRATION NOTES:
//   AGP 9.0: kotlin-android плагин УДАЛЁН — Kotlin поддержка встроена в AGP.
//   Firebase BoM 34.x: -ktx суффиксы удалены, используем базовые артефакты.
//   App Check: PlayIntegrity в release, Debug-провайдер в debug buildType.

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // ✅ AGP 9.0: kotlin-android УБРАН — встроенная Kotlin-поддержка AGP.
    // Применять kotlin-android здесь НЕЛЬЗЯ — вызовет конфликт с built-in Kotlin.
    // Если нужно временно откатиться: добавьте android.builtInKotlin=false
    // в gradle.properties (сломается в AGP 10.0, mid-2026).
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.voicedeutsch.master"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.voicedeutsch.master"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // Использует дефолтный debug keystore — конфигурация не нужна.
            // App Check в debug-сборках использует DebugAppCheckProviderFactory.
        }
        create("release") {
            val props = Properties()
            val signingFile = rootProject.file("signing.properties")
            if (signingFile.exists()) {
                props.load(signingFile.inputStream())
                storeFile = file(props["STORE_FILE"] as String)
                storePassword = props["STORE_PASSWORD"] as String
                keyAlias = props["KEY_ALIAS"] as String
                keyPassword = props["KEY_PASSWORD"] as String
            }
            // ⚠️ App Check (PlayIntegrity) требует, чтобы SHA-256 fingerprint
            // release-ключа был добавлен в Firebase Console → Project Settings →
            // Your apps → SHA certificate fingerprints.
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // App Check: DebugAppCheckProviderFactory активируется в Application классе
            // при BuildConfig.DEBUG == true. Debug-токен генерируется автоматически
            // и логируется в Logcat: DebugAppCheckProvider: Enter this debug secret...
            // Добавьте этот токен в Firebase Console → App Check → Apps → Debug tokens.
            buildConfigField("Boolean", "DEBUG_MODE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // App Check: PlayIntegrityAppCheckProviderFactory активируется в
            // Application классе при BuildConfig.DEBUG == false.
            buildConfigField("Boolean", "DEBUG_MODE", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        // ✅ AGP 9.0: kotlinOptions {} блок DEPRECATED.
        // Используем compilerOptions {} (новый API, стабилен с AGP 8.1+).
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            freeCompilerArgs.addAll(
                // Coroutines API: нужен для StateFlow.flatMapLatest и др.
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                // Serialization: нужен для @ExperimentalSerializationApi аннотаций.
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
                // Firebase AI (Live API): нужен для LiveSession, AudioConversation.
                // Убрать, когда firebase-ai выйдет из Beta.
                "-opt-in=com.google.firebase.vertexai.type.internal.InternalFirebaseVertexAiAPI",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            // Netty-артефакты попадают через Ktor/OkHttp транзитивно.
            excludes += "/META-INF/io.netty.*"
        }
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
        checkDependencies = true
        htmlReport = true
        baseline = file("lint-baseline.xml")
    }
}

dependencies {

    // ── Compose ──────────────────────────────────────────────────────────────
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // ── Lifecycle ────────────────────────────────────────────────────────────
    implementation(libs.bundles.lifecycle)

    // ── AndroidX Core ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // ── Room ─────────────────────────────────────────────────────────────────
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // ── DataStore ────────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.proto)

    // ── Koin DI ──────────────────────────────────────────────────────────────
    implementation(libs.bundles.koin)

    // ── Ktor (вспомогательные HTTP-запросы) ──────────────────────────────────
    // ℹ️ Основная коммуникация с Gemini — через firebase-ai (Live API).
    // Ktor остаётся для любых других REST-вызовов (не Gemini).
    // После полной миграции на firebase-ai — пересмотрите необходимость Ktor.
    implementation(libs.bundles.ktor)
    implementation(libs.ktor.client.cio)

    // ── Kotlin ───────────────────────────────────────────────────────────────
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // ── Security ─────────────────────────────────────────────────────────────
    // ⚠️ security-crypto 1.1.0-alpha07 — нестабильный релиз. См. libs.versions.toml.
    implementation(libs.security.crypto)

    // ── WorkManager ──────────────────────────────────────────────────────────
    implementation(libs.work.runtime.ktx)

    // ── Firebase ─────────────────────────────────────────────────────────────
    // platform() BoM управляет версиями ВСЕХ firebase-* зависимостей ниже.
    // НЕ указывайте версии для firebase-* явно — только через BoM.
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // App Check: разные провайдеры для debug и release.
    // PlayIntegrity — требует подписанный APK + SHA-256 в Firebase Console.
    // Debug — генерирует временный токен, логируемый в Logcat.
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // coroutines-play-services: .await() для Firebase Tasks в suspend-функциях.
    // Нужен для: auth.signInAnonymously().await(), firestore.get().await() и т.д.
    implementation(libs.kotlinx.coroutines.play.services)

    // ── UI Polish ─────────────────────────────────────────────────────────────
    // TODO: Перенести в libs.versions.toml при следующей ревизии зависимостей.
    // core-splashscreen 1.0.1 — последний стабильный (2023), нет 1.1.x stable.
    implementation("androidx.core:core-splashscreen:1.0.1")
    // ui-text-google-fonts: управляется через Compose BoM — версия не нужна.
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // ── Unit Testing ─────────────────────────────────────────────────────────
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
    testImplementation(libs.work.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // ── Android Instrumented Testing ─────────────────────────────────────────
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.runner)
}

tasks.withType<Test> {
    useJUnitPlatform()
}