/*
 * Jeeves — merged super app (Hermes Agent + Octo Jotter + Sassy Butler).
 * Root settings file. See docs/SUPER_APP_ROADMAP.md.
 *
 * Module layout:
 *   :app             — host: launcher, navigation, single Hilt graph (the Hermes Agent base)
 *   :feature:jotter  — Octo Jotter as an Android library (Compose)
 *   :feature:butler  — Sassy Butler as an Android library (View-based, no Compose)
 *
 * The two feature modules are empty scaffolds as of Phase 2; sources are ported
 * into them in Phase 3.
 */

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Termux terminal engine (terminal-view / terminal-emulator) is
        // published from github.com/termux/termux-app via JitPack.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Jeeves"
include(":app")
include(":feature:jotter")
include(":feature:butler")
