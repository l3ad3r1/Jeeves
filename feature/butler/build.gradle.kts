/*
 * :feature:butler — Sassy Butler as an Android library module.
 *
 * Phase 2 scaffold: the module is empty. Sources (AlarmReceiver,
 * AlarmForegroundService, lock-screen AlarmActivity, AudioEngine, ONNX TTS) and
 * the ~115 MB of TTS model assets land here in Phase 3.
 *
 * Butler is View-based, NOT Compose — compose stays off here on purpose.
 *
 * The namespace deliberately matches the standalone app's package
 * (com.sassybutler.alarm) so Phase 3 can move sources across without renaming
 * packages and without R-class collisions against :app.
 *
 * Deferred to Phase 3 (needed only once the assets/deps exist):
 *   - androidResources { noCompress += listOf("onnx", "bin") }
 *   - packaging { jniLibs { pickFirsts += "**\/libonnxruntime.so" } }
 *   - dependencies: appcompat, material, constraintlayout, splashscreen,
 *     lifecycle-service, onnxruntime-android
 *
 * AGP 9 supplies Kotlin support built-in; do NOT apply org.jetbrains.kotlin.android.
 */

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sassybutler.alarm"
    compileSdk = 36

    defaultConfig {
        minSdk = 29   // merged floor: highest of Hermes 29 / Jotter 24 / Butler 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Intentionally empty. Butler's dependencies arrive with its sources in Phase 3.
}
