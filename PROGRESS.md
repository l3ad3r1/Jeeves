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

### Phase 0 — Prep & fresh repo — IN PROGRESS
- [x] Imported Hermes base into `E:\claude-projects\jeeves` (excluded build/, .git, APKs, graphify-out).
- [ ] `git init` + initial import commit.
- [ ] Baseline build recorded (compile + unit tests on the untouched import).
- [ ] Rebrand to "Jeeves" (app label + applicationId).

### Phase 1 — Toolchain unification — NOT STARTED
Target: Gradle 9.6.1 / AGP 9.1.1 / Kotlin 2.2.10 / compileSdk 36 (from 8.13 / 8.13.2 / 2.0.21 / 34).
One bump per commit, build after each.

## Next steps
1. `git init`, commit the untouched import.
2. Run baseline `:app:compileDebugKotlin` + `:app:testDebugUnitTest`; paste results here.
3. Rebrand to Jeeves; rebuild; commit.
4. Begin Phase 1 toolchain bumps (Gradle -> AGP -> Kotlin -> compileSdk), committing each.
