# LESSONS.md — the defect ledger every agent reads before writing code

Append-only. Every entry is a **real defect that shipped or nearly shipped in this
repo**, generalized into a rule you can check your diff against. The learning loop
(see `AGENTS.md`) requires: read this file before starting work; self-check your diff
against it before committing; append a new entry whenever a review finds a defect in
your work.

Format for new entries:

```
## L-NNN — <short rule, imperative>
**Origin:** <version/commit/review where the defect appeared>
**Defect:** <what actually went wrong, one or two sentences>
**Rule:** <the generalized, checkable rule>
**Check:** <how to verify a diff complies — a grep, a question, or a test>
```

---

## L-001 — Run the feature once before claiming it works
**Origin:** v0.9.7 (`second brain` repo filter matched zero repos); v0.9.8 review
(summarization launched on a dead scope — never executed once).
**Defect:** Plausible code shipped that could not possibly have survived a single
manual invocation.
**Rule:** A feature claim requires at least one real invocation with real data —
or an explicit `UNVERIFIED` marker in the commit message and `PROGRESS.md`.
**Check:** For each claimed feature in your commit message, name the moment you (or a
test) actually executed that code path. No moment → mark UNVERIFIED.

## L-002 — Never launch coroutines on viewModelScope inside onCleared()
**Origin:** v0.12 drop — conversation summarization.
**Defect:** androidx cancels `viewModelScope` BEFORE `onCleared()` runs; the coroutine
is cancelled at birth. The flagship feature of the drop was dead code.
**Rule:** Work that must outlive a ViewModel goes on a singleton-owned scope (e.g. a
repository's `SupervisorJob` scope). `onCleared()` may only make plain calls.
**Check:** `grep -n "onCleared" -A10` on your diff — any `viewModelScope.launch` inside
is a bug.

## L-003 — Test string filters against the real data they must match
**Origin:** v0.9.7 — `contains("second brain")` vs. actual repo `Dronehire-second-brain`.
**Defect:** A space-vs-hyphen mismatch made the filter match ZERO items; the picker
looked broken with no explanation.
**Rule:** Any new filter/matcher must be exercised against at least one real value it
must accept and one it must reject. Filters that can yield an empty UI need an
empty-state message explaining why.
**Check:** Name the real accepted value in the commit message.

## L-004 — Never hardcode one user's data into product code
**Origin:** v0.9.7 — the "second brain" repo-name literal.
**Defect:** Personal naming conventions baked into shipped logic; breaks for every
other user and for the same user after a rename.
**Rule:** User-specific names, ids, or paths belong in settings/preferences. If you
must ship a heuristic, use it for ORDERING or defaults — never for hiding data.
**Check:** `grep -iE "second.brain|l3ad3r1|renja|dronehire"` over your diff must hit
nothing outside docs/tests.

## L-005 — Persist+schedule is one operation for alarms and jobs
**Origin:** v0.9.6 review — restored alarms were stored but never scheduled.
**Defect:** Alarms restored from backup stayed silent until reboot. The cron loop ten
lines above did it correctly.
**Rule:** Anything that stores a schedulable (alarm, cron, work request) must also
register it with its scheduler in the same code path. Follow the repo pattern:
`AlarmStore.upsert` + `AlarmScheduler.schedule`, mirroring `AddAlarmSheet`.
**Check:** Every new `upsert`/`insert` of a schedulable entity has a paired
`schedule()` call.

## L-006 — Restores and recurring writers must be idempotent
**Origin:** v0.9.6 review (note restore duplicated per tap); v0.9.8 review
(HabitExtractor appended a duplicate memory nightly).
**Defect:** Repeat execution multiplied user data.
**Rule:** Restore paths dedupe against existing records. Recurring snapshot-writers
REPLACE their previous output (delete-then-write with a stable marker), never append.
**Check:** Run the operation twice in your head (or a test): does the second run
change state? It must not.

## L-007 — User actions must never fail silently
**Origin:** v0.9.6 review — NotebookLM buttons (`aiProvider ?: return`) did nothing;
uncaught stream errors left a spinner forever.
**Defect:** The user's exact bug report was "actions don't do anything."
**Rule:** Every user-triggered action needs: a visible outcome on failure (message
naming the fix), try/catch around remote calls, and progress flags reset in `finally`.
**Check:** `grep -n "?: return" ` on UI/ViewModel diffs — each hit needs a
user-visible message instead.

## L-008 — Every await on a human needs a timeout and a headless answer
**Origin:** v0.12 drop — `awaitConfirmation` hung forever on turns with no UI
(Telegram, local API, cron).
**Defect:** A confirmation-requiring tool deadlocked the whole turn permanently.
**Rule:** Human-input awaits get `withTimeoutOrNull` + a safe default (deny), and you
must state what happens when NO UI is attached.
**Check:** Any new `CompletableDeferred`/dialog await: where's the timeout? What does
Telegram see?

## L-009 — New data flows to the LLM must respect privacy flags
**Origin:** v0.12 drop — locked/encrypted notes were indexed into RAG and injected
into cloud-LLM prompts.
**Defect:** The user's explicitly-private (biometric-locked) notes leaked to the cloud.
**Rule:** Any pipeline that moves user content toward a prompt or network call must
filter `locked`/`encrypted` content and EVICT items when they become private later.
**Check:** New ingest/index/injection code: where is the `locked`/`encrypted` filter?

## L-010 — Track offsets on the source string, never on a rebuilt one
**Origin:** v0.12 drop — streamed-TTS `joinToString(" ")` offset drift repeated and
swallowed spoken words.
**Defect:** Rebuilding text changed separator lengths; the position counter skewed.
**Rule:** Progress offsets into a stream are computed from indices in the ACTUAL
accumulated string (regex boundaries), never from lengths of transformed copies.
**Check:** Any `substring`+counter pattern: is the counter derived from the same
string being substringed?

## L-011 — Visual assets: check the foreground against the background it ships on
**Origin:** v0.9.5→0.9.6 — launcher icon changed to a white background while the fill
stayed cream: an invisible icon shipped.
**Defect:** Two halves of one visual changed in different commits; nobody looked at
the composite.
**Rule:** When touching either layer of a fg/bg pair (adaptive icon, widget, theme),
re-verify contrast of the PAIR — render it or compute it.
**Check:** Diff touches `ic_launcher*`/theme colors → state fg and bg hex and their
contrast in the commit message.

## L-012 — A version bump is part of the release, in the same change-set
**Origin:** v0.9.1–v0.9.3 all shipped with versionCode 60 / "0.9.0".
**Defect:** Three releases were indistinguishable to the OTA updater and lied in
"About".
**Rule:** `gradle.properties` versionCode/versionName bump lands with the release
commit; releases are cut from the pushed commit; signer `99255c31…` verified first.
**Check:** `git diff gradle.properties` non-empty in any release-prep change.

## L-013 — Kotlin default arguments evaluate at the CALL SITE
**Origin:** ButlerSpeech (`voiceName = VoiceCatalog.selected(context)` ran a disk read
on the caller's thread before `withContext(IO)`).
**Defect:** The exact main-thread stall the wrapper existed to prevent.
**Rule:** Defaults with side effects (disk, prefs, network) are forbidden — default to
`null` and resolve inside the dispatcher-controlled block.
**Check:** New/changed default parameter values: are any of them function calls with
I/O behind them?

## L-014 — Wire tools in all three places or the model can't use them
**Origin:** Standing repo rule (predates the merge; violated repeatedly upstream).
**Rule:** New agent tool = (1) `di/ToolsModule.kt` registration, (2)
`AgentToolAccess.kt` grant, (3) named in each granted agent's system prompt.
Registration alone does nothing.
**Check:** `grep <tool_name>` must hit all three files.

## L-015 — Measure APK size from a clean build only
**Origin:** Phase 2 — zipflinger's in-place rewrite left ~13 MB of zip gaps that
looked like a regression.
**Rule:** Size comparisons: `clean` first, or compare entry-by-entry.

## L-016 — Blocking downloads need a lifecycle that survives the screen
**Origin:** v0.9.6 — OTA download stalled when the screen slept (fixed with
keep-screen-on, properly fixed with a foreground service in v0.9.7).
**Rule:** Multi-minute network work runs in a foreground service or WorkManager,
not a ViewModel coroutine.

## L-018 — C/C++ builds for Android must not use APIs newer than minSdk
**Origin:** Phase 2 (LLM integration) — `__android_log_is_loggable` failed to link for Android 10 (API 29) because it was introduced in API 30, and the CI did not catch it because it bypassed the NDK build.
**Defect:** App failed to build natively for devices running the declared minSdk.
**Rule:** When adding C/C++ Android NDK code, ensure you use APIs compatible with the `minSdk` defined in `app/build.gradle.kts`. Do not rely on CI to catch NDK build errors unless CI explicitly runs `assembleDebug`.
**Check:** Verify NDK APIs against NDK documentation for the target minSdk.

## L-019 — File I/O inside BroadcastReceivers must be offloaded
**Origin:** Phase 2 (Local LLM) — 800MB model file was copied via `File.copyTo` directly inside `BroadcastReceiver.onReceive` on the main thread, causing the UI to lock up or the process to die silently, leaving the progress bar stuck at 99%.
**Defect:** App stalled at 99% download progress because the main thread was blocked copying a massive file.
**Rule:** Do not perform blocking I/O (like file copying or network requests) directly in `BroadcastReceiver.onReceive`. Offload to a coroutine on `Dispatchers.IO` or use `WorkManager`.
**Check:** `grep -A 5 "override fun onReceive"` and ensure no blocking I/O happens inside its body.

## L-020 — A nested git clone is invisible to every other checkout
**Origin:** Phase 2 (llama.cpp migration, `1ebf409`) — llama.cpp was cloned inside
`app/src/main/cpp/` and committed. Git stored it as a bare gitlink (mode 160000) with
no `.gitmodules` mapping, so every fresh clone — including CI — got an EMPTY directory.
Worse, the two local edits needed to build it (the Vulkan CMake fixes) existed only in
one machine's working tree, recorded nowhere. CI stayed green for days because it never
ran the native build; the first `assembleDebug` run failed instantly, and the fix
attempts that followed (a patch file written via PowerShell redirection → UTF-16,
unreadable by `git apply`; a wrong relative path; then inline `sed`) burned 11 red
commits in 40 minutes.
**Defect:** The repo could not produce its own APK anywhere except one Windows machine,
and nobody knew, because the gate never exercised the path that depended on the missing
files.
**Rule:** Third-party source trees are either (a) real submodules — `.gitmodules` entry,
pinned commit, `submodules: recursive` in the CI checkout — or (b) fully vendored
(delete the inner `.git` and commit the tree). Local modifications to a pinned tree live
IN THIS REPO as `tools/patches/*.patch`, applied by `tools/apply-llama-patches.sh`,
which fails loudly when the pin and the patches drift apart. Never patch via `sed` in
CI — sed no-ops silently when upstream moves the matched line, the same silent-failure
mode as L-007. Generate patch files with `git diff >` from bash, never from PowerShell
redirection (UTF-16 + CRLF make them invalid to `git apply`).
**Check:** `git ls-files -s | grep ^160000` — every hit must have a matching
`.gitmodules` entry AND CI must fetch it. After `apply-llama-patches.sh` runs,
`git -C app/src/main/cpp/llama.cpp status --short` must show ONLY the patched files;
any other dirt is an unrecorded local dependency. And remember: a green CI vouches only
for the tasks it actually ran — before trusting it, confirm the workflow builds what
your change depends on (here CI was "green" while the native build was unbuildable,
because `assembleDebug` was not in the workflow yet).

## L-021 — Never double-wrap prompt formats across language boundaries
**Origin:** v0.10.5 (`LocalLlmProvider` formatting conflict); v0.11.6 review
**Defect:** Local Llama 3 model produced total gibberish due to KV cache corruption. Kotlin code was manually injecting `<|begin_of_text|><|start_header_id|>...` tokens and passing the raw string to the C++ core. Meanwhile, the C++ core's Jinja engine `add_special=true` setting auto-injected a second set of headers, causing double-wrapping.
**Rule:** When bridging between higher-level logic (Kotlin/Java) and lower-level inference engines (C++ llama.cpp), pass the raw system and user prompts as distinct string arguments. Do not manually construct LLM templates (e.g. ChatML, Llama 3) in the host language if the native engine employs Jinja templates. Let the engine natively format the template exactly as the `.gguf` metadata expects it.
**Check:** Grep for `<|begin_of_text|>` or `[INST]` inside Kotlin files. If the model logic natively supports templating (Jinja/gguf), the Kotlin layer should not be concatenating string tags.

## L-022 — Screen teardown must not own application-singleton cleanup
**Origin:** v0.11.7 settings/cache defect — leaving Settings invoked
`LocalLlmManager.close()` from `SettingsViewModel.onCleared()`.
**Defect:** The application-scoped native inference engine was unloaded whenever one
screen was destroyed. Cleanup threw in the normal `Initialized` state, producing the
reported clear-cache/settings crash, and could race a request owned elsewhere.
**Rule:** A screen may request an explicit model switch, but it must never destroy an
application singleton. Native model switches are serialized with generation, and
unload is a suspending, idempotent no-op when no model is loaded.
**Check:** ViewModel `onCleared` methods must not call LLM `close`/`cleanUp`.
Tests must verify explicit model URI/id/directory changes unload before persistence.

## L-023 — Boot receivers must not start restricted foreground-service types
**Origin:** v0.11.7 device logs — `BootReceiver` started the data-sync foreground
service from `BOOT_COMPLETED`.
**Defect:** Android rejected the launch with
`ForegroundServiceStartNotAllowedException`, so reboot recovery crashed instead of
recovering queued work. `LOCKED_BOOT_COMPLETED` also ran before the app's
credential-protected Room/WorkManager state was available.
**Rule:** Reboot recovery is finite, idempotent work enqueued after
`BOOT_COMPLETED`. Continuous foreground monitoring remains an explicit user action
unless Android provides a permitted exemption. Do not touch credential-protected app
state at locked boot.
**Check:** `BootReceiver` contains no service-start call, the manifest does not
register `LOCKED_BOOT_COMPLETED`, and a regression test rejects locked/unrelated
broadcast actions.
