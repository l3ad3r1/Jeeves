# Build & Run

## Prerequisites

| Tool                     | Version              | Notes                                              |
|--------------------------|----------------------|----------------------------------------------------|
| JDK                      | 17                   | Required by AGP 8.x. Use `java -version` to check. |
| Android SDK              | Platform 34 + build-tools 34.0.0 | Android Studio Hedgehog (or newer) bundles both. |
| Android Studio (optional)| Hedgehog 2023.1+     | Recommended IDE; also works pure CLI.              |
| Gradle                   | 9.6.1 (auto via wrapper) | Don't use a system Gradle; the wrapper pins the version. |
| Kotlin                   | 2.2.10               | Bundled via AGP 9's built-in Kotlin support.       |

> **Check out the shared engine too.** The `:core:*` Gradle projects are not in
> this repository — they live in
> [`l3ad3r1/agent-core`](https://github.com/l3ad3r1/agent-core) and are mapped
> in by `settings.gradle.kts`. Without it, Gradle fails during settings
> evaluation before it compiles a line. Clone it beside this checkout:
>
> ```bash
> git clone https://github.com/l3ad3r1/agent-core.git ../agent-core
> ```
>
> Any other location works via `-PagentCoreDir=<path>` or the
> `AGENT_CORE_DIR` environment variable. `agent-core.ref` records the engine
> commit CI builds against — bump it in the same commit as any app change that
> needs a newer engine.
>
> `:core:jeeves-settings` and `:core:jeeves-theme` are Jeeves-only and do live
> in this repository, under `core/`.

Minimum runtime device: **Android 10 (API 29)**. The app installs and runs on
any Android 10+ device for development.

---

## 1. Open in Android Studio (recommended)

1. `File → Open…` and select the `jeeves/` directory.
2. When prompted, accept the suggested Gradle sync.
3. Wait for indexing and Gradle sync to complete (first run downloads
   dependencies; expect 2–5 minutes on a fresh machine).
4. Select a device or emulator (`API 29+`, ideally `API 34`).
5. Click ▶ Run 'app'.

## 2. Build from the command line

```bash
# Debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (requires signing config; see "Release builds" below)
./gradlew assembleRelease

# Install on a connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires a connected device or emulator)
./gradlew connectedAndroidTest

# Lint + checks
./gradlew lint
```

If you don't have the Android SDK configured via `ANDROID_HOME`, create a
`local.properties` file at the repo root with:

```
sdk.dir=/path/to/Android/Sdk
```

(This file is gitignored.)

---

## 3. Plugging in a real cloud LLM

The cloud provider is wired for any OpenAI-compatible endpoint. Three
parameters configure it: **API key**, **base URL**, and **model name**.

### Option A — Build-time (recommended for CI / shared dev devices)

Create `hermes.local.properties` at the repo root (gitignored):

```properties
hermes.cloudApiKey=sk-your-openai-key-here
hermes.cloudBaseUrl=https://api.openai.com/v1
hermes.cloudModel=gpt-4o-mini
```

These are read by `app/build.gradle.kts` and surfaced as
`BuildConfig.CLOUD_API_KEY`, `CLOUD_BASE_URL`, `CLOUD_MODEL`.

### Option B — Runtime (recommended for personal devices)

Run the app, open **Settings → Cloud LLM**, toggle **Cloud fallback** on,
and paste your API key. The value is persisted in DataStore; nothing is
checked into version control.

### Supported backends

Any endpoint that implements the OpenAI `/v1/chat/completions` contract
works. Tested configurations:

| Backend              | Base URL                              | Model example              |
|----------------------|---------------------------------------|----------------------------|
| OpenAI               | `https://api.openai.com/v1`           | `gpt-4o-mini`              |
| Azure OpenAI         | `https://{resource}.openai.azure.com/openai/deployments/{deployment}` | `gpt-4` (deployment name) |
| Together AI          | `https://api.together.xyz/v1`         | `meta-llama/Llama-3-8B-chat-hf` |
| Anyscale             | `https://api.endpoints.anyscale.com/v1` | `meta-llama/Meta-Llama-3-8B-Instruct` |
| vLLM (self-hosted)   | `http://your-host:8000/v1`            | any served model           |
| Ollama               | `http://localhost:11434/v1`           | `llama3`                   |
| llama.cpp server     | `http://your-host:8080/v1`            | any served model           |

When pointing at a self-hosted endpoint, use the device's actual IP (or
`10.0.2.2` for the Android emulator's host loopback).

---

## 4. Plugging in a real on-device LLM (Phase 2)

Phase 1 ships a mock on-device provider. To swap in MLC-LLM:

1. Add the MLC-LLM Android dependency to `app/build.gradle.kts`:
   ```kotlin
   implementation("ai.mlc:mlc-llm-android:0.1.0")
   ```
2. Replace the body of `OnDeviceLlmProvider.complete` / `stream` with
   calls into the MLC-LLM runtime. The public `LlmProvider` contract
   stays the same — no other code changes are needed.
3. Bundle a 4-bit quantized model (Hermes-3-8B-q4f16, Phi-3-mini-q4f16,
   or Llama-3-8B-q4f16) under `app/src/main/assets/models/` and load it
   via the MLC-LLM `ModelPath` API.
4. For NPU acceleration, register the Qualcomm AI Engine Direct delegate
   when constructing the MLC-LLM `LLM` instance.

See `docs/ARCHITECTURE.md` § 7 for the diagram of the swap.

---

## 4a. GPU offload via OpenCL (opt-in, unverified on device)

Decoding runs on the CPU. `GGML_VULKAN` is off because Vulkan offload triggered
`vk::DeviceLostError` (TDR) on Adreno, and `n_gpu_layers` follows whatever
backends actually registered — with no GPU backend built in, that is 0 and
nothing changes.

llama.cpp's **OpenCL** backend is the more promising route on Adreno: upstream
lists Adreno 750 (Snapdragon 8 Gen 3) as verified, and it supports Q4_K, so the
existing Q4_K_M catalogue works unchanged. It is wired up but **off unless
`OPENCL_SDK` is set**, and it has not yet run on real Adreno hardware.

### Providing an SDK

The NDK sysroot ships neither the CL headers nor a `libOpenCL.so` to link
against, so both are pointed at explicitly. Expected layout:

```
$OPENCL_SDK/
  include/CL/*.h                 # github.com/KhronosGroup/OpenCL-Headers
  lib/arm64-v8a/libOpenCL.so     # github.com/KhronosGroup/OpenCL-ICD-Loader,
                                 # built with the NDK toolchain for arm64-v8a
```

Build the loader with the NDK toolchain file, `-DANDROID_ABI=arm64-v8a` and
`-DANDROID_PLATFORM=24`; see `app/src/main/cpp/llama.cpp/docs/backend/OPENCL.md`
for upstream's version of these steps. A host Python 3 is also required — the
Adreno kernels are embedded into the backend at build time.

That `libOpenCL.so` is a **link-time stub only** and is not packaged into the
APK. On device the loader resolves the soname to the vendor's own
`/vendor/lib64/libOpenCL.so`, which is the real Adreno driver. Confirm it is
exported to apps before relying on this:

```bash
adb shell grep -r OpenCL /vendor/etc/public.libraries.txt
```

### Building and verifying

```bash
OPENCL_SDK=/path/to/opencl-sdk ./gradlew :app:assembleDebug
```

An extra `libggml-opencl.so` in the APK's `lib/arm64-v8a/` means the backend
compiled. To confirm it is live rather than merely present, check the load line
at model load — it reports the registered backends and the layer count:

```bash
adb logcat -s ai_chat | grep 'offloading'
```

`backends=[CPU], offloading 0 layers` means the backend did not register and you
are still on the CPU.

### Failure modes

Building this in is safe on non-Adreno hardware. The backend is a separate
`.so` loaded through `GGML_BACKEND_DL`, and `ggml_backend_load_best()` skips one
it cannot load instead of failing, so devices without a usable driver fall back
to the CPU — silently, since release builds define `NDEBUG`.

What that does **not** cover is a driver that loads and then faults mid-matmul,
which is exactly how the Vulkan attempt died. Two things to re-test if offload
goes live: `n_ubatch` (uncapped from 64 back to `BATCH_SIZE` once decoding moved
to the CPU — the 64 was an Adreno TDR workaround) and sustained multi-turn
generation, not just a single reply.

A Hexagon NPU backend (`ggml/src/ggml-hexagon`) also exists in-tree and is
faster still, but it is marked experimental, is Q4_0-only — which would mean
re-quantising the catalogue — and needs the Hexagon SDK at build time.

---

## 5. Release builds

Release builds need a signing key. Generate one (one-time):

```bash
keytool -genkeypair -v \
  -keystore hermes-release.jks \
  -alias hermes-release \
  -keyalg RSA -keysize 4096 \
  -validity 10000
```

Then add to `hermes.local.properties`:

```properties
hermes.signing.storeFile=/absolute/path/to/hermes-release.jks
hermes.signing.storePassword=...
hermes.signing.keyAlias=hermes-release
hermes.signing.keyPassword=...
```

And uncomment the `signingConfigs` block in `app/build.gradle.kts` (a
stub is left there for this purpose). Then:

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## 6. Troubleshooting

| Symptom                                                       | Likely cause                                                       | Fix                                                                            |
|---------------------------------------------------------------|--------------------------------------------------------------------|--------------------------------------------------------------------------------|
| `SDK location not found`                                      | `local.properties` missing or `sdk.dir` wrong                      | Create `local.properties` with `sdk.dir=/path/to/Android/Sdk`                |
| `Failed to transform kotlin-stdlib`                           | JDK 8 or 11 in use                                                 | Set `org.gradle.java.home` in `gradle.properties` to a JDK 17 path           |
| Hilt generates `unresolved reference: HiltAndroidApp`         | KSP not picking up Hilt                                            | Verify `ksp(libs.hilt.compiler)` is present in `app/build.gradle.kts`        |
| Cloud calls fail with `401 Unauthorized`                      | API key missing or wrong                                           | Check `Settings → Cloud LLM → API key` or `hermes.local.properties`          |
| Cloud calls fail with `Connection refused` on emulator        | Emulator can't reach your host                                     | Use `10.0.2.2` instead of `localhost` in the base URL                        |
| `OnDeviceLlmProvider` always returns canned replies           | Expected — Phase 1 mock                                            | See "Plugging in a real on-device LLM" above                                  |
| WorkManager crashes on launch                                 | `HiltWorkerFactory` not wired                                      | `HermesApp` must implement `Configuration.Provider` (it does in this repo)   |

---

## 7. CI sketch (optional)

A minimal GitHub Actions workflow for this project:

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: android-actions/setup-android@v3
      - run: ./gradlew assembleDebug test
```

Add `hermes.cloudApiKey` as a repository secret and inject it via
`-Phermes.cloudApiKey=$CLOUD_KEY` if you want CI to build a fully-wired
debug APK.
