/*
 * :feature:butler — Sassy Butler as an Android library module.
 *
 * The namespace deliberately matches the standalone app's package
 * (com.sassybutler.alarm) so sources moved across keep their package declarations
 * and their R class does not collide with :app.
 *
 * Butler is View-based (AppCompatActivity + viewBinding), NOT Compose.
 *
 * AGP 9 supplies Kotlin support built-in; do NOT apply org.jetbrains.kotlin.android.
 *
 * The ~120 MB of ONNX TTS models live at src/main/assets/tts/ and are gitignored,
 * mirroring the standalone repo. The build needs them present; see .gitignore.
 *
 * `androidResources { noCompress }` and `packaging { jniLibs { pickFirsts } }` are NOT
 * set here: both are packaging concerns applied when the APK is built, so they live in
 * :app/build.gradle.kts. A library's assets are merged into the app before packaging.
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // AlarmForegroundService extends LifecycleService.
    implementation(libs.androidx.lifecycle.service)

    implementation(libs.kotlinx.coroutines.android)

    // On-device TTS inference (Kokoro/KittenTTS ONNX models in src/main/assets/tts).
    implementation(libs.onnxruntime.android)
}
