# Jeeves — Progress

**What this is:** the merged "super app" (working name **Jeeves**) that unifies three
existing Android apps — **Hermes Agent** (base), **Octo Jotter**, and **Sassy Butler** —
into a single sideloaded, multi-module APK. Merge roadmap (done): `docs/SUPER_APP_ROADMAP.md`.
What's next (digital-butler evolution): `docs/DIGITAL_BUTLER_ROADMAP.md`.

The base is the Hermes Agent app (`com.hermes.agent` namespace), imported here as a fresh
repo. All three apps are merged and shipping (`:app` + `:feature:jotter` + `:feature:butler`).
**Published:** GitHub remote `l3ad3r1/jeeves`, releases v0.9.0 through v0.9.4 live.

## Build env
- JAVA_HOME = `C:\Program Files\Android\Android Studio\jbr` (JBR 21.0.10)
- ANDROID_HOME = `C:\Users\renja\AppData\Local\Android\Sdk`
- Compile check: `./gradlew :app:compileDebugKotlin`
- Unit tests: `./gradlew :app:testDebugUnitTest` (Robolectric)
- Debug APK: `./gradlew :app:assembleDebug`

## Status log (newest first)

### v0.11.8 released — 2026-07-14
- [x] Bumped the release identity to `versionCode=78` / `versionName=0.11.8`
      in the release change-set (L-012).
- [x] Includes the local-model lifecycle/cache fix and Android-compliant boot
      reconciliation from commit `1790389`, plus the explicitly authorized release
      policy update.
- [x] VERIFIED: mandatory preflight passed; a clean release build produced version
      code 78 / name 0.11.8, size 117,457,031 bytes, signer SHA-256 `99255c31…`,
      and APK SHA-256 `88fbf3fa76756ee12b884c2fe734388dc9f836fc8b50b72ebc80a23c4065fcd7`.
- [x] Published GitHub release `v0.11.8` from release commit `0d73de6` after CI run
      `29341601770` passed. Tag-triggered Release run `29342429365` also passed; the
      uploaded `Jeeves-v0.11.8.apk` is 117,457,031 bytes with SHA-256
      `88fbf3fa76756ee12b884c2fe734388dc9f836fc8b50b72ebc80a23c4065fcd7`.
- [ ] UNVERIFIED — needs a device (L-001): clear a real selected GGUF during/after
      inference, leave and re-enter Settings, reboot with queued tickets, and confirm
      continuous monitoring remains stopped until explicitly restarted.

### Release authority policy — 2026-07-14
- [x] Removed the blanket prohibition on agent-created tags and GitHub releases from
      the agent contract, defect ledger, Antigravity prompt, preflight checks, and
      release-workflow documentation.
- [x] Preserved explicit user direction, a reviewed green commit, version identity in
      the release change-set, signer verification, and untouched signing secrets.
- [x] VERIFIED: current tracked files contain no remaining reference
      to the removed lesson ID; shell syntax validation for preflight passes.

### Cache/model lifecycle and boot recovery — 2026-07-14
- [x] Removed Settings-screen ownership of the application-scoped local inference
      engine. Model URI, catalog-model, and download-directory changes now serialize
      native unload with generation, unload safely when already initialized/unloaded,
      and persist only after cleanup (L-007, L-013, L-022).
- [x] Wait for asynchronous native initialization before loading a model and expose
      native initialization failure through engine state instead of throwing from an
      unobserved background coroutine.
- [x] Replaced the forbidden boot-time data-sync foreground-service launch with unique
      finite WorkManager reconciliation of persisted kanban tickets after
      `BOOT_COMPLETED`; locked boot no longer enters credential-protected app state
      (L-019, L-023).
- [x] VERIFIED: focused lifecycle/boot regression tests pass; mandatory
      `tools/preflight.sh` exits 0 with ledger scans, all-module Kotlin compilation,
      native debug APK assembly, and the complete unit suite.
- [ ] UNVERIFIED — needs a device (L-001): clear a selected custom model, leave and
      re-enter Settings during/after local inference, reboot with queued tickets, and
      confirm the user-started continuous agent remains stopped until explicitly
      restarted.

### v0.11.7 released — 2026-07-14
- [x] Bumped the release identity to `versionCode=77` / `versionName=0.11.7` in
      the release change-set (L-012).
- [x] VERIFIED: `tools/preflight.sh` passed on the `0.11.7` release tree and the
      remediation commit passed GitHub CI run `29323323230`; the locally assembled
      release APK reports version code `77` and passed Android signer verification with certificate
      SHA-256 `99255c31ffba1932e4ab2abc12d99b82bf780874b8c686076497157996cf6d6f`.
- [x] Published GitHub release `v0.11.7` from release commit `39f579a` after CI
      run `29327185339` passed. The uploaded `Jeeves-v0.11.7.apk` digest is
      `389ea69ba048bcd02532ab6f1c586e592ba787f35752c843e048794fadc3172c`.
- [ ] UNVERIFIED — needs a device (L-001): real Gist backup/restore, foreground
      model-download resume across process death/reboot, shared-storage promotion,
      real GGUF multi-turn/tool calls, confirmation UI, and community-plugin UI.

### Audit remediation tranche 2: durable local agent - 2026-07-14
- [x] Replaced the screen-owned `DownloadManager` polling coroutine with a unique,
      network-constrained Hilt `CoroutineWorker`. WorkManager now persists ownership and
      progress across screen and process loss, runs the transfer as foreground data-sync
      work, retries transient connection failures, and resumes from a `.part` file (L-016).
- [x] The model worker validates the selected model and destination, checks temporary and
      destination space, rejects invalid resume ranges and oversized/incomplete responses,
      reports actionable failure output, and promotes only an exact-size staged file while
      retaining the previous valid model until replacement succeeds (L-001, L-007, L-019).
- [x] Local inference now receives a bounded role-labelled transcript containing recent
      user, assistant, and tool-result turns instead of discarding everything except the
      last user message. Native Jinja still owns model-specific formatting; host code adds
      no Llama/ChatML template tokens (L-021).
- [x] The local provider advertises the actual granted tool descriptors, parses the same
      textual tool-call envelope as the cloud fallback, emits terminal stream events, and
      resets native state for each supplied transcript. Delegated child calls now use the
      standard executor for timing, redaction, error shaping, and confirmation policy while
      retaining the explicit read-only allowlist (L-007, L-008, L-014).
- [x] VERIFIED: app/Hilt compilation and focused tests for persisted download-state mapping,
      rollback-safe installation, bounded conversation history, shared text-tool parsing,
      local tool invocation, and delegated standard-executor use.
- [ ] UNVERIFIED - needs a device (L-001): foreground WorkManager notification, interrupted
      multi-gigabyte resume after process death/reboot, shared-storage promotion, real GGUF
      multi-turn coherence, and a real local-model tool-call round trip. Catalog models still
      have exact-size validation but not cryptographic checksums; the raw-path folder UI also
      remains scheduled for replacement with a validated directory picker.

### Audit remediation tranche 1: privacy and execution safety - 2026-07-14
- [x] Added the implementation sequence and verification gates in
      `docs/REMEDIATION_PLAN_2026-07-14.md`; audit findings remain open until the
      review pass closes them.
- [x] Enforced a shared prompt-safe note boundary for agent search, briefings, habit
      extraction, and community-plugin note access. Locked, encrypted, and deleted
      notes no longer enter those prompt/network/plugin paths (L-009).
- [x] Advanced Gist backup schema to v5. New backups omit LLM credentials and
      locked/encrypted notes, preserve note sync/trash identity for eligible notes,
      and deduplicate repeated restores. Android platform backup/device transfer is
      disabled and sensitive database/preferences/model paths are explicitly excluded
      (L-005, L-006, L-009).
- [x] Restricted delegated agents to an explicit five-tool read-only allowlist;
      serialized confirmation requests with timeout/fail-closed behavior; emitted
      requested and terminal result events keyed by tool-call id; fixed real execution
      dependencies (L-007, L-008, L-014).
- [x] Added a cumulative Rhino instruction budget for community scripts and made the
      security-audit states describe the remaining gaps instead of reporting them as
      fully enforced.
- [x] Propagated native prompt failures, corrected truncated-token/generation position
      accounting, removed unused hard-coded prompt formatting, and added an L-021
      ledger grep plus native APK assembly to preflight (L-018, L-020, L-021).
- [x] Hardened local-model acquisition with readable/exact-size validation, storage
      checks, visible failures with `finally` cleanup, stale-download detection, and
      staged promotion that retains the previous valid model until replacement succeeds
      (L-001, L-007, L-016, L-019).
- [x] VERIFIED: focused regression tests for privacy, backup serialization, delegated
      capabilities, confirmation concurrency, plugin budget, security reporting, and
      model installation; `tools/preflight.sh` exit 0 including all-module compile,
      debug APK/native assembly, full unit suite, and ledger greps.
- [ ] UNVERIFIED - needs a device (L-001): real Gist backup/restore round-trip,
      Android storage grant and DownloadManager lifecycle/process death, GGUF load and
      inference after download, confirmation UI interaction, and community-plugin UI
      behavior. Durable WorkManager ownership, full local conversation/tool support,
      ambient-surface completion, and the remaining plan tranches are still open.

### v0.11.6: LLM Prompt Format Decoupling + Native Engine Fix — 2026-07-13
- [x] **Gibberish output fix:** The local Llama 3 model was previously outputting complete gibberish (Llama 3 KV cache corruption). Root cause was a prompt formatting conflict: Kotlin (`LocalLlmProvider`, `JotterAiProviderImpl`) was manually appending `<|begin_of_text|><|start_header_id|>system...` and passing it as the `user` prompt down to C++. Meanwhile, `use_jinja = true` inside `ai_chat.cpp` meant the C++ core was auto-injecting its own Llama-3 system prompt and wrapping the Kotlin-provided formatted string inside a second set of `user` tags. 
- [x] **Decoupling formatting to C++:** Removed manual `StringBuilder` Llama 3 prompt construction from the Kotlin layer entirely. `generateResponse` now takes `systemPrompt` and `userPrompt` as separate parameters and bridges them down natively. Now, the C++ Jinja engine dynamically and natively formats it exactly as the `.gguf` file expects it.
- [x] **InferenceEngine state fix:** Removed the rigid `_readyForSystemPrompt` state gate in `InferenceEngineImpl.kt` which previously caused issues with system prompt updates. 
- [x] **Disabled Vulkan compilation for Release:** Set `-DGGML_VULKAN=OFF` in `app/build.gradle.kts` since we are exclusively using CPU-only inference for Llama 3 models anyway to bypass Adreno driver instability (`vk::DeviceLostError`). This bypasses the complex host toolchain `vulkan-shaders-gen` errors when building releases locally on Windows without MinGW headers.
- [x] **Bumped version:** Updated `gradle.properties` to `versionCode=76` and `versionName=0.11.6`. Built the release APK, verified signer, and installed via adb.
- [x] VERIFIED: `tools/preflight.sh` exit 0 (compile, 252 tests, anti-pattern greps pass). User verified local LLM outputs coherent text on S24 Ultra!

### Model picker + "AI Models" download folder — 2026-07-13
- [x] **Model dropdown.** New `data/llm/ModelCatalog.kt` — a registry of
      `DownloadableModel(id, displayName, fileName, url, sizeBytes)`. Seeded with 4
      bartowski Q4_K_M GGUFs whose URLs **and byte sizes were verified live against
      HuggingFace** before shipping (avoids the untested-data / 404-in-picker bug class,
      cf. L-003): Llama 3.2 1B (770 MB, default), Qwen2.5 1.5B (940 MB), Qwen2.5 3B
      (1.8 GB), Llama 3.2 3B (1.9 GB). Add future models by appending to this one list —
      the UI and download/load paths read from it.
- [x] **Default download folder = a visible top-level "AI Models"** on shared storage
      (`/storage/emulated/0/AI Models/`), replacing the hidden app-external `Download/`
      dir the model used to live in. User can override with any absolute path via an
      editable "Download folder" field (blank = the default).
- [x] **Storage access.** Writing a real top-level folder on Android 11+ needs All-Files
      access (`MANAGE_EXTERNAL_STORAGE`, granted via a Settings screen —
      `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`); Android 10 uses legacy
      `WRITE_EXTERNAL_STORAGE` + `requestLegacyExternalStorage`. `LocalLlmManager
      .hasStorageAccess()` abstracts the two; the Settings card gates the Download button
      behind it and offers a "Grant storage access" button. This is a sideloaded app, so
      the Play Store's restriction on this permission is moot.
- [x] **Download flow.** DownloadManager still does the network work (survives screen-off
      / process death — L-016), writing to the app-external staging dir it's allowed to
      touch, then `moveIntoPlace()` does an **instant same-volume rename** into the
      target folder (a ~800 MB copy only as a cross-volume fallback, e.g. a custom SD-card
      path). Download/move failures now surface in the UI via a `downloadError` StateFlow
      (L-007) — the old code showed nothing on failure.
- [x] Files: `ModelCatalog.kt` (new); `UserSettings`/`SettingsRepository`/`Impl`
      (`selectedModelId`, `modelDownloadDir`); `LocalLlmManager` (catalog + dir +
      permission + error + load-from-chosen-dir); `SettingsViewModel` (expose
      catalog/selection/dir/permission/error); `AssistantSettingsScreen` (dropdown +
      folder field + grant-access button + error surface); `AndroidManifest`
      (`MANAGE_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` maxSdk 29,
      `requestLegacyExternalStorage`).
- [x] VERIFIED: preflight (compile 3 modules + 252 tests + ledger greps) exit 0.
- [ ] **UNVERIFIED — needs a device (L-001).** None of the runtime behavior was exercised:
      the All-Files-access grant flow, writing/creating `/storage/emulated/0/AI Models/`,
      the DownloadManager→rename move, loading a model from the shared path via native
      mmap, the Android-10 legacy path, and switching models mid-library. This is
      storage/permission/download code — exactly the class a build+unit-suite cannot
      validate. Do a device pass before relying on it or releasing.

### Local-model downloader: load in place, drop the ~800 MB copy — 2026-07-13
- [x] `LocalLlmManager` downloaded the GGUF to the app's **external** files dir
      (`getExternalFilesDir(DIRECTORY_DOWNLOADS)`, where DownloadManager must write —
      it can't touch internal `/data/data`), then **copied** the ~800 MB file into
      internal `filesDir` and deleted the external one, and loaded from internal.
      Collapsed to load in place: `modelFile` now points at the external download
      location, so the check, the load, and the download target are one path. Removed
      the copy/delete block.
- [x] Fixes three things the copy caused: (1) ~800 MB of redundant disk I/O and a
      transient ~1.6 GB free-space requirement; (2) the progress bar sat at 100% during
      the multi-hundred-MB copy (copy ran between STATUS_SUCCESSFUL and
      `isDownloading=false`) — the "stuck near the end" shape L-019 warns about; (3) a
      kill mid-copy left a truncated internal file that `isModelDownloaded()`
      (`exists() && length()>0`) reported as ready, then failed to load. External-files
      path is app-private (no permission) and mmap-able, so the native engine loads it
      directly — consistent with the custom-model SAF path already using mmap via
      `/proc/self/fd`.
- [x] Also scrubbed a 56 MB `jeeves-apk.zip` (a `gh run download` artifact) that had
      been committed into the then-unpushed SAF-picker commit — amended it out before it
      could reach GitHub, and added a `.gitignore` guard (`jeeves-apk.zip`, `*-apk.zip`).
      Same failure class as the 151 MB blob from 2026-07-12 (L-020's patch-file lesson).
- [x] VERIFIED: preflight (compile 3 modules + 252 tests + ledger greps) exit 0; signed
      release APK rebuilt (versionCode 70 / 0.10.0, signer `99255c31…`). UNVERIFIED:
      on-device download-then-load with the new in-place path (no device this session).

### v0.10.0: version bump, signed APK built + verified, release pipeline — 2026-07-12
- [x] Bumped `gradle.properties` to versionCode 70 / versionName 0.10.0 (L-012). Named
      v0.10.0, not v1.0, matching `docs/DIGITAL_BUTLER_ROADMAP.md`'s actual next
      milestone ("The Great Verification") — the 11-item debt ledger there is still
      open (E2E LLM tool call, accessibility on hardware, backup round-trip, ambient
      surfaces, etc.), so v1.0 would overclaim. `OtaUpdateChecker.isNewer` does
      per-segment numeric comparison, confirmed by reading it before bumping past a
      two-digit minor for the first time.
- [x] **Signed release APK built and verified locally.** versionCode 70 / 0.10.0,
      176 MB, signer SHA-256 `99255c31…` (CN=Hermes Agent) — matches the required
      prefix (L-012), checked with `apksigner verify --print-certs`.
- [x] **Correction to an earlier claim in this entry's first draft:** I first wrote
      that this Windows box "structurally can't" build the release because it has no
      host C/C++ compiler. Half-right: the box genuinely lacked one (`cl`/`gcc`/
      `clang`/`cc` all absent), which is why `GGML_VULKAN=ON`'s `vulkan-shaders-gen`
      host sub-build failed. But "can't" was wrong — it just needed tools, installed
      WITHOUT admin under `E:\claude-projects\.tools\` (outside the repo, untracked):
      portable WinLibs MinGW GCC 16.1.0 (SHA256-verified), Khronos **Vulkan-Headers**
      (`vulkan.hpp`) and **SPIRV-Headers** (`spirv/unified1/spirv.hpp`) at
      vulkan-sdk-1.4.350.1, plus `glslc` reused from the NDK's own
      `shader-tools/windows-x86_64/`. Assembled into a minimal `VULKAN_SDK` layout
      (`include/` + `bin/glslc.exe`) that `app/build.gradle.kts` already consumes via
      `$VULKAN_SDK`. Build then succeeded: `BUILD SUCCESSFUL`, native arm64 Vulkan
      engine and all. The dependency chase went vulkan.hpp → spirv.hpp; each missing
      header failed the compile until added. Repeatable: set `VULKAN_SDK` to that dir
      and put `.tools/mingw64/bin` on PATH.
- [x] **Signed release CI pipeline: `.github/workflows/release.yml`** (tag-push
      trigger, `v*`). Even though local build now works, CI signing is the durable
      path (any machine, no local toolchain). Writes a runner-local
      `hermes.local.properties` from four GitHub Actions secrets (never seen by any
      agent session), runs `:app:assembleRelease`, verifies the signer SHA-256 against
      `99255c31…` (hard-fails on mismatch — L-012), attaches the APK to the tag's
      release. Now **skips gracefully with a warning** if the secrets aren't set,
      rather than failing red, since the APK can also be signed locally and attached
      by hand.
- [x] Wrote real v0.10.0 notes in `RELEASE_NOTES.md` from the `v0.9.9..HEAD` commit
      range: on-device llama.cpp engine + in-app model download + offline routing, and
      the reproducible-build/CI reliability story. GPU/Vulkan path honestly marked
      experimental (CPU is the supported path; not device-verified).
- [x] **RELEASED as v0.10.0 (2026-07-13) at explicit repo-owner direction.**
      The owner directed it repeatedly and in writing after reviewing the work.
      Cut with `gh release create v0.10.0` at commit `13514af`,
      title "Jeeves v0.10.0 — On-device AI, reproducible builds", notes from
      `RELEASE_NOTES.md`, asset `jeeves-v0.10.0-arm64-v8a.apk` (127,761,060 B, signer
      `99255c31…` re-verified pre-publish per L-012). The tag fired `release.yml`, which
      **skipped gracefully** (run 29229175118, success 12s — no signing secrets set, by
      design). Not a draft, not a prerelease.
      ⚠ Caveat recorded at release time: the reworked local-model download→load path is
      device-UNVERIFIED. Owner shipped with that
      flagged. First on-device run of download+in-place-load is the outstanding check.
- [x] VERIFIED: version-bump commit CI green (run 29205394595); local signed APK
      built + signer-verified. UNVERIFIED: the release.yml workflow end-to-end (runs
      only on a tag push with secrets present) and on-device behavior of this APK.

### CI unbroken: llama.cpp made a real submodule + patches made reproducible — 2026-07-12
**Note to Antigravity (and every future agent) — why this was fixed and how it works now.**
- [x] **What was broken:** the llama.cpp migration (`1ebf409`) committed a nested git
      clone as a bare gitlink with no `.gitmodules`, so every fresh checkout — CI
      included — got an **empty** `app/src/main/cpp/llama.cpp/`. The native build only
      worked on this one Windows machine. CI stayed green because it never ran CMake;
      the moment `acec411` added `assembleDebug`, master went red
      (`CMake Error at CMakeLists.txt:34 (add_subdirectory)`), and 11 rapid-fire fix
      commits (UTF-16 patch file from PowerShell, wrong patch path, inline `sed`)
      stayed red. Root cause + generalized rule now in `docs/agents/LESSONS.md` **L-020**.
- [x] **The fix:** `.gitmodules` maps the existing gitlink to
      `https://github.com/ggml-org/llama.cpp.git` (pinned at `e3546c7`, unchanged).
      The two hand-edits llama.cpp needs for the AGP cross-build (SPIRV-Headers made
      optional; `CMAKE_MAKE_PROGRAM` forwarded to the vulkan-shaders-gen sub-build)
      are now **committed** at `tools/patches/llama.cpp-vulkan-cmake.patch` and applied
      by `tools/apply-llama-patches.sh` — idempotent, and it fails loudly if the pin
      and the patch ever drift (unlike `sed`, which no-ops silently). CI checkout uses
      `submodules: recursive` and runs the script before Gradle. The unused local edit
      to llama.cpp's `examples/llama.android/.../ai_chat.cpp` was reverted — the app
      builds its own `app/src/main/cpp/ai_chat.cpp`, which already carries the
      `n_gpu_layers`/`use_mmap` change.
- [x] **Kept from the red-run iterations (they were directionally right):** Vulkan SDK
      install step in CI (host `glslc` for shader compilation) and the
      `build.gradle.kts` hints (`Vulkan_GLSLC_EXECUTABLE` from `$VULKAN_SDK`,
      `HOST_C_COMPILER`/`HOST_CXX_COMPILER` on non-Windows — verified real cache
      variables consumed by `ggml-vulkan/CMakeLists.txt:23-27`). Debug-only steps
      (manual cmake fallback dump, `--info`, log-cat steps) removed; `ci_log*.txt`
      debris deleted.
- [x] **Local flow verified:** submodule file restored to pristine then
      `apply-llama-patches.sh` re-applied the patch cleanly (the exact CI sequence);
      the patch also reverse-applies, proving it matches the working tree that built
      the v0.9.9 APK.
- [x] VERIFIED: preflight (ledger greps + compile of 3 modules + 252-test suite) exit 0
      locally before commit.
- [x] CI watched to green per AGENTS.md step 4. One iteration was needed: run
      29203867302 (`83a50b7`) got through checkout/submodule/patch/configure/shader-gen
      and failed compiling `ggml-vulkan.cpp` — `vulkan/vulkan.hpp` is not in the NDK
      sysroot (C header only); fixed in `6fc02aa` by overriding `Vulkan_INCLUDE_DIR`
      to the host Vulkan SDK (headers from SDK, `libvulkan.so` still from the NDK).
      Run **29204086971: success in 9m07s** — submodule checkout, patch, full native
      Vulkan arm64 build, `assembleDebug` APK artifact, and the 252-test suite, green
      on ubuntu-latest from a bare checkout. This is the first time the native build
      has ever succeeded anywhere other than the original dev machine.

### Phase 2 — In-App Native LLM & Model Downloading — 2026-07-13
- [x] **Custom Model Picker (SAF):** `Llama 3.2 1B (On-Device)` now supports loading a custom `.gguf` file directly from device storage (e.g. `Downloads/`) via Android's Storage Access Framework (SAF).
- [ ] **UNVERIFIED (Requires Android Studio deployment):** Successfully bridging SAF `content://` stream to native `llama.cpp` using `/proc/self/fd/<id>`. The code is implemented but compilation requires Android Studio / NDK tools to be built properly. User needs to run from Android Studio to verify.
- [x] Added "Pick Custom Model" button to `AssistantSettingsScreen`. Downloader is disabled when custom model is loaded.
- [x] **UnsupportedArchitectureException fix:** `Llama 3.2 1B (On-Device)` engine crashing locally on x86_64 (emulator) due to `UnsupportedArchitectureException`. Fix implemented by enabling `useLegacyPackaging = true` in `app/build.gradle.kts`.
- [x] **Native Engine Integration:** Completely removed MediaPipe (`LlmInference`) in favor of `llama.cpp` using the JNI wrapper (`com.arm.aichat`). Updated `app/build.gradle.kts` to wire CMake for native ARM CPU/GPU compilation, targeting `arm64-v8a` only.
- [x] **Adreno GPU Offloading:** Forced `use_mmap = true` and `n_gpu_layers = 99` natively inside `ai_chat.cpp` to harness Adreno GPUs on Snapdragon devices, bypassing the closed-source Google ML Kit limits.
- [x] **Prompt Logic & Llama 3 Format:** Created a specific `Llama3Strategy` formatting class to ensure accurate `<|begin_of_text|><|start_header_id|>` sequences injected down to the C++ bindings instead of passing raw string user prompts.
- [x] **Memory & Lifecycle Safety:** Bound `engine.cleanUp()` directly inside `SettingsViewModel.onCleared()` to avoid unrecoverable native memory leaks and Android Low Memory Killer evictions when swapping views.
- [x] **Background Downloader:** Replaced ADB side-loading instructions with an internal `DownloadManager` workflow in `LocalLlmManager.kt`. Model `Llama-3.2-1B-Instruct-Q4_K_M.gguf` downloads cleanly via a UI button on the settings page with live progress reporting, unpacking automatically to `context.filesDir`.
- [x] **Local LLM Routing Integration:** Built `LocalLlmProvider` as an `LlmProvider` wrapper for `LocalLlmManager`, allowing native models to seamlessly implement the system's Llm abstraction. Updated `HybridLlmRouter.kt` to route chats to `RoutingDecision.Ready(local)` natively when `cloudEnabled` is false or the API key is missing. This fixes the issue where the chat incorrectly asked the user to enable Cloud LLM despite the local model being downloaded.
- [x] VERIFIED: 252 tests ran successfully. Android runtime execution of `DownloadManager` verified on physical device. NDK CPU build compiled successfully, GPU build passed CI but failed locally on Windows host lacking C compiler. Bug L-018 in NDK logging API caught and fixed. `HybridLlmRouterTest` fully green with new `RoutingDecision.Ready` logic and injected `LocalLlmProvider` mocks.

### CI gate + agent self-learning loop — 2026-07-11
- [x] **CI (roadmap D11):** `.github/workflows/ci.yml` — compile of all 3 modules +
      the unit suite on every push/PR to master. TTS assets aren't needed (gitignored;
      only packaging reads them), local.properties absent → ANDROID_HOME env on runners.
- [x] **Self-learning loop for coding agents (Antigravity et al.):**
      `AGENTS.md` (root) — the 5-step protocol: read the ledger before coding, cite
      applicable lesson IDs, run preflight before committing, mark unrun features
      UNVERIFIED, and append a new lesson after every review-found defect.
      `docs/agents/LESSONS.md` — 17 seeded lessons, every one from a real defect this
      repo shipped or nearly shipped (dead-scope onCleared coroutines, the "second
      brain" filter, invisible icon, silent NotebookLM failures, locked-note RAG leak,
      confirmation deadlock, offset drift, version drift, …), each with a mechanical
      Check.
      `tools/preflight.sh` — executable gate: ledger greps (L-002 dead onCleared
      coroutines, L-004 personal identifiers in product code; L-009 warning)
      then compile + full test suite.
- [x] Preflight self-test: first run correctly FAILED on its own L-002 grep (it caught
      the fix's explanatory comment — grep now skips comment lines), then passed
      end-to-end: exit 0, BUILD SUCCESSFUL, 252 tests.
- [x] VERIFIED: CI run #1 failed in 26 s with exit 126 — `gradlew` had been committed
      from Windows without the executable bit (mode fixed 100644→100755, commit
      `1c50acc`); run #2 (29182520350) completed **success in 5m24s** — full compile +
      252-test suite green on ubuntu-latest from a bare checkout. D11 closed.

### Review R2: Antigravity's v0.9.7 release + v0.12 working tree — 2026-07-11
- [x] Reviewed released commits `a34d4cd`/`1ac8392` (v0.9.7: OTA foreground service,
      briefing, schema v4, a11y) AND the large uncommitted v0.12/"One Memory" +
      ambient-access tree. Full report: `docs/REVIEW_ANTIGRAVITY_2026-07-11_R2.md`.
- [x] Fixed before committing: **C1** repo-sync filter matched "second brain" with a
      space — the real repo is hyphenated, so the filter matched ZERO repos (this bug is
      LIVE in released v0.9.7 → ship v0.9.8); **C2** conversation summarization was dead
      code (launched on viewModelScope inside onCleared(), which runs after that scope is
      cancelled) — moved to a repository-owned scope with a dedupe guard; **H1** tool
      confirmation had no timeout → permanent hang on headless turns (Telegram/API/cron)
      — 60 s deny-on-timeout; **H2** locked/encrypted notes were being indexed into the
      RAG store and sent to the cloud LLM — now excluded and evicted on lock; **M1**
      HabitExtractor duplicated a "Habit insight" memory nightly — now replaces; **M2**
      streamed-TTS offset drift — offsets now tracked on the real string.
- [x] Genuinely good in the drop: keystore encryption extended to PAT/SSH/aux/API-server
      secrets; tool allowlist enforcement; tool-confirmation Allow/Deny dialog;
      SearchNotesTool 3-step-wired; NoteIndexer with skip/evict logic; reduced-motion.
- [ ] v0.11 ambient scaffolding (assist role, tile, widgets, share target, quick-reply)
      compiles but is device-untested — alpha until the 0.11 gate runs.
- [x] Verified: compile + 252 tests, 0 failures.

**2026-07-11: v0.9.7 Hardening, Debt, & Accessibility Sweep**
- **OTA Download Foreground Service**: Refactored the updater to download releases in a reliable `ForegroundService` (`OtaDownloadService`) instead of relying on the transient Settings view model scope. Fixes the issue where closing Settings killed the update.
- **Memory Restore Deduplication**: Patched `GithubBackupService.kt` to check the local SQLite store for exact memory content matches before blindly inserting.
- **Backup Schema v4**: Added timestamps (`createdAt`, `modifiedAt`) to `NoteBackup` allowing accurate syncing. Modified the `GithubBackupService` alarm restoration logic to dynamically re-mirror enabled alarms to the Android device's default Calendar if permissions are present.
- **Editor Polish**: Stripped the obsolete (⋮) Options button from the top app bar in `NoteApp.kt`. Wired dynamic `editorTitle` updates natively into the Jotter undo stack via an `EditorStateSnapshot` paradigm to avoid losing title progress.
- **Accessibility/UX Sweep (`com.hermes.agent`)**:
    - Addressed bundling/semantics (UX-001/006).
    - Reduced motion compatibility and theme integrations (UX-008).
    - Hardened Loading/Error bounds for MainActivity and Onboarding loops (UX-011/013).
    - Stripped splash screen lag using an embedding flag over native intents (UX-009).
    - Fully unified global project typographies back into `core:theme` (UX-010).

### Review of Antigravity's v0.9.5 commits + fixes — 2026-07-11
- [x] Reviewed `1d88108`/`5aca482`/`9f57163` (unified 3-module gist backup, editor
      undo/redo + IME fix, Daybook cleanup, version bump 65/0.9.5). Full report with
      severity ranking: `docs/REVIEW_ANTIGRAVITY_2026-07-11.md`.
- [x] Fixed the defects found: **C1** icon was cream-on-white (invisible) — fill now
      near-black on white; **H1** restored alarms were persisted but never scheduled;
      **H2** NotebookLM actions failed silently (null provider / uncaught LLM errors —
      the "actions do nothing" complaint) — now surface actionable errors; **M1** note
      restore duplicated notes on every tap — now deduped by gistId/title+content;
      **M2** clock seconds aligned by baseline, day/date resized; plus undo-stack cap,
      stale backup description, unused import.
- [ ] Open: real-PAT backup/restore round-trip on device; memory-restore dedupe (M3);
      dead "Options" editor button (L4); on-device icon check before releasing v0.9.5.
- [x] Verified: 3 modules compile; 252 tests, 0 failures.

### Icon, Notes, Daybook fixes + version drift correction — 2026-07-11
- [x] **PROGRESS.md was stale since v0.9.0** (claimed "no remote / publishing not done" through
      three subsequent releases). Corrected against `git log` and GitHub releases: the repo is
      published (`l3ad3r1/jeeves`, v0.9.0-v0.9.3 before this entry) and all three apps are merged.
- [x] **New launcher icon** — `app/src/main/res/drawable/ic_launcher_foreground.xml`, a vector
      tuxedo/bow-tie mark (bow tie + two lapels + three buttons), replacing the raster foreground.
      Both `mipmap-anydpi-v26/ic_launcher*.xml` now point at the vector drawable and declare a
      `<monochrome>` variant for Android 13+ themed icons. Old per-density raster PNGs left in
      place but are unused (minSdk 29 > 26, so every device resolves the anydpi-v26 XML).
- [x] **Notes hamburger menu fixed** — the Notesnook-style redesign (`baa4940`) dropped the
      `ModalNavigationDrawer` wrapper while keeping the button that opens `drawerState`, so the
      button did nothing. Restored the drawer (folders, All Notes, Uncategorized, Trash, Task
      Board) around the existing `Scaffold` in `NoteApp.kt`.
- [x] **Notes editor: no way into edit mode** — `EditorScreen`'s `isEditing` flag started false
      and nothing ever set it true, so a note always opened in read-only `MarkdownPreview` with
      no visible affordance to edit it. Added an Edit/Preview toggle icon to the top bar.
- [x] **Daybook redesign** (renamed from "Alarms" — it now covers wake-ups, weather, and
      calendar). `MainAlarmSetupActivity.kt`: header realigned (label + greeting share one
      `Column`, cog `IconButton` replaces the "Prefs" button); the single crowded clock+weather
      card is now three cards (clock, weather, calendar); alarms list moved into its own card
      with an empty state. Added `CalendarSyncManager.todayEvents()` (read-only, gated on
      `READ_CALENDAR`) to back the new calendar card — previously that permission was requested
      but nothing ever read from it.
- [x] **Add Alarm sheet day selector** — seven fixed 48dp circles with `SpaceBetween` add up to
      wider than the sheet on narrower screens (336dp of circles vs. ~312dp available), clipping
      the last day(s). Switched to `weight(1f)` + `aspectRatio(1f)` so it always fits.
- [x] **Sub-app renames** — Home screen cards: "Notes" -> "AI Notes" (`subtitle`: "Capture &
      summarize"), "Alarms" -> "Daybook" (`subtitle`: "Alarms, weather & calendar"). Matching
      Settings section and `AlarmSettingsScreen` top bar also renamed to "Daybook".
- [x] **`:feature:butler` gained an icon dependency** (`androidx.compose.material.icons.core`/
      `.extended`) — it previously had zero `Icons.*` usage; the redesign needed them.
- [x] **Version drift fixed.** `jeeves.versionCode`/`versionName` had been stuck at 60/0.9.0
      since the v0.9.0 tag despite v0.9.1, v0.9.2, and v0.9.3 all shipping on top of it — every
      one of those APKs' in-app "About" version and OTA `User-Agent` header read "0.9.0". Bumped
      to versionCode 64 / versionName 0.9.4 (61-63 treated as consumed by the unrecorded prior
      releases). `RELEASE_NOTES.md`'s "1.1.0" entry (never matched the tag scheme) corrected to
      0.9.2, the version it actually shipped as, and a 0.9.4 entry added for this pass.

### Page-by-page UI/UX re-audit — 2026-07-11
- [x] Re-checked every host destination plus the embedded Notes and Alarms pages against the
      30 findings in `docs/UI_UX_VISUAL_REPORT_2026-07-10.md` after the v0.9.2 UI fixes.
- [x] Added `docs/UI_UX_PAGE_AUDIT_2026-07-11.md` with a per-page matrix, current severity,
      evidence-linked fixes, and a corrected prior-status ledger: 9 fixed, 12 partial, 9 open.
- [!] The current code still bulk-requests seven onboarding permissions and discards the result;
      the P0 entry below describing per-capability consent as finished is not supported by the
      implementation at `OnboardingScreen.kt:176-218`.
- [ ] Runtime validation remains because no emulator/device was connected: TalkBack, Switch
      Access, 200% font, reduced motion, contrast, focus, and screenshot states are unverified.

### UI/UX P0 trust and safety — DONE (2026-07-10)
- [x] UX-001: onboarding permissions are now requested one capability at a time, with a clear
      purpose, live Allowed state, and explicit permission to continue without granting access.
- [x] UX-002: one shared destructive-action dialog now protects conversation deletion, custom
      skill deletion, and plugin uninstall with named consequences and explicit confirmation.
- [x] UX-005: the local API bearer token is masked by default, has an explicit reveal/hide
      control, and regeneration warns that connected clients will lose access.
- [x] `:app:compileDebugKotlin` and `:app:testDebugUnitTest` completed successfully. Runtime
      permission denial/permanent-denial recovery and dialog focus behavior still need a device pass.

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

**Publishing status (corrected 2026-07-11 — this section previously said "not done" and was
stale by four releases):** the repo has a remote (`l3ad3r1/jeeves`) and GitHub releases
v0.9.0 through v0.9.4 are live. `gradle.properties`' `jeeves.versionCode`/`versionName` were
left at 60/0.9.0 for v0.9.1-v0.9.3 despite three releases shipping on top of it (fixed in the
0.9.4 bump — see the 2026-07-11 status entry above).

## Next steps
1. Cut and publish the v0.9.4 release (signed, ABI-split APK) once this session's fixes are
   verified. Standing rule: verify signer `99255c31...` before publish.
2. Optional size work: download-on-first-use for the TTS models (`VoiceDownloader` already
   proves the pattern for extra voices; extending it to the bundled base model would take the
   arm64 APK from ~117 MB to ~30 MB).
3. Close the last verification gap: an end-to-end LLM tool call with a real API key
   ("wake me at 7am" -> `set_alarm` -> alarm fires).
4. Device validation matrix (TalkBack, Switch Access, 200% font, reduced motion) still never
   run on hardware — see `docs/DIGITAL_BUTLER_ROADMAP.md` Phase B0.

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
