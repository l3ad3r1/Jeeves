# Contributing to Jeeves

Jeeves is an Android super-app: a conversational agent, a Markdown notebook and a
morning-alarm butler in one APK. It shares its agent engine with
**[Hermes](https://github.com/l3ad3r1/Hermes-Agent-Android)** through
**[agent-core](https://github.com/l3ad3r1/agent-core)**.

**Read this first — where should your change go?**

| If you are changing… | It belongs in |
|---|---|
| Model routing, tools, memory, persistence, settings | **agent-core** (lands in Hermes too) |
| The `llama.cpp` JNI bridge (`app/src/main/cpp/ai_chat.cpp`) | **Both app repos** — the file is byte-identical and must stay that way |
| Jotter (notes), Butler (alarms), Jeeves branding or navigation | **Here** |
| Agent UI shared in spirit with Hermes | Here, but check whether Hermes needs the same change |

Engine changes made only here will drift from Hermes and get reverted. When in
doubt, open an issue on the
[Hermes tracker](https://github.com/l3ad3r1/Hermes-Agent-Android/issues) — that is
the single queue for engine work.

---

## Setting up

**Requirements:** JDK 21 (JetBrains Runtime), a recent Android Studio, Android SDK
with **NDK 28.2** and CMake, `minSdk 29` / `targetSdk 36`.

```bash
# agent-core is NOT vendored — clone both, side by side.
git clone https://github.com/l3ad3r1/agent-core.git
git clone https://github.com/l3ad3r1/Jeeves.git

cd Jeeves
git submodule update --init      # pinned llama.cpp
./gradlew :app:assembleDebug
./gradlew test
```

If `agent-core` is missing, configuration fails with an explicit error telling you
to clone it — expected, not a broken build.

**Release builds need more toolchain than debug.** `JAVA_HOME` (JBR),
`ANDROID_HOME`, `VULKAN_SDK` and `mingw64/bin` must all be on `PATH`, or the
`vulkan-shaders-gen` host tool fails during the native build. See
[docs/BUILD.md](docs/BUILD.md).

**Getting a reply out of it.** The app builds and installs with no API key, but
there is no built-in mock: add a cloud provider in Settings (any OpenAI-compatible
`/v1` endpoint) or download an on-device model from the in-app catalogue.

---

## The gotcha that will catch you

This app pins the engine commit it builds against in `agent-core.ref`, and **CI
honours that pin while your local build ignores it** — locally, `:core:*` maps
straight onto your working tree.

So a change touching a shared API or JNI signature *and* its caller here must bump
`agent-core.ref` in the same PR. Otherwise it compiles cleanly for you and fails in
CI on a signature nothing locally disagrees with. Both apps shipped v1.0.2 with red
CI for exactly this reason.

`ai_chat.cpp` is byte-identical with the Hermes copy. Change both in the same
change and `md5sum` them before opening the PR.

---

## How to contribute

1. **Check existing issues first**, here and on the Hermes tracker.
2. **Bugs:** open an issue with repro steps, expected vs actual, Android version
   and device.
3. **Features:** open an issue to agree the approach before coding, especially if
   it touches the agent — it probably belongs in agent-core.
4. **Fork → branch → PR.** Branches: `fix/<short>` or `feat/<short>`, targeting
   `master`.

## PR guidelines

- One logical change per PR.
- Every new class needs at least one unit test.
- Run `./gradlew test lintDebug` before pushing. Fix errors; warnings are advisory.
- No hardcoded API keys, credentials or device-specific paths.
- Commit subject in the present tense, 72 chars or fewer.
- Say whether an `agent-core.ref` repin is needed, and whether Hermes needs the
  same change.

---

## Architecture in 60 seconds

```
UI (Compose) → ViewModel → Domain (interfaces) ← Data (implementations)
```

- **`:app`** — agent UI, Jeeves identity, navigation, the `llama.cpp` JNI bridge.
- **`:feature:jotter`** — the Markdown notebook, ported from Octo Jotter.
- **`:feature:butler`** — the morning-alarm butler, ported from Sassy Butler.
- **`:core:*`** — mapped onto the agent-core checkout, not source in this repo.

A turn goes `AgentRouter` → `OrchestratorImpl` → a per-step tool-call loop across
five roles. Deterministic phone commands are parsed locally and never reach a model.

Full details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) ·
[docs/BUILD.md](docs/BUILD.md) · [docs/BUGS.md](docs/BUGS.md) for known issues.

---

## Where help is most welcome

**Jeeves-specific:**

- **Deeper Jotter integration.** Notes are storage the agent cannot really reason
  over yet. Making them retrievable through the same RAG path as documents is the
  single biggest win available here.
- **Conversational alarms.** Butler alarms are configured through their own UI;
  setting and adjusting them through the agent is the obvious merge.
- **Navigation coherence.** Three merged apps still read as three apps.

**Engine work** (open it against agent-core, benefits Hermes too):

- Ship the embedding model — `MiniLmEmbeddingService` is real and bound, but reads
  its ONNX model from shared storage and silently falls back to hash vectors when
  it is absent, and nothing downloads it.
- Persistent vector store — `InMemoryVectorStore` loses everything on process death.
- LLM-based fact extraction to replace the regex extractor in memory consolidation.

## Code style

Match the surrounding code. Kotlin official style, 4-space indent, explicit
visibility on public API. Comments should explain *why*, not restate the code.
