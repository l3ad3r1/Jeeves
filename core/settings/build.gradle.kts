/*
 * :core:jeeves-settings — Jeeves-only synchronous settings and feature contracts.
 *
 * Depended on by :app, :feature:jotter and :feature:butler. It must therefore depend on
 * none of them, and must stay free of Compose and Hilt so Butler's plain Views and
 * services can use it directly.
 *
 * Storage is SharedPreferences, not DataStore, on purpose: ButlerPrefs is read
 * SYNCHRONOUSLY from AlarmForegroundService and AudioEngine while an alarm is firing.
 * DataStore has no synchronous read, and making the wake path async is a rewrite with
 * real "the alarm didn't go off" risk. See docs/STATE.md ## Decisions.
 *
 * AGP 9 supplies Kotlin support built-in; do NOT apply org.jetbrains.kotlin.android.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.jeeves.core.settings"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        // Carried over from Octo Jotter's build: its annotated constructor properties
        // rely on the pre-2.2 default annotation target.
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    // Flows for the Compose settings UI; the sync getters need nothing.
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
