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

### Phase 3 — Feature port + manifest merge (booting merged shell) — IN PROGRESS
- [x] **3a. Octo Jotter -> `:feature:jotter`** (commit `4ca4f69`). 32 sources moved verbatim.
      Dropped Firebase + google-services + secrets plugin (all unused — 0 grep hits).
      Kept Jotter's own `MainActivity` (a `FragmentActivity`, required by BiometricPrompt;
      the host's is a `ComponentActivity`), minus its LAUNCHER filter, keeping ACTION_SEND.
      Jotter's FileProvider roots merged into the host's `file_paths.xml` (its own copy
      would have been overridden -> `getUriForFile` would throw at runtime).
- [x] **3b. Sassy Butler -> `:feature:butler`.** 15 sources + res (no mipmaps) + the
      ~120 MB TTS models. Models are **gitignored** (mirroring the standalone repo) but
      are REQUIRED at `feature/butler/src/main/assets/tts/` for the build.
      `noCompress(onnx,bin)` and jniLibs `pickFirsts` live in `:app`, not the library,
      because both are applied when the APK is packaged.
- [x] Manifest union verified by parsing the merged manifest (see below).
- [x] **3c. Host navigation** — an "Apps" section on the Hermes home dashboard starts
      `com.l3ad3r1.octojotter.MainActivity` and `com.sassybutler.alarm.MainAlarmSetupActivity`
      by Intent (same APK, no IPC).
- [x] **Runtime smoke test on a Pixel_7 emulator (API 34, x86_64)** — see below.

**MILESTONE REACHED: booting merged shell.** Installed the 283 MB debug APK
(`com.jeeves.app.debug`) and drove the UI with adb:
- Hermes host launches; single launcher icon.
- Tapping **Octo Jotter** opens its Compose UI (Room DB live: "No notes saved locally";
  Gist sync UI present). No crash.
- Tapping **Sassy Butler** opens its View UI ("THE PARLOUR", clock, custom Cinzel/Playfair
  fonts, weather "21 OVERCAST"). Started `from uid 10194 ... result code=0`, i.e. in-process.
  Starting it from `adb shell` is correctly *denied* (`not exported from uid 10194`) — the
  `exported=false` guard works as designed; only the host can launch it.
- **Butler's full ONNX TTS pipeline ran inside the merged app:**
  `TtsEngine: Loaded voice 'bm_george' (510 style rows)` -> `AudioEngine: TTS synthesis
  started` -> `MediaPlayer started (birds)` -> `TtsEngine: Synthesized 299400 samples (~12s)`
  -> `AlarmForegroundService: Birds finished, TTS playing` -> `AudioEngine: AudioTrack
  playback complete`. No crash, no UnsatisfiedLinkError, no OOM.
  This proves the `noCompress(onnx,bin)` packaging, the ONNX native lib, the merged asset
  directory, and `mediaPlayback` coexisting with Hermes's `dataSync` foreground services.

**Exit gate so far (clean build):** `:app:assembleDebug` + `:app:testDebugUnitTest`
-> BUILD SUCCESSFUL, 219 tests, 0 failures. Merged manifest verified with an XML parser:
- exactly **1** launcher activity (`com.hermes.agent.MainActivity`);
- all 4 activities present (Hermes, Jotter, Butler setup, Butler lock-screen alarm);
- **both** boot receivers survive — `com.hermes.agent.service.BootReceiver` and
  `com.sassybutler.alarm.AlarmReceiver` (which keeps BOOT_COMPLETED *and* ACTION_ALARM_FIRE);
- foreground service types coexist: `dataSync` (Hermes x2) + `mediaPlayback` (Butler);
- all 6 Butler permissions merged (SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM,
  FOREGROUND_SERVICE_MEDIA_PLAYBACK, USE_FULL_SCREEN_INTENT, VIBRATE, DISABLE_KEYGUARD).
- TTS models packaged **Stored/uncompressed** as intended; clean APK 282,977,250 bytes.

### Phase 4 — Unified Hilt graph — NOT STARTED

## Next steps
1. Phase 4: one Hilt graph. `HermesApp` stays the sole `@HiltAndroidApp`; convert Butler's
   Activities/Service/Receiver to `@AndroidEntryPoint` where they need injection; resolve
   duplicate bindings (two OkHttpClients: Hermes's and Jotter's RetrofitClient).
2. Phase 5: data layer. Jotter ships its own Room DB (`AppDatabase`) separate from
   `HermesDatabase` — keep them separate unless the agent must query notes directly
   (which Phase 6's `create_note` tool will decide).
3. Phase 6: the payoff — `create_note` / `set_alarm` agent tools (each needs all 3 wiring
   steps), and Butler's `TtsEngine` exposed so Hermes can speak agent replies.

## Not yet exercised at runtime
- Jotter: creating/saving a note, Gist sync, biometric app-lock, share-target intent,
  and the FileProvider export path (the `file_paths.xml` merge fix is unverified at runtime).
- Butler: scheduling a real alarm and firing the lock-screen `AlarmActivity`; boot re-registration.
- Hermes: agent chat / cron / foreground service after the toolchain upgrade.

## Deferred / noted (not done)
- Jotter's 4 roborazzi screenshot tests were not ported (would need the roborazzi plugin).
- Butler's `androidTest`/`test` sources not ported.
- Butler pins newer core/lifecycle/activity (1.18.0 / 2.10.0 / 1.13.0); `:feature:butler`
  deliberately consumes the shared catalog's existing versions instead, so no forced
  bump was pushed onto Hermes. Revisit only if Butler needs a newer API.
- Jotter's in-app updater and Hermes's OTA both exist; only one should ship (Phase 7).

## Deferred / noted (not done)
- Butler's toolchain (AGP 9.0.1 / Gradle 9.1.0) is close but not identical; it will inherit
  the root toolchain when it becomes a module in Phase 2/3.
- `-Xjvm-default=all` is deprecated in Kotlin 2.2 (warns; superseded by `-jvm-default`).
- Room is still 2.7.0-alpha11; Octo Jotter uses stable 2.7.0. Consolidate in Phase 2's
  single version catalog.
- Compose deprecation warnings (AutoMirrored icons, `menuAnchor()`, Room
  `fallbackToDestructiveMigration()`) pre-date this work and remain.
