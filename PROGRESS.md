# Jeeves — Progress

**What this is:** the merged "super app" (working name **Jeeves**) that unifies three
existing Android apps — **Hermes Agent** (base), **Octo Jotter**, and **Sassy Butler** —
into a single sideloaded, multi-module APK. Roadmap: `docs/SUPER_APP_ROADMAP.md`.

The base is the Hermes Agent app (`com.hermes.agent` namespace), imported here as a fresh
repo. Octo Jotter and Sassy Butler are NOT yet imported (Phase 3).

## Build env
- JAVA_HOME = `C:\Program Files\Android\Android Studio\jbr` (JBR 21.0.10)
- ANDROID_HOME = `C:\Users\renja\AppData\Local\Android\Sdk`
- Compile check: `./gradlew :app:compileDebugKotlin`
- Unit tests: `./gradlew :app:testDebugUnitTest` (Robolectric)
- Debug APK: `./gradlew :app:assembleDebug`

## Status log (newest first)

### Phase 0 — Prep & fresh repo — DONE
- [x] Imported Hermes base into `E:\claude-projects\jeeves` (excluded build/, .git, APKs, graphify-out). Commit `8017806`.
- [x] `git init` + initial import commit.
- [x] Baseline recorded on the untouched import:
  - `:app:compileDebugKotlin` -> BUILD SUCCESSFUL (deprecation warnings only).
  - `:app:testDebugUnitTest` -> BUILD SUCCESSFUL, **219 tests, 0 failures, 0 errors** (30 classes).
- [x] Rebrand to "Jeeves": `app_name` -> Jeeves, `rootProject.name` -> Jeeves,
      `applicationId` -> `com.jeeves.app` (namespace stays `com.hermes.agent`).
  - `:app:assembleDebug` -> BUILD SUCCESSFUL; APK package `com.jeeves.app.debug`.

### Phase 1 — Toolchain unification — IN PROGRESS
Target: Gradle 9.6.1 / AGP 9.1.1 / Kotlin 2.2.10 / compileSdk 36 (from 8.13 / 8.13.2 / 2.0.21 / 34).
One bump per commit, build after each. Order: Gradle -> AGP -> Kotlin(+KSP/Compose compiler) -> compileSdk.
- [ ] Gradle wrapper 8.13 -> 9.6.1
- [ ] AGP 8.13.2 -> 9.1.1
- [ ] Kotlin 2.0.21 -> 2.2.10 (KSP lockstep; check `ksp.useKSP2` flag)
- [ ] Compose BOM 2024.09.02 -> Kotlin-2.2-compatible 2025 BOM
- [ ] compileSdk 34 -> 36 (keep minSdk 29; targetSdk -> 36)

## Next steps
1. Bump Gradle wrapper to 9.6.1; run compile + tests; commit.
2. Bump AGP to 9.1.1; resolve removed/renamed DSL; build; commit.
3. Bump Kotlin to 2.2.10 (+ KSP/Compose compiler); build; commit.
4. Move Compose BOM forward; fix API deprecations; build; commit.
5. Bump compileSdk/targetSdk to 36; build + tests; commit. Phase 1 exit gate.
