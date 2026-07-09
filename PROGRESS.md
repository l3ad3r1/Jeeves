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

### Phase 1 — Toolchain unification — DONE
Now on Gradle 9.6.1 / AGP 9.1.1 / Kotlin 2.2.10 / KSP 2.3.5 / Hilt 2.60.1 / compileSdk 36.
- [x] Gradle wrapper 8.13 -> 9.6.1
- [x] AGP 8.13.2 -> 9.1.1 (+ built-in Kotlin migration)
- [x] Kotlin 2.0.21 -> 2.2.10; KSP -> 2.3.5; Hilt 2.52 -> 2.60.1. Commit `f463595`.
- [x] compileSdk 34 -> 36, targetSdk 34 -> 36 (minSdk stays 29).
- [n/a] Compose BOM stays 2024.09.02 — Octo Jotter runs 2024.09.00 on this same
      toolchain, so the roadmap's "Compose BOM skew" risk did not materialise.

**Exit gate verified:** `:app:testDebugUnitTest` + `:app:assembleDebug` -> BUILD SUCCESSFUL;
219 tests, 0 failures; APK `com.jeeves.app.debug` (~73 MB).

**Key findings (feed these back into the roadmap):**
1. Gradle/AGP/Kotlin/KSP/Hilt could NOT be bumped one-per-commit — each forces the next.
   AGP 8.x dies on Gradle >= 9.6.0 (`InternalProblems` removed).
2. AGP 9 has **built-in Kotlin**: the `org.jetbrains.kotlin.android` plugin must be
   removed everywhere, and `android { kotlinOptions { } }` becomes top-level
   `kotlin { compilerOptions { } }`. Keep `kotlin.plugin.compose`, serialization, KSP.
3. Hilt < ~2.57 fails on AGP 9 ("Android BaseExtension not found").
4. KSP must be on the 2.3.x line; older lines add sources via `kotlin.sourceSets`,
   which built-in Kotlin forbids.
5. `ksp.useKSP2=false` had to be dropped (KSP2-only now).

### Phase 2 — Multi-module scaffold — DONE
- [x] `:feature:jotter` (android-library, Compose on) and `:feature:butler`
      (android-library, viewBinding, Compose off) created empty. Commit `870afb8`.
- [x] `:app` depends on both; `settings.gradle.kts` includes both.
- [x] Version catalog consolidation (skew resolved to highest, matching Octo Jotter):
      Room `2.7.0-alpha11` -> `2.7.0` (stable), coroutines `1.9.0` -> `1.10.2`.

**Design decision:** each feature module's `namespace` is the ORIGINAL standalone
package (`com.l3ad3r1.octojotter`, `com.sassybutler.alarm`), not a new one. Phase 3
can therefore move sources across verbatim — no package renames, no R-class collisions.

**Exit gate verified:** `./gradlew projects` lists `:app`, `:feature:jotter`,
`:feature:butler`; `:app:assembleDebug` -> BUILD SUCCESSFUL; `:app:testDebugUnitTest`
-> 219 tests, 0 failures.

**Investigated and dismissed — apparent +12.8 MB APK growth.** After adding the two
empty modules the debug APK read 86.4 MB vs 73.6 MB. Entry-by-entry diff of the two
APKs showed the payloads are byte-identical (dex 65,339,312 stored; native libs
6,686,864 stored; the only new entry is a 5-byte viewBinding version marker). The
extra ~12.9 MB was pure zip gap: AGP's incremental packager (zipflinger) rewrites
`app-debug.apk` in place and leaves freed space instead of compacting it. A clean
build of the current tree produces 73,617,900 bytes — 12 KB SMALLER than the Phase 1
exit APK. **Always measure APK size from a clean build.** No regression; nothing to fix.

### Phase 3 — Feature port + manifest merge (booting merged shell) — NOT STARTED
Port Jotter and Butler sources into their modules, merge the manifests, reach both
from the Hermes launcher.

## Next steps
1. Port Octo Jotter sources into `:feature:jotter` + its dependencies into the catalog.
2. Port Sassy Butler sources + ~115 MB TTS assets into `:feature:butler`; re-add its
   `androidResources { noCompress += ("onnx","bin") }` and jniLibs `pickFirsts` config.
3. Merge the three manifests (permission/service/receiver union per the roadmap).
4. Add host navigation entries so Jotter and Butler are reachable from the launcher.

## Deferred / noted (not done)
- Butler's toolchain (AGP 9.0.1 / Gradle 9.1.0) is close but not identical; it will inherit
  the root toolchain when it becomes a module in Phase 2/3.
- `-Xjvm-default=all` is deprecated in Kotlin 2.2 (warns; superseded by `-jvm-default`).
- Room is still 2.7.0-alpha11; Octo Jotter uses stable 2.7.0. Consolidate in Phase 2's
  single version catalog.
- Compose deprecation warnings (AutoMirrored icons, `menuAnchor()`, Room
  `fallbackToDestructiveMigration()`) pre-date this work and remain.
