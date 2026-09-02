# Known bugs and limitations — Jeeves

Jeeves shares the `agent-core` engine with the public Hermes app, so most engine
issues apply to both. This file is the Jeeves-side summary.

Last reviewed: **2026-09-03 (v0.17.3)**.

## Open

- **K21 — a turn cancelled by `ChatViewModel` being cleared is lost silently.**
  `sendMessage` launches the orchestrator in `viewModelScope`; navigating away
  mid-turn cancels it. The user message is persisted, no reply/error ever
  arrives. [issue #1](https://github.com/l3ad3r1/Jeeves/issues/1) ·
  Hermes [#13](https://github.com/l3ad3r1/Hermes-Agent-Android/issues/13).
- **K18 — Shizuku is unusable on Android 16** (upstream `AbstractMethodError` on
  API 36). The privileged-shell path stays unavailable there; the Companion-apps
  card still offers the F-Droid install for older devices.
  [issue #2](https://github.com/l3ad3r1/Jeeves/issues/2).
- **K04 — `RepeatedExecutionGuard` cannot detect repeats, by design** — its
  fingerprint includes tool output and create-style tools return a fresh id per
  call.

## Current limitations

- The on-device model is a final fallback, not selected ahead of an available
  cloud provider for structured tool tasks.
- Retrieval embeddings are SHA-256 hash vectors and the vector index is in-memory
  (rebuilt from Room each cold start). Same `agent-core` gap tracked on the Hermes
  repo as issues #3 and #4.
- Cloud-provider health is evaluated per request; no persistent cross-session
  score yet.
- Screen automation and app launching require the accessibility service and stay
  interactive even in trusted background mode.
- Shell and Termux commands always require biometric or device-PIN approval.
- Certificate pinning is not applied (the cloud endpoint is user-configurable);
  TLS is still enforced everywhere.
- The release build needs the Vulkan + MinGW host toolchain on `PATH`
  (`JAVA_HOME`=JBR, `ANDROID_HOME`, `VULKAN_SDK`, `mingw64/bin`) or the
  `vulkan-shaders-gen` host tool fails.

## Fixed in 0.17.x

- **Wake word removed entirely.** The KWS engine crash-looped the app on
  Android 14+ (a `FOREGROUND_SERVICE_TYPE_MICROPHONE` service started without
  `RECORD_AUDIO`) and mis-reported Bluetooth routing. The whole path — engine,
  foreground service, boot receiver, `FOREGROUND_SERVICE_MICROPHONE` permission —
  was deleted. Hands-free use is the manually-opened Talk mode.
- **Permissions screen was misreporting grants.** The About screen trusted
  `PackageInfo.requestedPermissionsFlags`, which does not reflect special-access
  grants ("All files access" showed *Not granted* while granted). It now checks
  the real platform API per permission and renders each as a live toggle.
- **Controls clipped under large system fonts** on the Logs, Experiment and Usage
  screens — now `FlowRow` + stacked fields.
- **Send button changed shape mid-stream** — now one stable control.
- **Samsung Knox row deleted** — Phase-1 stub that always returned false.
- **Standing-instruction block was computed and never concatenated** into the
  system prompt (caught by `OpenClawWiringTest`).

## Reporting a new issue

Include the app version, Android version/device, whether the action was
interactive or background, the selected provider/model, and a redacted log
excerpt. Never attach API keys, tokens, notes, calendar contents, or personal
data.
