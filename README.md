# Jeeves — AI Assistant & Butler

**Jeeves** is a private Android super-app: a proactive productivity suite and a
conversational agent in one APK. It merges the Hermes agent engine with a
morning-alarm butler and a Markdown notebook, routes each turn to the best
available model (cloud-first, on-device GGUF fallback), and keeps every secret
in the Android Keystore.

> **Status — v0.17.3 (2026-09-03).** Signed release APKs are attached to each
> [GitHub release](https://github.com/l3ad3r1/Jeeves/releases). Jeeves shares its
> engine with the public **Hermes** app through the
> [`agent-core`](https://github.com/l3ad3r1/agent-core) library (pinned per build
> in `agent-core.ref`); it keeps its own `com.jeeves.app` identity, branding, and
> release signing so it installs alongside a standalone Hermes.

---

## What's in the app

### Agent core (shared with Hermes)
- **Model routing** — `HybridLlmRouter` ranks configured cloud providers by
  quality/cost/latency, fails over in order, then falls back to an on-device
  Llama 3.2 1B (or any `.gguf` you supply) via a pinned `llama.cpp` submodule.
- **Multi-agent orchestration** — five roles (Conversational, Productivity,
  Research, Device control, Creative), plan-then-execute with a per-step
  tool-call loop; deterministic phone commands skip the LLM.
- **~50 tools** behind two gates (per-role grants + a runtime allow/confirm/deny
  policy): calendar, alarms, communication, media, navigation, device settings,
  camera, Home Assistant, web search/fetch, file read/write/patch, shell + Termux
  (biometric-gated), accessibility screen automation, Kanban, memory, skills,
  delegation.
- **Memory & RAG** — sliding-window + long-term semantic store (vector + BM25),
  daily fact consolidation while charging, document indexing.
- **Plugins** — in-app JavaScript plugins (`ScriptPluginEngine`, Room-backed),
  first-party native plugins, and SHA-256-pinned community modules over HTTPS.
- **Gateways** — Telegram, Discord, Signal, WhatsApp, webhooks, and a local API
  server other apps on your network can call.
- **Proactivity** — background heartbeat runs standing orders on a schedule,
  ambient presence beacon (labelled places only, coordinate discarded), digest
  and nudges with quiet hours.
- **Home Assistant** — read/control with a per-category approval model, plus an
  embedded dashboard (token-seeded WebView + optional Home-screen tile).
- **Security** — provider keys/tokens AES-256-GCM under a non-exportable Keystore
  key, TLS enforced, OAuth `state` verified, plugin sandbox with an
  uncatchable instruction deadline, in-app security-audit panel.

### Jeeves features
- **Sassy Butler** — an intelligent alarm clock: daily briefing on wake, honorifics
  (Sir / Madam / Boss), sass that scales with your snoozes, weather + calendar.
- **Jotter** — Markdown notes, to-dos, and long-form documents that Jeeves pulls
  into context automatically.
- **Self-improvement** — Jeeves reflects on how its skills and agents performed on
  your device and proposes changes; every change is approved by you and version-
  tracked for rollback.
- **Local backups** — memory, skills and config to an encrypted archive on your
  device; credentials live in a passphrase-protected `secrets.json` inside it.

## Removed / not present

- **Wake word** — removed entirely in v0.17.x (engine + foreground service gone).
  Hands-free use is the manually-opened Talk mode.
- **Samsung Knox** — stub, deleted.
- The older Gist backup and offline session-export paths are retired.

---

## Getting started

1. Install the latest APK from [Releases](https://github.com/l3ad3r1/Jeeves/releases).
2. Grant permissions as prompted — Jeeves asks only when a feature needs one, and
   **Settings → Device & security → About, permissions & security** shows every
   permission as a live toggle.
3. **Settings → Assistant → Providers** — add your LLM provider and API key
   (entered in-app, encrypted).
4. Set your morning alarm from **Sassy Butler** on the Home screen.

## Building from source

```bash
git submodule update --init            # pinned llama.cpp
./gradlew :app:assembleDebug           # debug APK

# Release build needs the Vulkan + MinGW toolchain on PATH
# (JAVA_HOME=JBR, ANDROID_HOME, VULKAN_SDK, mingw64/bin) or the
# vulkan-shaders-gen host tool fails.
./gradlew :app:assembleRelease
```

| Toolchain | |
|---|---|
| Gradle / AGP / Kotlin / KSP | 9.6.1 / 9.1.1 / 2.2.10 / 2.3.5 |
| JDK | 21 (JetBrains Runtime) |
| minSdk / targetSdk | 29 / 36 |
| Native | `llama.cpp` submodule, arm64-v8a |

The code namespace stays `com.hermes.agent`; only the `applicationId` is
`com.jeeves.app`.

## Advanced usage

- **CRON routines** — schedule background agent tasks with 5-field CRON expressions
  (Settings → Connections & automation → CRON routines).
- **Delegate** — one-shot background agent tasks via WorkManager, results on Home.
- **A/B benchmark** — compare two models on one prompt with live TTFT / tok-s.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and
[docs/BUGS.md](docs/BUGS.md) for the design and known issues.

## Attribution

Shares the `agent-core` engine with [Hermes](https://github.com/l3ad3r1/Hermes-Agent-Android),
conceptually aligned with [NousResearch/hermes-agent](https://github.com/NousResearch/hermes-agent)
(no source taken). Butler alarm lineage: `l3ad3r1/Sassy-Butler-Alarm`; Jotter
lineage: `l3ad3r1/Octo-Jotter`. Direction: **l3ad3r1**. Build/test assistance:
**OpenAI Codex** and **Claude**.
