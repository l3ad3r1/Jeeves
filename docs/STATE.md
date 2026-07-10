## Goal
Unify the three merged apps: same settings screen, same update channel, one place settings are
saved. Jotter and Butler are integrated parts of Hermes, not separate apps.

## Now
Step 1 of 3 — collapse the two update channels into one configurable Jeeves channel.

## Next
1. Step 1: one update channel. `jeeves.updateRepo` in gradle.properties -> `BuildConfig.UPDATE_REPO`
   + `OTA_ENABLED`. Jotter's updater UI/VM gated off; Hermes OTA is the only updater; blank repo
   hides the UI and cancels the background worker. Check: `:app:assembleDebug` + `:app:testDebugUnitTest`.
2. Step 2: new `:core:settings` Android library with `JeevesSettings` — ONE SharedPreferences
   file `jeeves_settings`, sync getters (Butler's Views/services need sync) + Flows (Compose).
   One-time migration from legacy `butler_prefs`, `voice_prefs`, `theme_settings`. Butler's
   `ButlerPrefs`/`VoiceCatalog` and Jotter's `ThemePreferences` delegate to it, public APIs
   unchanged. Check: Robolectric tests for migration + delegation.
3. Step 3: one Settings screen — Hermes Settings gains "Notes" and "Alarms" sections editing
   `JeevesSettings`; Jotter's theme row and Butler's preferences sheet route there.

## Constraints
- Never point the updater at the standalone Hermes/Jotter repos: their APKs have a different
  applicationId and would install a SECOND app (docs/UX_AUDIT.md JX-01).
- `hermes-release.jks` must never be moved or regenerated; signer starts 99255c31.

## Decisions
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
- (this task) nothing yet.

## Open items
- docs/UX_AUDIT.md JX-02..JX-16 remain unfixed (brand fracture, splash replay, Home a11y,
  Butler contrast 1.83:1, Butler dark mode, permission ambush, 24h-only time format).
- Release APKs at commit c3a2665 predate the two review bugfixes — rebuild before publishing.

## Failed attempts
- (none for this task)
