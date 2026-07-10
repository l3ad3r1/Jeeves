/*
 * :feature:jotter — Octo Jotter as an Android library module.
 *
 * The namespace deliberately matches the standalone app's package
 * (com.l3ad3r1.octojotter) so sources moved across keep their package
 * declarations and their R class does not collide with :app.
 *
 * AGP 9 supplies Kotlin support built-in; do NOT apply org.jetbrains.kotlin.android.
 *
 * Dropped from the standalone build, because nothing in the 32 ported sources
 * references them (verified by grep before the port):
 *   - firebase-bom / firebase-ai / firebase-appcheck-recaptcha and the
 *     com.google.gms.google-services plugin  -> 0 usages; also removes the need
 *     for a google-services.json in this repo.
 *   - the secrets Gradle plugin (.env)       -> the only BuildConfig reference is
 *     VERSION_NAME, which is supplied below.
 *   - roborazzi (screenshot tests)           -> Jotter's 4 test sources are not ported yet.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.l3ad3r1.octojotter"
    compileSdk = 36

    defaultConfig {
        minSdk = 29   // merged floor: highest of Hermes 29 / Jotter 24 / Butler 26

        // An Android library's BuildConfig does NOT get VERSION_NAME, but
        // NoteViewModel reads com.l3ad3r1.octojotter.BuildConfig.VERSION_NAME.
        // Supply it explicitly rather than editing the ported source.
        buildConfigField("String", "VERSION_NAME", "\"${project.findProperty("jeeves.versionName") ?: "0.8.9"}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    // --- Hilt (contributes JotterModule to the host's single object graph) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // --- Compose (BOM-managed) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- AndroidX ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    // Also supplies androidx.fragment: MainActivity is a FragmentActivity because
    // BiometricPrompt requires one.
    implementation(libs.androidx.biometric)

    // --- Room (Jotter keeps its own database file; see Phase 5) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- Networking (GitHub Gist sync) ---
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // --- Coroutines ---
    implementation(libs.kotlinx.coroutines.android)

    // --- Community-plugin scripting runtime (interpreted, sandboxed, no native code) ---
    implementation(libs.rhino)
}
