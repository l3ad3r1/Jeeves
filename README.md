# Jeeves — Android App

A privacy-first, on-device AI assistant platform for Android.

> **Status:** v0.9.2 — Now with a fully revamped "Sassy Butler" dark-mode theme, a new notes module (Jotter) with Notesnook-inspired styling, and an elegant conversational interface.

## Core Features

| Module                      | Status | Notes                                                                 |
|-----------------------------|--------|-----------------------------------------------------------------------|
| Jetpack Compose UI shell    | ✅      | Revamped Butler Theme: Dark-mode by default, sleek chat & jotter interfaces |
| Hilt DI                     | ✅      | App / Database / Network / LLM / Tools / Agents                       |
| Room persistence            | ✅      | Conversations, messages, memories, documents, and notes (Jotter)      |
| On-device & Cloud LLM       | ✅      | Intelligent routing between on-device and cloud LLM execution         |
| **Multi-agent orchestration** | ✅    | Multi-agent architecture with robust tool-calling support             |
| **Tool system**             | ✅      | First-party and plugin-contributed tools for extended capabilities    |
| **Jotter (Notes UI)**       | ✅      | Markdown-supported note editor with Notesnook-inspired minimal layout |
| **Memory & RAG**            | ✅      | Semantic search, memory consolidation, and hybrid RAG pipeline        |
| **Real SSE streaming**      | ✅      | Seamless typing indicators and word-by-word streaming                 |
| **Settings & Security**     | ✅      | Secure encrypted keys, Samsung Knox, DataStore toggles                |

## Quick start

```bash
# 1. Open in Android Studio (Hedgehog or newer)
#    OR build from the command line:
./gradlew assembleDebug

# 2. Install on a device or emulator (Android 10+ / API 29+):
./gradlew installDebug

# 3. (Optional) Provide a cloud API key without checking it in.
#    Create jeeves.local.properties at the repo root with:
#        jeeves.cloudApiKey=sk-your-openai-key
```

## UI and Theming (Butler Theme)

We recently overhauled the UI to use the `Butler Theme`. The app embraces a dark, sophisticated aesthetic:
- **Jotter Module:** A highly capable note-taking application module built directly into Jeeves. Features a sleek sidebar and distraction-free Markdown editor.
- **Chat:** An interactive Chat UI that pairs you with Jeeves for seamless conversations.

## Project layout

```
jeeves-android/
├── app/
│   ├── src/main/kotlin/com/l3ad3r1/octojotter/
│   │   ├── HermesApp.kt              # Application + WorkManager bootstrap
│   │   ├── MainActivity.kt           # Single-activity entry
│   │   └── di/                       # Hilt modules
├── feature/
│   ├── jotter/                       # The Jotter notes module
│   └── butler/                       # The core UI theme & components
├── core/                             # Core logic, domain, and data layers
└── gradle/libs.versions.toml         # Version catalog
```

## Tech stack

| Layer        | Library                                                   |
|--------------|-----------------------------------------------------------|
| UI           | Jetpack Compose + Material 3 + Navigation                 |
| DI           | Hilt                                                      |
| Persistence  | Room (schema export on)                                   |
| Networking   | Retrofit + OkHttp + kotlinx.serialization                 |
| Async        | Coroutines + WorkManager                                  |

## License & attribution

This project evolved from early agent platforms and is now a standalone assistant named Jeeves.
