plugins {
    alias(libs.plugins.android.application) apply false
    // kotlin-android УДАЛЁН: AGP 9.0 имеет встроенную поддержку Kotlin
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}
```
