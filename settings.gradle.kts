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
include(":core:theme")
include(":core:util")
include(":core:domain")
include(":core:plugin")
include(":core:settings")
include(":core:persistence")
include(":core:memory")
include(":core:llm")
include(":core:jeeves-settings")
include(":core:jeeves-theme")
include(":feature:jotter")
include(":feature:butler")

// Shared, product-neutral theme primitives live in the public agent-core repo.
project(":core:theme").projectDir = file("../agent-core/core/theme")
project(":core:util").projectDir = file("../agent-core/core/util")
project(":core:domain").projectDir = file("../agent-core/core/domain")
project(":core:plugin").projectDir = file("../agent-core/core/plugin")
project(":core:settings").projectDir = file("../agent-core/core/settings")
project(":core:persistence").projectDir = file("../agent-core/core/persistence")
project(":core:memory").projectDir = file("../agent-core/core/memory")
project(":core:llm").projectDir = file("../agent-core/core/llm")

// Jeeves-only cross-feature settings/contracts and branding remain private.
project(":core:jeeves-settings").projectDir = file("core/settings")
project(":core:jeeves-theme").projectDir = file("core/theme")
