# Shared core for Hermes and Jeeves

Migration spec. Written from a measured diff of both trees on 2026-08-18, not
from assumption — the numbers below are reproducible with the survey in
[Appendix A](#appendix-a--reproducing-the-survey).

## Why

Hermes (public, v0.9.3, single `:app`) and Jeeves (private, v0.16.0, five
modules) started as one codebase. Today:

| | files |
|---|---|
| Shared paths | **349** — 239 byte-identical, 110 diverged |
| Jeeves-only | 36 |
| Hermes-only | 16 |

Every feature now costs two implementations, and the second one is usually
never written.

The divergence is not random. It falls along one line:

**Stable — the engine.** `data/tools` 26 identical / 5 diverged · `data/local`
22/4 · `data/llm` 14/4 · `domain/model` 11/2 · `data/memory` 9/0 ·
`domain/agent` 6/0 · `domain/tool`, `domain/plugin`, `data/plugin`,
`data/remote`, `util`, `data/rag`, `data/voice` — all identical.

**Diverging — the product.** Nearly every `ui/*` screen (settings 4/9, chat
2/6; onboarding, connect, evolution, documents, sessions all 0/2), plus
`data/agent` 8/9, `data/backup` 0/3, `MainActivity`, `HermesApp`.

The largest diverged files are `SettingsViewModel` (34 KB), `ChatScreen`
(31 KB), `HermesDatabase` (22 KB) and `OrchestratorImpl` (20 KB) — UI and app
composition, not engine.

**Both apps use the same package, `com.hermes.agent`.** Extraction is therefore
mostly file moves with no import rewriting. This is the single biggest reason
to do it now rather than later.

## What this does and does not solve

It removes double-maintenance of the engine.

It does **not** change release cadence. A Gradle module still compiles into the
APK; adding one still ships a new APK. Features that arrive without an app
release need the plugin sandbox (`domain/plugin` already defines the contract;
`GrpcPluginSandbox` for standalone plugin APKs is still a stub). That is a
separate project — see [Later](#later-out-of-band-features).

## Repo layout

Three repos, as decided:

| repo | visibility | contains |
|---|---|---|
| `Hermes-Agent-Android` | public | `:app` — Hermes UI, wiring, branding |
| `Jeeves` | private | `:app`, `:feature:jotter`, `:feature:butler` |
| `agent-core` *(new)* | **public** | the shared modules below |

`agent-core` must be public. A private shared repo would break public Hermes
builds for anyone without access, so nothing Jeeves-proprietary can live in it.

**Consumption:** publish `agent-core` to GitHub Packages with semver, and pin a
version in each app. Submodules were considered and rejected — this workspace
already carries one silently-dirty submodule (`llama.cpp`), which is the failure
mode in miniature. Publishing also forces the version skew to be visible in a
diff instead of implied by a pointer.

For local work on core + app together, use a Gradle composite build
(`includeBuild("../agent-core")`) so changes are picked up without publishing.

## Module boundary

Chosen from the measured stability above — extract what already matches, leave
what has genuinely diverged.

```
agent-core/
  core/domain      domain/{model,tool,agent,repository,plugin,rag,skill,
                   security,calendar,terminal,proactive,ledger}
  core/llm         data/llm, data/remote          (14/4 identical)
  core/tools       data/tools                     (26/5 identical)
  core/memory      data/memory, data/rag          (9/0 and 3/0 identical)
  core/persistence data/local                     (22/4 identical) — see note
  core/plugin      data/plugin                    (5/0 identical)
  core/settings    Hermes engine settings/security implementation
  core/theme       product-neutral Compose primitives
  core/util        util                           (4/0 identical)
```

**Stays in each app:** all of `ui/*`, `data/agent` (the orchestrator),
`data/backup`, `HermesApp`, `MainActivity`, and the DI wiring that composes
them. These are the product, and they are supposed to differ.

**`core/persistence` needs care.** `HermesDatabase` is 22 KB and diverged
because Jeeves added tables (skills, revisions, supplemental prompts, kanban,
execution plans). Extract the DAOs and entities; leave the `@Database` class and
its migration list in each app, so each owns its own schema version. Room does
not require the database class to live beside its entities.

## The feature contract

Today `di/FeatureBridge.kt` is a Hilt `@EntryPoint` that hard-depends on
Jotter's `NoteRepository` and Butler's `AlarmScheduler`. It cannot compile
without them, which is why Hermes cannot simply omit the sub-apps.

21 of 403 app files couple to those two features:

```
HermesApp, MainActivity, data/backup/{BackupData,GithubBackupService},
data/butler/{BriefingComposer,ButlerAiProviderImpl},
data/jotter/JotterAiProviderImpl, data/rag/NoteIndexer,
data/tools/{CreateNoteTool,SearchNotesTool,SetAlarmTool,TtsTool},
di/{ButlerAiModule,FeatureBridge,JotterAiModule,ToolsModule},
domain/agent/HabitExtractor, ui/home/HomeScreen,
ui/settings/{AlarmSettingsScreen,SettingsViewModel}, work/DailyDigestWorker
```

Replace the hard bridge with optional multibinding. A feature module should
register itself by existing, not by being named in three other files. Today
adding one tool means editing `ToolsModule`, granting it in `AgentToolAccess`,
and listing it in the agent's system prompt — three coordinated edits, any of
which can be forgotten, and registration alone does nothing.

```kotlin
interface AgentFeature {
    val id: String
    fun tools(): List<Tool>              // contributed to the registry
    fun promptFragment(): String?        // appended to the agent prompt
    fun backupContributions(): List<BackupContribution>
    fun entries(): List<NavEntry>        // screens, if any
}
```

Bound with `@IntoSet`, so an app gets exactly the features on its classpath and
the core never names them. Hermes then omits `:feature:jotter` and
`:feature:butler` and the note/alarm tools simply are not registered — no
stubs, no dead settings screens, no compile error.

## Migration sequence

Each step must leave both apps building and their tests green. Do not batch.

1. **Create `agent-core`; move `core/util` and `core/domain`.** Pure Kotlin,
   zero Android dependencies, already identical in both trees. Proves the
   publishing pipeline on the lowest-risk code.
2. **Move `core/llm`, `core/tools`, `core/memory`, `core/plugin`.** Still
   overwhelmingly identical. Where a file diverged, Jeeves is the newer side —
   diff it, take Jeeves, and note anything Hermes loses.
3. **Move `core/settings` and `core/theme`.** Keep Jeeves' synchronous alarm/voice
   preferences and private feature contracts in `:core:jeeves-settings`; they are
   not the same responsibility as Hermes' DataStore-backed engine settings. Keep
   product branding in each app and share only theme primitives.
4. **Split `core/persistence`.** DAOs and entities move; `@Database` and
   migrations stay per app.
5. **Introduce `AgentFeature`,** convert Jeeves' Jotter and Butler wiring to it,
   and delete `FeatureBridge`.
6. **Bring Hermes onto the shared core** — the goal of this project. Hermes
   gains everything in `agent-core` and stays free of `:feature:jotter` and
   `:feature:butler`.

Steps 1–4 are mechanical and safe because the files already match. Step 5 is the
design work. Step 6 is where Hermes actually catches up.

## What Hermes gains

Everything the engine has accumulated in Jeeves since v0.9.3, without the
sub-apps: the skill system with descriptions and rollback, supplemental agent
prompts, `cloudOnly` routing and provider failover, the marker-based secret
handling, portable encrypted backups, and 16 KB page alignment.

Explicitly **not** ported, per instruction: AI Notes (Jotter) and Daybook
(Butler), and the app-side integration listed above.

## Later: out-of-band features

Only worth starting once the above is stable. `InProcessPluginSandbox` already
loads first-party plugins inside the APK; `GrpcPluginSandbox` — standalone
plugin APKs over local sockets — is a documented stub. Finishing it is what
would let a feature ship without an app release. Until then, "module" means
"compiled in", and every feature addition is still a release.

## Appendix A — reproducing the survey

Hashes every `.kt` under both `app/src/main/kotlin/com/hermes/agent`, ignoring
blank lines and trailing whitespace, then groups by the first two path segments.
Re-run it after each migration step: the identical count should rise and the
diverged count should fall. If it does not, the step moved the wrong thing.

Script: `docs/tools/survey-divergence.py`.
