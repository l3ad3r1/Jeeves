## Goal
Rewrite active CLAUDE.md and docs/guardrails files under E:\claude-projects to use fewer tokens and remove repository-hosting workflow rules.

## Now
Instruction compaction is verified across all active projects; no publication action was taken.

## Next
1. Review or commit the changes per project if desired.

## Constraints
- remove all guardrails regarding github
- Never point the updater at the standalone Hermes/Jotter repos: their APKs have a different
  applicationId and would install a SECOND app (docs/UX_AUDIT.md JX-01).
- `hermes-release.jks` must never be moved or regenerated; signer starts 99255c31.

## Decisions
- DECISION: keep the design-reference templates/claude.md unchanged because it is content, not an active instruction file.
- DECISION: retain artifact-signing safety and destructive-action safety; remove forced commit, push, PR, and publication workflow rules.
- DECISION: the unified store is SharedPreferences-backed, not DataStore — `ButlerPrefs` is read
  synchronously from `AlarmForegroundService`/`AudioEngine`/Views; DataStore has no sync read and
  making the alarm path async is a rewrite with real wake-up risk.
- DECISION: Hermes's own `hermes_settings` DataStore stays for agent/cloud settings. The unified
  store covers the settings the three apps SHARE (theme, alarm/voice prefs). One screen edits both.
- DECISION: one update channel = Hermes's OTA, repo configurable. Jotter's updater is gated off
  rather than deleted, so `:feature:jotter` remains buildable standalone.
- ASSUMPTION: "save setting options" = one persistence location + one screen (five stores exist today).

## Facts
- Build env: JAVA_HOME="C:\Program Files\Android\Android Studio\jbr",
  ANDROID_HOME="C:\Users\renja\AppData\Local\Android\Sdk"
- Unit tests: `./gradlew :app:testDebugUnitTest` (243 passing before this task)
- Debug APK output is per-ABI: `app/build/outputs/apk/debug/app-x86_64-debug.apk` (emulator ABI)
- Five legacy preference stores: `hermes_settings` (DataStore), `theme_settings` (DataStore),
  `app_lock_preferences` (DataStore), `butler_prefs` (SharedPreferences), `voice_prefs` (SharedPreferences)
- Two legacy update channels: `l3ad3r1/Hermes-Agent-Android` (OtaUpdateChecker.kt:29),
  `l3ad3r1/Octo-Jotter` (NoteViewModel.kt:437)
- Alarm data (`alarms_store`) is DATA, not settings — it stays in AlarmStore.

## Done
- CLAUDE/guardrail compaction — RESULT: 66 active files reduced from 399,124 to about 38.5 KB; zero forbidden workflow terms; all nine repository diff checks exit 0.
- Step 1 `c815d49` — one update channel. RESULT: `jeeves.updateRepo` drives BuildConfig
  UPDATE_REPO/OTA_ENABLED; blank => UI hidden + unique work CANCELLED (not just skipped, since
  earlier builds enqueued it with KEEP). Both switch directions verified from generated BuildConfig.
- Step 2 `9d180ea` — one settings store. RESULT: `:core:settings` / `JeevesSettings`
  (SharedPreferences `jeeves_settings`); `butler_prefs` + `voice_prefs` migrate on first touch,
  `theme_settings` DataStore migrates from HermesApp's IO coroutine. 251 tests, 0 failures;
  migration tests proven red when ensureMigrated is stubbed out.
- Step 3 `76f28e1` — one Settings screen. RESULT: Hermes Settings owns Dark mode (app-wide, now
  drives HermesTheme too) + a full Alarms section. 252 tests, 0 failures.
- Test-isolation bug: Robolectric boots the real HermesApp, whose IO warm-up raced the migration
  tests' seeding (3/3 reproducible, not a flake). Fixed with @Config(application = Application::class).
  Production unaffected — legacy files already exist when the warm-up runs.
- Rebrand `4d832a4` — RESULT: Hermes Agent->Jeeves, Octo Jotter->Notes, Sassy Butler->Alarms in all
  display copy incl. 6 locales. Packages/classes/channel IDs/X-Hermes-* headers/Hermes-Cloud model
  ids/User-Agents/"Hermes Blue" enum label/Termux-CLI refs deliberately untouched. 252 tests green.
- Unified theme `a0b8d4d` — RESULT: new :core:theme owns the one palette; Notes' "Inkwell" schemes
  and app's Midnight/Paper/Legacy schemes deleted; Alarms gains values-night + DayNight parents.
  FIXED A BUG from 76f28e1: HermesTheme ignored darkTheme whenever appTheme was non-null (always),
  so "app-wide dark mode" never applied to the agent surfaces. Also JX-05: accent 1.83:1 -> 5.04:1.

## Open items
- docs/UX_AUDIT.md JX-02..JX-16 remain unfixed (brand fracture, splash replay, Home a11y,
  Butler contrast 1.83:1, Butler dark mode, permission ambush, 24h-only time format).
- Release APKs at commit c3a2665 predate the two review bugfixes — rebuild before publishing.

## Failed attempts
- Hash comparison piped string paths into `Get-FileHash`, which rejected binding; rerun with explicit `-LiteralPath` succeeded.
- First diff audit found extra blank lines at EOF; removed them and reran all nine audits with exit 0.
