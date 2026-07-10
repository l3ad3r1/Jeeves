# Hermes Merged App — Merge Notes

This project combines the two prototypes from `Hermes Agent Android App/`:

| Source | Package | What it brought |
|--------|---------|-----------------|
| **Hermes Android App** → `hermes-agent-repo` | `com.hermes.agent` | The full, working agent app — chat, multi-agent orchestration, LLM routing, memory/RAG, skills (incl. autonomous creation), terminal/shell/Termux, cron/WorkManager, plugins, GitHub backup, OTA, settings & security. **Used as the base.** |
| **Hermes Android App 2** | `com.nous.hermes` | A lighter app on mostly dummy data, but with a **working Kanban board**, an `AgentForegroundService`, messaging gateways, and a scheduler. **Good parts ported in.** |

The base is `hermes-agent-repo` (the more complete of App 1's two copies — its sibling
`hermes-agent-android` was an older subset). Everything lives under one package,
`com.hermes.agent`, one Hilt graph, one Room database, and one navigation graph.

---

## What was ported from App 2 (and how)

### 1. Kanban board — now backed by real Room persistence
App 2 rendered the board from hard-coded in-memory tickets. In the merged app it is a
real, durable feature:

- `domain/model/KanbanTicket.kt` — `KanbanTicket`, `KanbanStatus`, `TicketPriority`.
- `data/local/entity/KanbanTicketEntity.kt` + `data/local/dao/KanbanTicketDao.kt`.
- Registered in `HermesDatabase` (schema **v5 → v6**, new `kanban_tickets` table via
  `MIGRATION_5_6`) and provided through `DatabaseModule`.
- `domain/repository/KanbanRepository.kt` + `data/repository/KanbanRepositoryImpl.kt`
  (create / move / complete / delete / seed), bound in `di/KanbanModule.kt`.
- UI re-homed to `ui/kanban/`: `KanbanViewModel` (observes the repo), `KanbanBoardScreen`
  (board + list views, status tabs, **create-ticket dialog**), `TicketDetailScreen` +
  `TicketDetailViewModel` (real detail, status moves, delete), and `KanbanChips`.
- Kanban status/priority colors added to the app's existing `ui/theme/Color.kt`.
- New **"Board"** entry in the bottom navigation; ticket detail is a child route.
- First open seeds four example tickets (mirrors the prototype's demo data) — after that
  everything you create/move/delete is persisted.

### 2. Always-on AgentForegroundService (24/7 persistence)
App 1 only used periodic WorkManager jobs; it had no always-on service. App 2's
foreground service was ported and **rewired to do real work**:

- `service/AgentForegroundService.kt` — `@AndroidEntryPoint` foreground service
  (`dataSync` type) with a persistent low-priority notification. Its loop polls the real
  `KanbanRepository` for the oldest **TODO** ticket, marks it **IN_PROGRESS**, runs it,
  marks it **DONE** with a result, and pushes a completion message through the existing
  `notify` tool.
- `service/AgentServiceController.kt` — process-wide start/stop + a `running` StateFlow the
  UI observes. Start/stop is surfaced as a ▶/⏹ button in the Kanban top bar.
- `service/BootReceiver.kt` — restarts the service on `BOOT_COMPLETED` so Hermes resumes
  after a reboot.
- Manifest: `<service>` + `<receiver>` declarations and the
  `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `RECEIVE_BOOT_COMPLETED`,
  `WAKE_LOCK` permissions.

> The ticket "execution" is intentionally a placeholder `delay()` — the claim → execute →
> complete → notify pipeline is fully wired so dropping the real orchestrator call into
> `AgentForegroundService.tick()` is a one-line change.

### 3. Battery-optimization exemption (Doze survival)
Salvaged from App 2's scheduler: `AgentServiceController.requestBatteryOptimizationExemption()`
prompts the OS to exempt Hermes from Doze so the always-on service survives long idle
periods. It is called automatically when the agent service is started, and the
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission was added to the manifest.

### 4. SMS gateway — added to App 1's existing messaging system
App 1 already had a **better** gateway system than App 2: the `notify` tool
(`data/tools/WebhookTool.kt`) plus `ConnectorRepository` and the "Messaging" screen already
send to **Telegram, Discord, Signal, WhatsApp, and webhooks** over REST. Rather than bolt
on App 2's parallel `GatewayManager`, the one channel App 1 lacked — **SMS** — was added
natively:

- `ConnectorType.SMS` added to `domain/model/Connector.kt`.
- `WebhookTool` now injects `@ApplicationContext` and sends SMS via `SmsManager`
  (multipart, permission-checked). It appears automatically in the Messaging screen.
- `SEND_SMS` permission added to the manifest.

---

## What was intentionally **not** ported (and why)

| App 2 component | Reason |
|-----------------|--------|
| `GatewayManager`, `TelegramGateway`, `DiscordGateway` | **Redundant.** App 1's `WebhookTool` + connector system already covers Telegram/Discord (and Signal/WhatsApp/webhook) over REST, persisted and exposed as an LLM tool. App 2's `DiscordGateway` also pulled in the heavy **JDA** library, which is poorly suited to Android. |
| `SchedulerManager` / `CronWorker` / `CronAlarmReceiver` | **Redundant.** App 1 already has a stronger scheduling stack: `CronRepository`, `ScheduledTaskWorker`, the CRON screen, and Hilt-aware WorkManager wiring. |
| App 2 `HomeScreen` / `SessionList` / dummy `MemoryScreen` / `SkillsScreen` | App 1's equivalents are real (repository-backed) and more complete. |
| App 2 `CoreTools` (web_search, terminal, read/write/search file) | Redundant — App 1 already has a web-search tool, Termux/shell execution, and file tooling. App 2's `terminal` used `ProcessBuilder("bash")`, which doesn't exist on stock Android anyway. |
| App 2 extra deps: JDA, Rhino, jGit, iText7, Glide, kaml, ktor, Markwon | None were needed — each backed a feature App 1 already implements another way (or that no salvaged feature uses). Avoided to keep the build lean. |
| App 2 inbound SMS (`READ_SMS`/`RECEIVE_SMS` + receiver) | App 1's gateways are outbound (`notify`); a full inbound SMS pipeline was out of scope. Only outbound `SEND_SMS` was added. |

**Final salvage pass:** after a full file-by-file review of App 2, the only additional item
worth taking was the battery-optimization exemption (above). Everything else was either
already ported, redundant with a stronger App 1 implementation, or unused scaffolding. The
`Hermes Android App 2` source folder was removed after this review.

Porting these would have created two competing subsystems on different schemas. The merge
keeps **one** of each.

---

## Build / verify

- No new Gradle dependencies were required — OkHttp, WorkManager, Hilt (+ hilt-work), Room,
  `core-ktx`, and `material-icons-extended` were all already present. SMS and the foreground
  service use the Android framework directly.
- Requires **JDK 17** and the Android SDK (AGP 8.13.2, Kotlin 2.0.21, `compileSdk 34`).
  Build with `./gradlew assembleDebug` in Android Studio or a configured CI.
- This merge was assembled and statically reviewed in an environment without the Android
  SDK, so it has **not been compiled** here — do a Gradle sync + debug build before shipping.

## Notes
- The original prototype source folders are left untouched; this is a fresh copy.
- A handful of prebuilt `*.apk` files copied in from the base could not be deleted from the
  working environment — they are stale artifacts and safe to remove.

---

# Second merge — Jeeves (Hermes + Octo Jotter + Sassy Butler)

The app above became the base of **Jeeves**, which folds in two more standalone apps.
Full history and per-phase evidence: `PROGRESS.md`; plan: `docs/SUPER_APP_ROADMAP.md`.

| Source | Package | Became |
|--------|---------|--------|
| Hermes Agent (this repo) | `com.hermes.agent` | `:app` — host, launcher, single Hilt graph |
| Octo Jotter | `com.l3ad3r1.octojotter` | `:feature:jotter` — Android library (Compose) |
| Sassy Butler | `com.sassybutler.alarm` | `:feature:butler` — Android library (Views + ONNX TTS) |

Shipped as one APK, `applicationId = com.jeeves.app`, so it installs alongside the three
standalone apps rather than replacing them. Each feature module's **namespace is the original
package**, so sources moved across with no renames and no `R`-class collisions.

## What differs from the plan
- **Toolchain bumps could not be committed one at a time.** AGP 8.x dies on Gradle >= 9.6.0
  (`InternalProblems` removed); AGP 9 has built-in Kotlin and rejects `kotlin-android`;
  Hilt < 2.57 fails on AGP 9; the old KSP line registers sources via `kotlin.sourceSets`,
  which built-in Kotlin forbids. They land as one commit.
- **Jotter is launched as an Activity, not embedded in the host nav graph.** Its entry is a
  `FragmentActivity` (BiometricPrompt requires one); the host is a `ComponentActivity`.
- **No duplicate `OkHttpClient` binding existed.** Jotter's client is a private field in a
  Kotlin `object`, never a Hilt binding. `JotterModule` deliberately does not bind one.
- **Phase 5 (fold Jotter's Room DB into `HermesDatabase`) was skipped.** The agent reaches
  notes through the injected `NoteRepository`, so no schema bump or migration is warranted.
  `gist_notes_database` and `hermes.db` coexist.

## Landmines worth remembering
- **`file_paths.xml`.** Jotter calls `getUriForFile(..., "${packageName}.fileprovider", ...)`,
  which resolves to the *host's* provider — and an app-module `res/xml` always overrides a
  library's. Jotter's three roots had to be merged into `app/src/main/res/xml/file_paths.xml`.
  Without them, every note export crashes with `Failed to find configured root`. Proven by
  removing them and watching it crash; restored and re-verified.
- **`noCompress` and `jniLibs.pickFirsts` belong in `:app`, not `:feature:butler`.** Both are
  applied when the APK is packaged, after library assets are merged in.
- **`TtsEngine` must never be a Hilt binding.** Its `init` reads the whole ~92 MB ONNX model
  synchronously. `ButlerSpeech` owns it and builds it lazily on `Dispatchers.IO` behind a Mutex.
- **Kotlin default arguments are evaluated at the call site.** `speak(text, voice = VoiceCatalog
  .selected(context))` ran a SharedPreferences disk read on the caller's thread, *outside* the
  `withContext(Dispatchers.IO)` it appeared to sit behind.
- **Neither Jotter nor Butler ever shipped minified** (`isMinifyEnabled = false` standalone).
  Their reflective surfaces — Moshi's `KotlinJsonAdapterFactory`, Rhino, ONNX's JNI peers — all
  needed explicit keep rules before the merged release build would work. See `proguard-rules.pro`.
- **Measure APK size only from a clean build.** AGP's incremental packager rewrites
  `app-debug.apk` in place and leaves gaps; a dirty tree reported ~13 MB of phantom growth.

## Agent integration (the reason for merging at all)
`create_note` -> Jotter's `NoteRepository`; `set_alarm` -> Butler's `AlarmScheduler` +
`AlarmStore` (persist *before* scheduling, or the alarm dies on reboot); and Hermes's `speak`
tool now uses Butler's on-device Kokoro voice via `ButlerSpeech`, falling back to platform TTS.
Every tool needs all three wiring steps — `di/ToolsModule`, `AgentToolAccess`, persona prompts.
