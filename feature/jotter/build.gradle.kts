/*
 * :feature:jotter — Octo Jotter as an Android library module.
 *
 * Phase 2 scaffold: the module is empty. Sources (Markdown notes UI, GitHub Gist
 * sync, plugin system) land here in Phase 3.
 *
 * The namespace deliberately matches the standalone app's package
 * (com.l3ad3r1.octojotter) so Phase 3 can move sources across without renaming
 * packages and without R-class collisions against :app.
 *
 * AGP 9 supplies Kotlin support built-in; do NOT apply org.jetbrains.kotlin.android.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.l3ad3r1.octojotter"
    compileSdk = 36

    defaultConfig {
        minSdk = 29   // merged floor: highest of Hermes 29 / Jotter 24 / Butler 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Intentionally empty. Jotter's dependencies (Compose, Gist sync, plugin
    // runtime) arrive with its sources in Phase 3, resolved through the shared
    // version catalog.
}
