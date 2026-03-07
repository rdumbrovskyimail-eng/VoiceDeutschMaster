import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("jacoco")
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
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            buildConfigField("Boolean", "DEBUG_MODE", "true")
            buildConfigField("Boolean", "USE_DEBUG_APP_CHECK", "true")
            buildConfigField(
                "String",
                "APP_CHECK_DEBUG_TOKEN",
                "\"${project.findProperty("appCheckDebugToken") ?: ""}\""
            )
        }

        create("releaseDebug") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "DEBUG_MODE", "false")
            buildConfigField("Boolean", "USE_DEBUG_APP_CHECK", "true")
            buildConfigField(
                "String",
                "APP_CHECK_DEBUG_TOKEN",
                "\"${project.findProperty("appCheckDebugToken") ?: ""}\""
            )
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "DEBUG_MODE", "false")
            buildConfigField("Boolean", "USE_DEBUG_APP_CHECK", "false")
            buildConfigField("String", "APP_CHECK_DEBUG_TOKEN", "\"\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
                "-opt-in=com.google.firebase.ai.type.PublicPreviewAPI",
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
                it.jvmArgs("-Xmx2g")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
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

    // ── Ktor ─────────────────────────────────────────────────────────────────
    implementation(libs.bundles.ktor)

    // ── Kotlin ───────────────────────────────────────────────────────────────
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // ── Security ─────────────────────────────────────────────────────────────
    implementation(libs.security.crypto)

    // ── WorkManager ──────────────────────────────────────────────────────────
    implementation(libs.work.runtime.ktx)

    // ── Firebase ─────────────────────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    "releaseDebugImplementation"(libs.firebase.appcheck.debug)

    implementation(libs.kotlinx.coroutines.play.services)

    // ── UI Polish ─────────────────────────────────────────────────────────────
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // ── Unit Testing (src/test/) ─────────────────────────────────────────────
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
    testImplementation(libs.test.core)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ── Android Instrumented Testing (src/androidTest/) ──────────────────────
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

jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }

    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*_Factory.*",
            "**/*_MembersInjector.*",
            "**/*_Impl.*",
            "**/*_Impl$*.*",
            "**/di/**Module*",
            "**/app/MainActivity*",
            "**/app/VoiceDeutschApp*",
            "**/app/StaticDebugAppCheckProviderFactory*",
            "**/presentation/screen/**Screen*",
            "**/presentation/screen/**Canvas*",
            "**/presentation/components/**",
            "**/presentation/theme/**",
            "**/presentation/navigation/AppNavigation*",
            "**/presentation/navigation/NavAnimations*",
            "**/util/LogViewerDialog*",
            "**/ComprehensiveTestScreen*",
            "**/RuntimeTestScreen*",
        )
    }

    classDirectories.setFrom(kotlinClasses)
    sourceDirectories.setFrom("${project.projectDir}/src/main/java")
    executionData.setFrom(
        fileTree(layout.buildDirectory) { include("jacoco/testDebugUnitTest.exec") }
    )
}

tasks.register("printAppCheckToken") {
    val token = project.findProperty("appCheckDebugToken")?.toString()
        ?: "❌ НЕ ЗАДАН — добавь APP_CHECK_DEBUG_TOKEN в GitHub Secrets"

    doLast {
        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🔑 APP CHECK DEBUG TOKEN: $token")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("👆 Добавь этот токен в Firebase Console:")
        println("   App Check → Apps → твоё приложение → Manage debug tokens")
        println("")
    }
}