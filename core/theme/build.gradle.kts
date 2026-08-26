/*
 * :core:theme — the one Jeeves colour palette.
 *
 * Depended on by :app and :feature:jotter, so it must depend on neither. Butler is
 * View-based and cannot consume a Compose ColorScheme; its XML themes mirror the same
 * hex values in feature/butler/src/main/res/values{,-night}/colors.xml.
 *
 * AGP 9 supplies Kotlin support built-in; do NOT apply org.jetbrains.kotlin.android.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jeeves.core.theme"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
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
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
}
