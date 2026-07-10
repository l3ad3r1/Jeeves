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

### Product UI/UX audit — 2026-07-10
- [x] Created `docs/UI_UX_VISUAL_REPORT_2026-07-10.md`: a code-evidenced visual audit of
      Material Design, accessibility, navigation, trust, safety, workflow continuity, and the
      shared design system across the Jeeves host, Notes, and Alarms.
- [x] Prioritized 30 findings: 0 Critical, 10 High, 15 Medium, and 5 Low, each with an impact
      statement and recommended fix. Added a severity scorecard, journey and information-
      architecture diagrams, phased remediation plan, and device-validation checklist.
- [ ] Runtime validation remains: no emulator/device was connected for this audit. Run the
      report's TalkBack, Switch Access, 200% font, reduced-motion, theme, and screenshot matrix.

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

### Phase 4 — Unified Hilt graph — DONE
`HermesApp` remains the only `@HiltAndroidApp` (verified: exactly 1 in the tree, and it is
the only `Application` subclass). Jotter and Butler had **zero** Hilt annotations — they were
ported verbatim and wire themselves manually.

- [x] Hilt + KSP added to `:feature:jotter` and `:feature:butler`.
- [x] `JotterModule` contributes `AppDatabase`, `NoteDao`, `PluginDao`, `TokenManager`,
      `GithubApiService`, `NoteRepository` into `SingletonComponent`.
- [x] `ButlerModule` contributes `AlarmScheduler`.
- [x] `app/.../di/FeatureBridge.kt` — a Hilt `@EntryPoint` exposing `noteRepository()` and
      `alarmScheduler()`. This is the seam Phase 6's tools (and Butler's plain
      `BroadcastReceiver`) will use, and it turns the unified graph into a **compile-time
      guarantee**: Dagger only validates bindings reachable from an entry point.

**Bindings are additive — no behaviour change.** Jotter's `NoteViewModel`/`SyncWorker` still
construct `NoteRepository` directly; Butler's components still construct what they need.

**The roadmap's predicted "duplicate OkHttpClient binding" did not materialise.** Jotter's
client is a private field inside the `RetrofitClient` *object*, never a Hilt binding, so there
is nothing to collide with. The graph has exactly one `OkHttpClient` provider (Hermes's
`NetworkModule`). `JotterModule` deliberately does not bind one — doing so *would* have been a
duplicate-binding compile error. Consolidating the two client instances (they have different
interceptors/timeouts) is a separate behaviour-changing decision; not done.

**`TtsEngine` is deliberately NOT bound.** Its `init` block calls `initSession()`, which does
`context.assets.open(...).readBytes()` on the ~92 MB ONNX model and builds an ORT session
*synchronously*. A `@Singleton @Provides` would load 92 MB on whatever thread first injected
it — possibly the main thread. Exposing TTS as a shared service needs a lazy, off-main-thread
guard: Phase 6.

**Not done (deliberately):** no Butler component was converted to `@AndroidEntryPoint`. The
roadmap says "where they need injection" and, today, none do. Convert when Phase 6 requires it.

**Verified:**
- `:app:assembleDebug` + `:app:testDebugUnitTest` -> BUILD SUCCESSFUL, 219 tests, 0 failures.
- Both feature modules aggregate into the graph: `hilt_aggregated_deps/_com_l3ad3r1_octojotter_di_JotterModule.java`
  and `..._com_sassybutler_alarm_di_ButlerModule.java` are generated.
- The generated `DaggerHermesApp_HiltComponents_SingletonC` now materialises
  `provideNoteRepository`, `provideAlarmScheduler`, `provideAppDatabase`, `provideTokenManager`,
  `provideGithubApiService` plus both entry-point accessors. (Before `FeatureBridge` existed
  they were absent — Dagger prunes bindings nothing consumes, so "module on the classpath"
  is NOT the same as "binding resolves".)
- On-device: app installs and `com.hermes.agent.MainActivity` resumes with no Hilt/Dagger
  runtime error and no crash. The objects the bridge provides were already proven
  constructible on-device in Phase 3, since `NoteViewModel` builds the same
  `NoteRepository(noteDao, githubApiService, tokenManager)` and Jotter's UI rendered.

**NOTED (unverified):** Jotter's `SyncWorker` is a plain `CoroutineWorker`, not `@HiltWorker`,
while `HermesApp` installs `HiltWorkerFactory`. `HiltWorkerFactory` should return null for
unknown workers and let WorkManager fall back to default instantiation, but Gist sync has not
been run. Check when exercising Jotter sync.

### Phase 5 — Data layer — SKIPPED (deliberately, decision recorded)
Jotter keeps its own Room database (`gist_notes_database`), separate from `hermes.db`. Both
files coexist on device (verified via `run-as ... ls databases/`). No entities were folded into
`HermesDatabase` and no migration was written, because the agent reaches notes through the
injected `NoteRepository`, not by querying tables. Revisit only if the agent must query notes
directly (e.g. full-text search across notes from the agent's own RAG index).

### Phase 6 — Integration synergies — IN PROGRESS
- [x] **`create_note` tool** -> Octo Jotter's `NoteRepository`.
- [x] **`set_alarm` tool** -> Sassy Butler's `AlarmScheduler` + `AlarmStore`.
- [x] **Shared TTS** — `ButlerSpeech` (`:feature:butler`) wraps `TtsEngine` behind a lazy,
      mutex-guarded init on `Dispatchers.IO`, and Hermes's `speak` tool now uses it by default.

Both tools follow the mandatory 3-step wiring rule:
1. registered in `di/ToolsModule` (as `provideToolRegistry` parameters),
2. granted in `data/agent/agents/AgentToolAccess` (CONVERSATIONAL + PRODUCTIVITY),
3. named in both agents' `systemPrompt`.

Design notes:
- `create_note` is distinct from the pre-existing `notes` tool, which despite its name stores
  *long-term memories* via `MemoryRepository`. Both descriptions cross-reference each other so
  the model does not confuse them.
- `set_alarm` persists to `AlarmStore` **before** scheduling, mirroring Butler's own
  `AddAlarmSheet`. `AlarmReceiver` re-registers stored alarms after reboot, so scheduling an
  unstored alarm would silently vanish on restart. A unit test locks this ordering in.
- `set_alarm` creates one-shot alarms (`days = emptySet()`, Butler labels them "Once") and
  reports a caveat when `SCHEDULE_EXACT_ALARM` is not granted (Butler degrades to an inexact
  alarm rather than throwing).
- Agent-created notes are local-only (`needsSync = false`); pushing to a Gist needs a token and
  is left to Jotter's own sync flow.

**Verified:**
- `:app:testDebugUnitTest` -> **235 tests, 0 failures** (was 219; +16).
  New: `CreateNoteToolTest` (6), `SetAlarmToolTest` (8, Robolectric + real `AlarmStore`),
  and 2 added to `AgentToolAccessTest` — one of which asserts step 3, that any agent granted a
  tool also names it in its system prompt.
- The `persists to AlarmStore before scheduling` test was **proven to detect the bug**: inverting
  the two calls in `SetAlarmTool` turned exactly that one test red
  (`alarm was scheduled before it was persisted expected:<1> but was:<0>`); order restored, green.
- **Runtime, on device:** `OrchestratorImpl @Inject constructor(... toolRegistry: ToolRegistry ...)`
  and `ToolsModule.provideToolRegistry` takes both new tools as parameters, so Dagger cannot build
  the orchestrator without instantiating them. Logcat shows `Orchestrator: Routed to CONVERSATIONAL`
  followed by a real `HTTP 401` from the chat-completions POST (dummy API key) — i.e. the request
  was actually assembled and sent. That proves `CreateNoteTool` -> Jotter's `NoteRepository`/Room and
  `SetAlarmTool` -> Butler's `AlarmScheduler` both resolve across module boundaries in the live app.
  No Dagger/Hilt runtime error, no crash.

**Not verified:** an end-to-end LLM tool call (needs a real API key), and the alarm actually firing
from an agent-created alarm.

### Shared TTS — `ButlerSpeech`
`feature/butler/.../ButlerSpeech.kt` is an injectable `@Singleton` that owns `TtsEngine`.
`TtsEngine` itself is still NOT a Hilt binding, because its `init` loads the ~92 MB ONNX model
synchronously; `ButlerSpeech` builds it exactly once, lazily, on `Dispatchers.IO`, behind a
`Mutex` so concurrent `speak` calls cannot both load the model. Playback mirrors Butler's
`AudioEngine.playPcmBuffer` (Float32 PCM, tail drained before release) with one deliberate
difference: `USAGE_ASSISTANT` instead of `USAGE_ALARM`, so an agent reply is not routed to the
alarm stream or through Do Not Disturb.

Hermes's existing `speak` tool now tries `ButlerSpeech` first and falls back to the platform
`TextToSpeech` when the model is unavailable, or when the model passes `voice='system'`.
Both agents' prompts were updated (step 3 of the wiring rule).

**A real bug the tests caught.** `speak(text, voiceName = VoiceCatalog.selected(context))` looked
harmless, but Kotlin evaluates default arguments **at the call site** (`speak$default`), so
`VoiceCatalog.selected()` — a `SharedPreferences` disk read — ran on the *caller's* thread, before
`withContext(Dispatchers.IO)`. On the main thread that is exactly the stall this wrapper exists to
prevent. The parameter is now `voiceName: String? = null`, resolved inside the IO block.

**Verified:**
- `:app:testDebugUnitTest` -> **241 tests, 0 failures** (+6 `TtsToolTest`: Butler preferred,
  fallback on failure, `voice='system'` bypass, both-engines-fail, stop halts both, missing text).
- `:app:connectedDebugAndroidTest` on a Pixel_7 emulator -> **4/4 passed**
  (`ButlerSpeechInstrumentedTest`). Logcat: `TtsEngine: Loaded voice 'bm_george' (510 style rows)`
  -> `Synthesized 97200 samples (~4s)` -> `AudioTrack: stop(16): called with 97200 frames delivered`
  -> `ButlerSpeech playback complete`. Every synthesized sample reached the speaker.
  Blank text returns false in ~3 ms without loading the model; `release()` unloads and `warmUp()`
  transparently reloads.
- Also added the missing `androidx.test:runner` / `androidx.test:core` androidTest dependencies —
  `AndroidJUnitRunner` was named in `defaultConfig` but the artifact was never declared, so the
  project could not run an instrumented test at all.

### Phase 7 — Release hardening — DONE (except publishing)
Version is now a single source of truth in `gradle.properties` (`jeeves.versionCode=60`,
`jeeves.versionName=0.9.0`), read by `:app` and by `:feature:jotter`'s `BuildConfig.VERSION_NAME`.

**R8 was the real risk, not signing.** Neither Jotter nor Butler ever shipped minified
(both set `isMinifyEnabled = false` standalone), yet the merged release runs R8 + resource
shrinking over them. Added targeted keep rules for every reflective surface:
- `ai.onnxruntime.**` — JNI peers looked up by name from `libonnxruntime.so`.
- `org.mozilla.javascript.**` — Rhino reflects into host objects for plugin scripting.
- Moshi — 26 `@JsonClass` codegen adapters *plus* a reflective `KotlinJsonAdapterFactory`
  (`Converters`, `RetrofitClient`, `PluginRepository`, `NoteViewModel`), which needs Kotlin
  metadata and constructor parameter names.
- Jotter's `plugin.**`, `data.remote.**`, `DatabaseBackup` — constructed by name.
- Butler's manifest entry points (receiver, service, both activities).

**ABI splits.** ONNX Runtime shipped `libonnxruntime.so` for four ABIs = 73.8 MB of the APK,
of which x86/x86_64 were dead weight on a phone. `splits { abi { ... } }` now emits per-ABI
APKs plus a universal one (x86_64 kept so emulators still work).

| Artifact | Size |
|---|---|
| `app-arm64-v8a-release.apk` | 145.5 MB |
| `app-armeabi-v7a-release.apk` | 139.9 MB |
| `app-x86_64-release.apk` | 148.1 MB |
| `app-universal-release.apk` | 200.9 MB |

Composition of the universal APK: TTS models 115.0 MB (Stored, by design), native libs 73.8 MB,
dex 7.6 MB, resources 3.6 MB. R8 + resource shrinking cut 69 MB off the debug build.

**Verified:**
- `:app:assembleRelease` -> BUILD SUCCESSFUL; `:app:testDebugUnitTest` -> 241 tests, 0 failures.
- **Signer on all four APKs: `99255c31ffba1932e4ab2abc12d99b82bf780874b8c686076497157996cf6d6f`**
  (CN=Hermes Agent) — matches the required `99255c31...` prefix.
- `app-arm64-v8a-release.apk` contains only `lib/arm64-v8a/`; TTS models still `Stored`.
- `hermes-release.jks` is byte-identical (md5 `4aa1d0c9...`) to the copy in the Hermes repo and
  remains gitignored. It was copied, never moved.
- **The minified, signed, ABI-split APK was smoke-tested on device** — every reflective surface
  R8 could have broken:
  1. Jotter opens (Room + Moshi `Converters`).
  2. "Export Database to JSON" succeeds -> reflective `DatabaseBackup` adapter works.
  3. Share resolves `content://com.jeeves.app.fileprovider/exports/...`.
  4. Butler's ONNX TTS: `Synthesized 297000 samples` -> `AudioTrack: stop(22): called with
     297000 frames delivered`. No `UnsatisfiedLinkError`, no `dlopen` failure.
  5. Community Plugins renders the fetched registry ("Ocean Dark", "Rose Light") -> the
     reflective `RegistryIndex`/`PluginManifest` adapters survive minification.

**NOT DONE — publishing.** The repo has **no git remote**, and publishing a GitHub release is
irreversible, so nothing was pushed. To ship: create the remote, push `master`, then
`gh release create v0.9.0 --latest` with the signed APKs, verifying the signer once more first.

## Next steps
1. Publish (needs an explicit go-ahead): create the GitHub remote, push, cut release `v0.9.0`
   marked `--latest` with the signed APKs. Standing rule: verify signer `99255c31...` before publish.
2. Optional size work: download-on-first-use for the 115 MB TTS models would take the arm64 APK
   from 145 MB to ~30 MB. Bigger change — the models are currently required at build time.
3. Close the last verification gap: an end-to-end LLM tool call with a real API key
   ("wake me at 7am" -> `set_alarm` -> alarm fires).

### Post-Phase-7 code review — two bugs found and fixed
An independent review pass over the session's new integration code found two real bugs,
both fixed with regression tests proven red first:

1. **`set_alarm` id collision (fixed, commit `f9c0cbc`).** Agent ids were `hour*100+minute`
   (0–2359); Butler's UI assigns sequential ids via `AlarmStore.nextId()` (1, 2, 3, ...). The
   ranges overlap in the midnight hour — "12:05am" produced id 5 and silently REPLACED the
   user's fifth UI alarm (upsert and the PendingIntent request code both key on id). Agent ids
   now live at `10000 + hour*100 + minute`, disjoint from the UI space, dedupe preserved.
2. **`speak` stop→fallback leak (fixed).** `ButlerSpeech.speak()` returned a Boolean that
   conflated "engine unavailable" with "user stopped it"; a stop during the multi-second
   synthesis window made `TtsTool` fall through and read the whole text via platform TTS.
   `speak()` now returns `SpeakResult { SPOKEN, STOPPED, UNAVAILABLE }` and only UNAVAILABLE
   falls back. Both regression tests were proven to detect their bug (red) before the fix /
   via mutation; instrumented suite re-run on device: 4/4.

Review findings NOT fixed (accepted, low severity):
- `ButlerSpeech` playback is not mutex-guarded: two concurrent `speak()` calls would overlap
  two AudioTracks and `stop()` only halts the last (`track` is last-writer-wins). The
  orchestrator serializes tool calls today; add a playback mutex if that changes.
- The minified export verification ran against an empty DB, so `NoteEntity`'s reflective
  serialization with real data was never exercised under R8 (the Room `@Entity { *; }` keep
  rule covers it).
- Agent alarm at a time where a UI alarm already exists = two alarms firing together
  (different ids). Acceptable; by design.

## Deferred / noted
- `DeviceControlAgent`'s and `CreativeAgent`'s prompts still describe `speak` without the new
  `voice` parameter; both are granted the tool and get Butler's voice by default.
- `ButlerSpeech` keeps the ONNX session warm after first use (reload costs seconds). `release()`
  exists for memory pressure but nothing calls it — consider wiring it to `MemoryPressureMonitor`.

## Verified at runtime: the FileProvider export fix (Phase 3a)
Tested on the Pixel_7 emulator via Jotter Settings -> "Export Database to JSON" ->
"Share backup file", which calls `getUriForFile` on `filesDir/exports/`.

- **With the fix** (host `file_paths.xml` carrying Jotter's 3 roots): the share sheet opens
  ("Sharing 1 file - octojotter_backup_*.json"), the process stays alive, and the URI
  `content://com.jeeves.app.debug.fileprovider/exports/octojotter_backup_*.json` is built.
  The `/exports/` segment can only come from the merged `files-path name="exports"` root.
- **Counterfactual (fix removed, rebuilt, reinstalled)**: the same tap hard-crashes:
  `FATAL EXCEPTION: main / java.lang.IllegalArgumentException: Failed to find configured
  root that contains /data/data/com.jeeves.app.debug/files/exports/octojotter_backup_*.json`
  at `FileProvider$SimplePathStrategy.getUriForFile` <- `NoteApp.kt:3871`.
  Fix restored, rebuilt, re-tested green.

So the latent crash was real, and the fix both necessary and sufficient. This is the class
of bug that a build and a unit-test suite cannot catch.

**NOTED (pre-existing, not a merge regression):** the share sheet logs
`ChooserPreview: Could not read content://... stream types ... call Intent#setClipData()`.
Jotter's share block never calls `setClipData`; the ported source is byte-identical to
upstream, which has the same omission. Only the chooser's *preview* thumbnail is affected —
the share itself works. Fix upstream if it ever matters.

## Not yet exercised at runtime
- Jotter: creating/saving a note, Gist sync, biometric app-lock, share-target intent,
  and the in-app updater's `jotter-updates` FileProvider root.
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
