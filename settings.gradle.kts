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

// Shared engine modules — source lives in the separate agent-core repo, which
// is NOT vendored here. Resolution order, first hit wins:
//   1. -PagentCoreDir=<path> or the AGENT_CORE_DIR environment variable
//   2. ./agent-core        — a checkout inside this repo (what CI does)
//   3. ../agent-core       — a sibling checkout (the local dev layout)
// A checkout that cannot find it fails here with instructions rather than with
// Gradle's bare "projectDirectory does not exist".
val agentCoreDir: File = run {
    val explicit = (settings.providers.gradleProperty("agentCoreDir").orNull
        ?: System.getenv("AGENT_CORE_DIR"))
        ?.takeIf { it.isNotBlank() }
        ?.let { file(it) }
    // An explicitly named path is authoritative: honour it or fail. Only the
    // implicit layouts fall through to each other.
    val candidates = if (explicit != null) listOf(explicit) else listOf(file("agent-core"), file("../agent-core"))
    candidates.firstOrNull { File(it, "core/domain/build.gradle.kts").isFile }
        ?: error(
            buildString {
                appendLine("Cannot find the agent-core engine checkout.")
                appendLine()
                appendLine("Jeeves maps its :core:* Gradle projects into the separate")
                appendLine("agent-core repository. Clone it beside this checkout:")
                appendLine()
                appendLine("    git clone https://github.com/l3ad3r1/agent-core.git ../agent-core")
                appendLine()
                appendLine("...or point at an existing clone with -PagentCoreDir=<path>")
                appendLine("or the AGENT_CORE_DIR environment variable.")
                appendLine()
                appendLine("Looked in:")
                candidates.forEach { appendLine("  - $it") }
            }
        )
}

include(":core:theme")
include(":core:util")
include(":core:domain")
include(":core:plugin")
include(":core:settings")
include(":core:persistence")
include(":core:memory")
include(":core:llm")
include(":core:tools")
include(":core:jeeves-settings")
include(":core:jeeves-theme")
include(":feature:jotter")
include(":feature:butler")

listOf("theme", "util", "domain", "plugin", "settings", "persistence", "memory", "llm", "tools")
    .forEach { module -> project(":core:$module").projectDir = File(agentCoreDir, "core/$module") }

// Jeeves-only cross-feature settings/contracts and branding remain private.
project(":core:jeeves-settings").projectDir = file("core/settings")
project(":core:jeeves-theme").projectDir = file("core/theme")
