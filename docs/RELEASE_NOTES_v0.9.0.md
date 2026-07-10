# Jeeves v0.9.0 — first release

One Android app: an on-device AI agent, **Notes**, and **Alarms** — merged from three
standalone apps (Hermes Agent, Octo Jotter, Sassy Butler) into a single signed APK with one
launcher icon, one settings screen, one update channel, and one theme.

## What it is
- **Jeeves** — the agent. Chat, tools, skills, cron, memory, Termux/shell.
- **Notes** — Markdown notes with GitHub Gist sync and a community plugin system.
- **Alarms** — an alarm clock with an on-device Kokoro/ONNX voice.

They are not three apps in a trench coat: they share one Hilt graph, one settings store, and
one process — which is what lets the agent act across them.

## The payoff: the agent can act on the other two
- `create_note` — writes a Markdown note into Notes.
- `set_alarm` — schedules an alarm ("wake me at 7am"), persisted so it survives a reboot.
- `speak` — replies aloud in Alarms' on-device ONNX voice, falling back to the platform engine.

## Install
Pick the APK for your device. `arm64-v8a` is right for essentially every modern phone;
`universal` works everywhere but is 55 MB larger.

| APK | Size | For |
|---|---|---|
| `app-arm64-v8a-release.apk` | 145.6 MB | most phones |
| `app-armeabi-v7a-release.apk` | 140.0 MB | older 32-bit phones |
| `app-x86_64-release.apk` | 148.2 MB | emulators |
| `app-universal-release.apk` | 201.0 MB | one artifact, all ABIs |

Signed with the release key — certificate SHA-256 begins `99255c31`. Verify before installing:
```
apksigner verify --print-certs app-arm64-v8a-release.apk
```

The APK is large because it embeds ~115 MB of Kokoro TTS model weights, stored uncompressed so
they can be memory-mapped at wake time.

## Notes on data
Jeeves uses its own `applicationId` (`com.jeeves.app`), so it installs **alongside** the three
standalone apps rather than replacing them. It does not read their data:
- **Alarms** starts fresh — recreate your alarms.
- **Notes** starts empty — reconnect GitHub and re-pull your Gists.

If you previously ran a Jeeves build, your Alarms preferences and theme migrate automatically
into the unified settings store.

## Known gaps
- **This build has not been verified on a device.** It compiles and passes 252 unit tests plus
  4 instrumented tests, and earlier builds were smoke-tested on a Pixel 7 emulator, but the
  rebrand, unified theme, unified settings screen and settings migration have only been
  verified by build and unit test.
- No end-to-end LLM-driven tool call has been observed (needs a real API key). Everything up to
  the network boundary is proven.
- Open UX issues are tracked in `docs/UX_AUDIT.md` (JX-03 splash replay, JX-04 Home
  accessibility, JX-07 permission choreography, and others).

## Setup
Cloud LLM is off by default. Settings → Cloud LLM → enable, paste an OpenAI-compatible API key
and base URL.
