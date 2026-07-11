# Digital Butler Roadmap — From Merged Super App to True Personal Butler

**Status:** Planning, revised against the repo as of **v0.9.3** (2026-07-11).
Successor to `SUPER_APP_ROADMAP.md`, whose Phases 0–7 are complete: the merge is done and
**published** — releases v0.9.0 → v0.9.3 are live on `l3ad3r1/jeeves`, latest is a single
arm64-v8a APK (~117 MB).

> ⚠ `PROGRESS.md`'s Phase 7 section ("no remote, publishing NOT DONE") predates
> v0.9.1–v0.9.3 and is stale. Trust the git log and GitHub releases over it.

**This roadmap answers:** the merge made Jeeves *one app*, and the v0.9.1–v0.9.3 wave made
it *one product* (one brand, one theme, one settings store, sane consent). What makes it a
*butler*? Executed phase-by-phase; each phase independently shippable with a hard gate.

---

## 1. Where the app actually is (v0.9.3)

### Landed since v0.9.0 (10 commits — do not re-plan these)

- **Published & updatable:** remote `l3ad3r1/jeeves`, releases v0.9.0–v0.9.3 (`--latest`);
  one OTA channel driven by `jeeves.updateRepo` (Jotter's second updater gated off).
- **One settings store:** `:core:settings` / `JeevesSettings` with proven migrations from
  the five legacy stores; **settings god-surface split** into six subpages (Assistant,
  Appearance, Alarms, Connections, Advanced, About/Security) — old audit H-08 addressed.
- **Trust fixes shipped:** onboarding permission walls removed — permissions are now
  requested lazily per feature (old P0/H-01); standardized destructive-action dialogs
  across plugins/skills/sessions (H-06); `LiveRegionMode.Polite` announcements, top bars
  with back nav, unified loading/error/empty + Retry states, debounced session search.
- **One brand:** Jeeves / Notes / Alarms naming (6 locales), new adaptive icon, unified
  `:core:theme` dark "Butler" palette (fixed the darkTheme-ignored bug and the 1.83:1
  accent contrast, now 5.04:1).
- **Butler grew real butler features:** Compose theme + overhauled setup UI;
  `ButlerScript` (sass-tiered spoken lines: time + cached `WeatherService` weather +
  sass line); AI greeting via `ButlerAiProviderImpl` (LLM, ≤30 words); `CalendarSyncManager`
  (alarms mirrored to calendar events); `VoiceDownloader` (extra Kokoro voices fetched at
  runtime via DownloadManager); `PhonemeEncoder` lexicon pipeline.
- **Notes UI overhauled** (Notesnook-style editor + sidebar with Favorites/Reminders/
  Archive/Trash groupings).
- **Size work started:** int8 Kokoro model, bundled TTS assets 115 → 89 MB, release
  restricted to arm64-v8a only → one ~117 MB APK.

### Honest deficits (each anchors a phase)

| # | Deficit | Evidence |
|---|---------|----------|
| D1 | **Version bookkeeping broke.** Three releases were tagged but the single source of truth still reads `jeeves.versionCode=60 / versionName=0.9.0` (`gradle.properties`); root `RELEASE_NOTES.md` calls the same work "1.1.0". If all shipped APKs carry versionCode 60, the OTA updater cannot distinguish them. `PROGRESS.md` is stale. | `gradle.properties:21-22`; tags v0.9.1–v0.9.3 |
| D2 | **Audit closure unproven.** The big UX fixes shipped, but the 07-11 page audit was never re-run against them, and the device-validation matrix (TalkBack, Switch Access, 200% font, reduced motion) has still never executed on hardware. | `docs/UI_UX_PAGE_AUDIT_2026-07-11.md` §Required device validation |
| D3 | **The agent cannot see the user's knowledge.** Notes live in a separate Room DB the agent can write to (`create_note`) but not search (Phase 5 skipped). | `PROGRESS.md` Phase 5 |
| D4 | **Reactive only.** Wake-up now speaks time + weather + sass, but there is no calendar/todo/news *briefing*, no digest, no nudges; cron is time-only. | `ButlerScript.greeting()`; `work/CronScheduler.kt` |
| D5 | **Buried behind an app icon.** No assistant role, widget, tile, share-target on the agent, or notification quick-reply. | manifest has no ASSIST/widget/tile entries |
| D6 | **Autonomy without accountability surfaces.** `DelegateTool` exists but background delegation, async result delivery, and approval gates for risky tools (shell, SMS, device settings) are partial. | `docs/FEATURE_GAP_ANALYSIS.md` §3 |
| D7 | **Still a 117 MB install.** 89 MB of TTS assets remain build-time-bundled; `VoiceDownloader` proves the runtime-fetch pattern (currently without checksum verification) but the base model doesn't use it. | `feature/butler/src/main/assets/tts/`; `VoiceDownloader.kt` |

Principle for prioritizing: **butler value beats desktop parity.** Items from
`FEATURE_GAP_ANALYSIS.md` (kanban dispatcher, Slack connector, CLI slash commands) enter
only when a phase below needs them.

---

## 2. North star

A butler is judged on five qualities, in order:

1. **Trustworthy** — never surprises you with a permission, a deletion, or a leaked secret.
2. **Proactive** — briefs you in the morning, nudges you about commitments, digests noise.
3. **Ambient** — reachable in ≤ 1 gesture from anywhere on the device.
4. **Knows you** — one unified memory across chat, notes, alarms, and habits.
5. **Acts autonomously, accountably** — does multi-step work in the background, shows its
   receipts, and asks before anything irreversible.

---

## 3. Phases

### Phase B0 — Truth & verification debt · Effort: S–M

**Goal:** the record matches reality and the shipped fixes are proven, not just written.
(The original B0 "ship & stabilize" is mostly DONE — this is what's left of it.)

- [ ] **Fix version bookkeeping:** bump `jeeves.versionCode`/`versionName` in
      `gradle.properties` to match the actual release line; reconcile root
      `RELEASE_NOTES.md` ("1.1.0") with the tag scheme; verify the OTA updater's
      version comparison actually detects v0.9.3 → next as an upgrade on device.
- [ ] **Refresh `PROGRESS.md`:** record v0.9.1–v0.9.3 (publish, unification, rebrand,
      Butler/Notes overhauls) so future sessions don't plan from stale state.
- [ ] **Re-run the page audit** against the v0.9.3 UI; close or re-file the 07-11
      findings (the fixes shipped but were never verified as such).
- [ ] **Device validation matrix** (still never run on hardware): TalkBack, Switch
      Access, 200% font, reduced motion, contrast, focus order. Record per page.
- [ ] Close the last merge-verification gap: one end-to-end LLM tool call with a real key
      ("wake me at 7am" → `set_alarm` → alarm actually fires to the lock screen).
- [ ] H-07 check: confirm connector/messaging secrets are masked post-refactor (the
      settings split moved these surfaces; re-verify rather than assume).

**Verify:** OTA upgrade proven on device between two real versions; audit re-run shows
0 P0 and every High either closed with evidence or re-filed; `PROGRESS.md` current.
**Rollback:** docs and version metadata only; UI re-fixes are isolated commits.

### Phase B1 — The signature butler moment: the morning briefing · Effort: M

**Goal:** waking up to Jeeves is *the* demo. More pieces than ever exist — alarm pipeline,
`ButlerScript` (time + weather + sass), AI greeting, calendar sync, on-device TTS — but
the wake-up still doesn't tell you about *your day*.

- [ ] **Briefing composer** (`data/butler/BriefingComposer.kt`): today's calendar events
      (`CalendarTool` — note `CalendarSyncManager` already proves calendar access),
      weather (reuse `WeatherService`'s cache), pending todos (`TodoTool`), notes flagged
      for today, optionally 2-3 headlines (`WebSearchTool`) → a ≤ 90-second spoken script.
      Degrade gracefully: any missing permission/source is skipped, never blocks the alarm.
- [ ] Extend `ButlerAiProvider` with `generateBriefing(context: BriefingContext)`; keep
      the sass greeting at alarm-fire, speak the full briefing **after dismiss** (not
      while the user fights the snooze button).
- [ ] **Pre-generate at alarm-time minus ~15 min** via WorkManager so the briefing plays
      offline at wake; fall back to `ButlerScript.greeting()` (which already works
      offline via cached weather) if pre-generation failed.
- [ ] Mind the `PhonemeEncoder` lexicon constraint (`ButlerScript` header warning):
      LLM-generated briefing text goes through the letter-rule fallback — evaluate
      pronunciation quality; extend the lexicon generator if needed.
- [ ] Briefing sections + sass + honorific configurable in the existing Alarms settings page.
- [ ] **Evening wind-down** (cron template, off by default): tomorrow's first event,
      unfinished todos, "anything to note before bed?".

**Verify:** on device — alarm fires, dismiss, Jeeves speaks calendar + weather + todos via
ONNX TTS with the network off at wake time (pre-generated path proven). Unit tests:
composer section-skipping, pre-generation staleness (regenerate if > N hours old).
**Rollback:** feature-flag; alarm reverts to current greeting behavior.

### Phase B2 — Ambient access: one gesture to Jeeves · Effort: M

**Goal:** Jeeves is reachable the way Google Assistant is — without hunting for an icon.

- [ ] **Digital-assistant role:** `android.intent.action.ASSIST` +
      `VoiceInteractionService` so Jeeves can be the device assistant (long-press
      power / corner-swipe → chat with voice armed).
- [ ] **Quick-settings tile:** one tap → voice capture → reply spoken via `ButlerSpeech`.
- [ ] **Home-screen widgets:** briefing summary + tap-to-chat; "quick jot" into Notes.
- [ ] **Share-sheet target on the agent** (Notes already handles ACTION_SEND): share any
      text/URL → "summarize / save as note / remind me about this".
- [ ] **App shortcuts** (long-press icon): New note · Set alarm · Ask Jeeves · Briefing.
- [ ] **Notification quick-reply:** agent replies as messaging-style notifications with
      inline reply (foreground-service notification channel already exists).
- [ ] Voice latency pass: stream TTS sentence-by-sentence instead of whole-text
      synthesis in `ButlerSpeech`.

**Verify:** lock screen → spoken answer in ≤ 2 gestures, measured; each entry point
smoke-tested; Jeeves appears in Android's default-assistant picker.
**Rollback:** every entry point is an isolated manifest/UI commit.

### Phase B3 — One memory: the agent knows what you know · Effort: L

**Goal:** fix the deliberate Phase 5 skip. A butler that files your notes but can't
recall them is a filing clerk.

- [ ] **`search_notes` tool** backed by `NoteRepository` through the existing
      `FeatureBridge` (3-step wiring rule: `ToolsModule` + `AgentToolAccess` + prompts).
- [ ] **Index notes into the RAG pipeline:** on note save/sync, chunk + embed into the
      vector store alongside conversation memory, tagged by source; one relevance scorer
      across chat memory AND notes. (Index, don't migrate — the two Room DBs stay.)
- [ ] **Relevance-scored cross-session injection** (gap analysis §2) instead of
      blanket injection.
- [ ] **LLM conversation summarization** on session close, feeding the user model.
- [ ] **Habit signals into the user model:** wake times from `AlarmStore`, note topics,
      recurring cron tasks — "you usually wake at 6:30 on weekdays" is butler knowledge.
- [ ] Privacy stance stated in-app: everything stays on-device; only the composed prompt
      goes to the configured LLM endpoint.

**Verify:** a question answerable only from a note gets the right answer with the note
cited. Unit tests: scorer ordering, index lifecycle (edit → re-index, delete → evict).
Full suite (250+) stays green.
**Rollback:** the index is derived data; drop it and the tool, nothing user-owned lost.

### Phase B4 — Proactive engine: anticipate, don't just answer · Effort: L

**Goal:** Jeeves initiates — within a strict annoyance budget.

- [ ] **Trigger framework** generalizing `CronScheduler`: time (exists), charging/battery,
      calendar-event proximity ("meeting in 15 min" — calendar read already proven),
      geofence (optional, permission-gated), notification events (below).
- [ ] **NotificationListenerService** (opt-in, its own consent screen following the new
      lazy-permission pattern): digest noisy apps, surface "this looks important", never
      auto-act on notification content (prompt-injection boundary: notification text is
      data, not instructions — route through the skills-guard scrubber).
- [ ] **Commitment tracking:** the consolidator already extracts facts — route "remind
      me / I need to…" statements to `TodoTool` + a follow-up nudge trigger.
- [ ] **Daily digest** cron template: notification summary + todo deltas + tomorrow
      preview, one notification (and Telegram if configured).
- [ ] **Proactivity budget:** hard caps (max N pings/day), quiet hours honoring DND,
      one-tap "less of this" feedback stored to the user model.

**Verify:** each trigger fires in a device test; digest renders from real notification
data; budget enforcement unit-tested (N+1th ping suppressed); consent flow matches the
lazy-permission pattern.
**Rollback:** every trigger type behind its own toggle, default off except time-based.

### Phase B5 — Autonomy with accountability · Effort: L

**Goal:** "Jeeves, research X and file a note" works while the phone is pocketed — and
you can always see what he did.

- [ ] **Finish delegation:** background subagents on isolated `CoroutineScope`s with
      *filtered* `ToolRegistry` subsets (gap analysis §3); `DelegateTool` dispatches long
      tasks to WorkManager; results delivered as chat message + notification.
- [ ] **Approval gates:** classify tools safe / confirm / never-autonomous. `ShellTool`,
      `TermuxTool`, SMS, `DeviceSettingsTool` require in-notification approval when
      invoked from background/proactive contexts; interactive chat keeps current behavior.
- [ ] **Activity ledger:** surface `util/audit` as a user-visible "What Jeeves did"
      screen — every background tool call, timestamped, with outcome.
- [ ] **Kanban as the long-task queue** (repo + UI already exist): background jobs are
      tickets with states; failures auto-block after N retries instead of silent loops.

**Verify:** e2e — delegate a research task, kill the UI, receive the completed note +
notification; a background shell call blocks on an approval notification (unit + device
test); ledger shows the full trace.
**Rollback:** delegation stays chat-foreground-only if backgrounding proves unstable.

### Phase B6 — Reach & interop · Effort: M

**Goal:** Jeeves answers wherever you are, not just on the phone.

- [ ] Discord and WhatsApp connectors (Telegram is the template), per-platform session
      isolation, message fanout.
- [ ] **Provider registry + credential pooling** (gap analysis §7): multiple keys,
      rotation, OAuth device flow where supported.
- [ ] **Intent/Tasker API:** documented broadcast/intent surface for device automation,
      guarded by the local API token.
- [ ] Local API server: document + example scripts; it's the desktop bridge.

**Verify:** message Jeeves from Discord, get a reply with tool use; two providers with
rotation exercised in tests; a Tasker task triggers a briefing.
**Rollback:** connectors are additive modules behind settings toggles.

### Phase B7 — Fit & finish · Effort: M

**Goal:** the install funnel and daily feel match the capability.

- [ ] **Base TTS model download-on-first-use:** extend the `VoiceDownloader` pattern to
      the bundled 89 MB `kokoro-v1.0.int8.onnx` + `voices-v1.0.bin` → ~30 MB APK.
      **Add checksum verification + resume** (today's `VoiceDownloader` verifies nothing
      it downloads); platform-TTS fallback until installed (already exists in `TtsTool`).
- [ ] Sweep the audit re-run's surviving items (B0 tells us which of H-04/05/09…14
      actually remain post-v0.9.3 — don't assume the old list).
- [ ] Persona polish: one voice/tone across host/Notes/Alarms copy; expressive-eyes
      consistency; honorifics everywhere.
- [ ] Performance pass: cold-start budget; wire `ButlerSpeech.release()` to
      `MemoryPressureMonitor` (deferred note in `PROGRESS.md`); battery audit of the
      foreground service + B4 triggers.

**Verify:** clean-install funnel on a real device from the GitHub release: install
~30 MB APK → onboard → voice pack downloads with verified checksum → briefing works.
**Rollback:** size work is packaging-only; polish items are isolated commits.

---

## 4. Sequencing rationale & risks

**Why this order:** B0 is small now but non-negotiable — a broken version comparison can
strand every installed copy off the update channel, and unverified fixes rot. B1 is the
identity feature and mostly wires existing parts (`ButlerScript` + `WeatherService` +
`CalendarSyncManager` + `ButlerSpeech`). B2 multiplies usage of everything after it. B3
before B4 because proactive messages are only smart if memory is unified. B5 needs B3's
context and the consent patterns. B6/B7 are expansion and polish on a proven core.

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Shipped APKs all carry versionCode 60 → OTA can't upgrade them | High | B0 first item; verify upgrade path on device before the next feature release |
| NotificationListener / assist role are sensitive permissions | High | Reuse the shipped lazy-permission pattern; default off |
| Proactivity becomes annoyance | High | Budget + quiet hours + one-tap feedback shipped WITH B4, not after |
| Prompt injection via notifications/notes into an agent with shell access | High | B5 approval gates land before/with B4 listener; skills-guard scrubs third-party text; background contexts never get `ShellTool` unattended |
| Unverified voice-pack downloads (existing `VoiceDownloader` has no checksum) | Medium | B7 adds checksum + resume before extending the pattern to the base model |
| LLM briefing text vs. `PhonemeEncoder` lexicon → degraded pronunciation | Medium | B1 evaluates fallback quality; extend the lexicon generator if needed |
| Background work vs. Doze/battery | Medium | WorkManager constraints; pre-generation pattern instead of exact-time network work; battery audit in B7 |
| Scope creep toward desktop parity | Medium | Gap-analysis items enter only via a phase's need |

**Standing rules carried forward:** 3-step tool wiring (`ToolsModule` + `AgentToolAccess`
+ system prompts); commit per discrete step; update `PROGRESS.md`/`STATE.md` per phase;
never move `hermes-release.jks`; verify signer `99255c31…` before every release; every
version bump ships a signed `--latest` GitHub release **with the version actually bumped
in `gradle.properties`**.

## 5. Success metrics

| Quality | Metric | Today (v0.9.3) | Target |
|---------|--------|----------------|--------|
| Trustworthy | Audit P0 / High findings **verified on device** | Fixes shipped, unverified | 0 / 0, evidence recorded |
| Updatable | OTA upgrade between real versions proven | Broken (versionCode stuck at 60) | Proven on device |
| Proactive | Useful proactive touches/day (briefing, digest, nudges) | Wake greeting only | 2–4 within budget |
| Ambient | Gestures from lock screen to spoken answer | ~4 | ≤ 2 |
| Knows you | Agent answers from notes/habits with citation | No | Yes (B3 gate) |
| Autonomous | Background task completes with ledger + approval gating | Partial | Yes (B5 gate) |
| Installable | Release APK size | 117 MB (arm64) | ~30 MB + verified voice pack |
