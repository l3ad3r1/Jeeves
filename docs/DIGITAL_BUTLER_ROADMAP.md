# Jeeves Roadmap — Features & Updates from v0.9.6 to 1.0

**Status:** Plan of record, revised against **v0.9.6** (2026-07-11).
Successor to `SUPER_APP_ROADMAP.md` (merge — complete) and the v0.9.3 revision of this
document. Each milestone below is a shippable release with a hard verification gate;
work top-to-bottom, don't batch milestones.

**The question this roadmap answers:** Jeeves is now *one product* — one brand, one
theme, one settings store, one backup, one update channel, published and updatable.
What's left is making it a *butler*: proactive, ambient, knowing, and accountable.

---

## 1. Baseline — where v0.9.6 stands (don't re-plan these)

Shipped and verified as of today:

- **One app, published:** `l3ad3r1/Jeeves`, releases v0.9.0 → v0.9.6, single arm64
  APK (~122 MB), OTA channel working with a real numeric version compare, version
  drift fixed (bump-per-release rule enforced twice now).
- **One backup:** GitHub Gist bundle covers memories, skills, settings, crons, notes,
  and alarms; restore schedules alarms, dedupes notes, and reports counts.
- **Agent core:** 5 role agents, 22 tools (incl. cross-module `create_note`,
  `set_alarm`, Butler-voiced `speak`), RAG + dual-store memory, skills + evolution
  workers, cron scheduler, local OpenAI-compatible API server, Telegram + webhooks,
  SSH/Termux.
- **Daybook:** clock/weather/calendar/alarms as separate cards; calendar read + alarm
  → calendar mirroring; on-device ONNX TTS wake-ups with sass tiers.
- **Notes:** folder drawer, editor with undo/redo + IME-safe toolbar, hashtag tags,
  NotebookLM actions (summary/flashcards/audio/chat) with real error surfacing.
- **Fixed this cycle:** invisible-icon regression, silent alarm restore, NotebookLM
  silent failures, note-restore duplication, clock baseline, screen-off download stall.

### Debt carried into this roadmap (from `REVIEW_ANTIGRAVITY_2026-07-11.md` + audits)

| Item | Severity |
|---|---|
| Backup/restore never exercised with a real PAT end-to-end | Verify |
| Device-validation matrix (TalkBack, Switch Access, 200% font, reduced motion) never run | Verify |
| End-to-end LLM tool call ("wake me at 7am" → alarm fires) never proven with a real key | Verify |
| Memory restore duplicates on repeat (M3) | Medium |
| OTA download dies if user leaves the Updates screen (keep-screen-on is foreground-only) | Medium |
| Editor "Options" (⋮) button is a dead TODO (L4) | Low |
| Restored alarms not mirrored to calendar (L5); note timestamps lost in backup (L6); title edits not undoable (L7) | Low |
| `VoiceDownloader` verifies no checksum on downloaded voices | Low (grows with B7) |

---

## 2. North star

A butler is judged on five qualities, in order:

1. **Trustworthy** — never surprises you with a permission, a deletion, or a leaked secret.
2. **Proactive** — briefs you in the morning, nudges you about commitments, digests noise.
3. **Ambient** — reachable in ≤ 1 gesture from anywhere on the device.
4. **Knows you** — one unified memory across chat, notes, alarms, and habits.
5. **Acts autonomously, accountably** — does multi-step work in the background, shows
   receipts, asks before anything irreversible.

Principle for scope calls: **butler value beats desktop parity.** Items from
`FEATURE_GAP_ANALYSIS.md` (kanban dispatcher, CLI slash commands, Slack) enter only when
a milestone below needs them.

---

## 3. Milestones

### v0.9.7 — Hardening & debt (Effort: S–M)

**Goal:** clear the review debt and prove the things that have only ever been claimed.

- [ ] **OTA download → foreground service** (`dataSync` type already declared): survives
      screen-off AND navigation away; notification shows progress. Supersedes the
      keep-screen-on stopgap.
- [ ] **Memory restore dedupe** (exact-content skip, same shape as the note fix).
- [ ] Backup schema v4: carry note timestamps (L6); mirror restored alarms to calendar (L5).
- [ ] Wire or remove the editor "Options" button (L4); title edits in the undo stack (L7).
- [ ] **Verification sweep, on a real device:** backup+restore round-trip with a real PAT;
      "wake me at 7am" e2e through the LLM with a real key; the accessibility matrix
      (TalkBack, Switch Access, 200% font, reduced motion) — record results per page.
- [ ] Delete the orphaned v0.9.5 GitHub release (ships the invisible-icon build).

**Gate:** every "Verify" row in the debt table above flips to evidence recorded in
`PROGRESS.md`; release cut.

### v0.10 — The signature moment: the morning briefing (Effort: M)

**Goal:** waking up to Jeeves is *the* demo. Every ingredient already exists — alarm
pipeline, LLM bridge (`ButlerAiProviderImpl`), cached weather, calendar read, on-device
TTS — but the wake-up still doesn't tell you about *your day*.

- [ ] **`BriefingComposer`:** today's calendar + weather (cached) + pending todos + notes
      flagged today + optional 2-3 headlines (`WebSearchTool`) → ≤ 90-second spoken
      script. Any missing source is skipped, never blocks the alarm.
- [ ] `ButlerAiProvider.generateBriefing(context)`; sass greeting at alarm-fire, full
      briefing speaks **after dismiss**.
- [ ] **Pre-generate at alarm-minus-15-min** via WorkManager so the briefing plays
      offline at wake; fall back to today's `ButlerScript.greeting()` if it failed.
- [ ] Watch the `PhonemeEncoder` lexicon constraint: LLM text hits the letter-rule
      fallback — evaluate pronunciation, extend the lexicon generator if needed.
- [ ] Briefing sections/sass/honorific configurable in Daybook settings; **evening
      wind-down** cron template (off by default).

**Gate (device):** alarm fires with network OFF → dismiss → Jeeves speaks calendar +
weather + todos via ONNX TTS (pre-generated path proven). Unit: composer section-skip,
staleness regeneration.

### v0.11 — Ambient access: one gesture to Jeeves (Effort: M)

**Goal:** reachable the way Google Assistant is — without hunting for an icon.

- [ ] **Digital-assistant role** (`ACTION_ASSIST` + `VoiceInteractionService`):
      long-press power / corner-swipe opens chat with voice armed.
- [ ] **Quick-settings tile:** tap → voice capture → spoken reply via `ButlerSpeech`.
- [ ] **Widgets:** briefing summary + tap-to-chat; "quick jot" straight into Notes.
- [ ] **Share-sheet target on the agent:** share text/URL → summarize / save note /
      remind me.
- [ ] **App shortcuts** (long-press icon): New note · Set alarm · Ask Jeeves · Briefing.
- [ ] **Notification quick-reply:** agent replies as messaging-style notifications with
      inline reply.
- [ ] **Voice latency:** stream TTS sentence-by-sentence instead of whole-reply synthesis.

**Gate (device):** lock screen → spoken answer in ≤ 2 gestures, measured; Jeeves appears
in Android's default-assistant picker; every entry point smoke-tested.

### v0.12 — One memory: the agent knows what you know (Effort: L)

**Goal:** a butler that files your notes but can't recall them is a filing clerk.

- [ ] **`search_notes` tool** via the existing `FeatureBridge` (3-step wiring rule:
      `ToolsModule` + `AgentToolAccess` + system prompts).
- [ ] **Index notes into the RAG pipeline** on save/sync (chunk + embed, tagged by
      source); one relevance scorer across chat memory AND notes. Index, don't migrate —
      the two Room DBs stay.
- [ ] **Relevance-scored cross-session injection** instead of blanket injection.
- [ ] **LLM conversation summarization** on session close, feeding the user model.
- [ ] **Habit signals:** wake times from `AlarmStore`, note topics, recurring crons —
      "you usually wake at 6:30 on weekdays" is butler knowledge.
- [ ] Privacy stance in-app: all on-device; only the composed prompt reaches the
      configured LLM endpoint.

**Gate:** a question answerable only from a note gets the right answer with the note
cited; index lifecycle unit-tested (edit → re-index, delete → evict); suite stays green.

### v0.13 — Proactive engine: anticipate, don't just answer (Effort: L)

**Goal:** Jeeves initiates — inside a strict annoyance budget.

- [ ] **Trigger framework** generalizing `CronScheduler`: time, charging/battery,
      calendar-event proximity ("meeting in 15 min"), optional geofence.
- [ ] **NotificationListenerService** (opt-in, own consent screen): digest noisy apps,
      surface "looks important". Notification text is DATA, not instructions — through
      the skills-guard scrubber; never auto-act on it.
- [ ] **Commitment tracking:** consolidator-extracted "remind me / I need to…" →
      `TodoTool` + follow-up nudge.
- [ ] **Daily digest** cron template: notifications + todo deltas + tomorrow preview.
- [ ] **Proactivity budget:** hard cap N pings/day, quiet hours honoring DND, one-tap
      "less of this" feedback into the user model.

**Gate (device):** each trigger fires; digest renders from real notifications; budget
suppression unit-tested (N+1th ping suppressed); every trigger type has its own toggle,
default off except time-based.

### v0.14 — Autonomy with accountability (Effort: L)

**Goal:** "research X and file a note" works with the phone pocketed — and you can
always see what he did.

- [ ] **Background delegation:** subagents on isolated scopes with *filtered*
      `ToolRegistry` subsets; `DelegateTool` → WorkManager for long tasks; results
      delivered as chat message + notification.
- [ ] **Approval gates:** tools classified safe / confirm / never-autonomous.
      `ShellTool`, `TermuxTool`, SMS, `DeviceSettingsTool` require in-notification
      approval from background/proactive contexts.
- [ ] **Activity ledger:** `util/audit` surfaced as a "What Jeeves did" screen — every
      background tool call, timestamped, with outcome.
- [ ] **Kanban as the long-task queue** (repo + UI already exist): jobs are tickets;
      failures auto-block after N retries instead of silent loops.

**Gate (device):** delegate a research task, kill the UI, receive the completed note +
notification; a background shell call blocks on approval; ledger shows the full trace.

### v0.15 — Reach & interop (Effort: M)

**Goal:** Jeeves answers wherever you are, not just on the phone.

- [ ] Discord and WhatsApp connectors (Telegram is the template); per-platform session
      isolation; message fanout.
- [ ] **Provider registry + credential pooling:** multiple keys, rotation, OAuth device
      flow where supported.
- [ ] **Intent/Tasker API** guarded by the local API token; document the local API
      server with example scripts (the desktop bridge).

**Gate:** message from Discord → reply with tool use; two providers with rotation
exercised in tests; a Tasker task triggers a briefing.

### v1.0 — Fit & finish (Effort: M)

**Goal:** the install funnel and daily feel match the capability.

- [ ] **Base TTS model download-on-first-use:** extend the `VoiceDownloader` pattern to
      the bundled 89 MB model → **~30 MB APK**. Checksum verification + resume required
      (today's downloader verifies nothing); platform-TTS fallback until installed.
- [ ] Audit re-run + sweep of surviving H-items (don't assume the old list — re-audit
      against what actually shipped in 0.9.x).
- [ ] Persona polish: one voice/tone across host/Notes/Daybook copy; expressive-eyes
      consistency; honorifics everywhere.
- [ ] Performance pass: cold-start budget; `ButlerSpeech.release()` wired to
      `MemoryPressureMonitor`; battery audit of the foreground services + triggers.

**Gate (device):** clean install from GitHub release: ~30 MB APK → onboard → verified
voice-pack download → briefing works; audit shows 0 High findings.

---

## 4. Sequencing rationale & risks

**Why this order:** 0.9.7 is small but non-negotiable — unverified claims rot (this
cycle proved it twice: the "fixed" consent flow that wasn't, and the versionCode that sat
still through three releases). 0.10 is the identity feature and mostly wires existing
parts. 0.11 multiplies usage of everything after it. 0.12 before 0.13 because proactive
messages are only smart if memory is unified. 0.14 needs 0.12's context and the consent
patterns. 0.15/1.0 are expansion and polish on a proven core.

| Risk | Impact | Mitigation |
|---|---|---|
| Sensitive permissions (assistant role, notification access) mishandled | High | Reuse the shipped lazy-permission pattern; default off; own consent screens |
| Proactivity becomes annoyance | High | Budget + quiet hours + one-tap feedback ship WITH 0.13, not after |
| Prompt injection via notifications/notes into an agent with shell access | High | 0.14 approval gates land before/with the 0.13 listener; skills-guard scrubs third-party text; background contexts never get `ShellTool` unattended |
| Unverified downloads (voice packs, base model) | Medium | Checksum + resume are gate conditions for 1.0's size work |
| LLM briefing text vs. `PhonemeEncoder` lexicon → degraded pronunciation | Medium | 0.10 evaluates the fallback; extend the lexicon generator if needed |
| Background work vs. Doze | Medium | Foreground service for downloads (0.9.7); pre-generation instead of exact-time network (0.10); battery audit (1.0) |
| Multi-agent tools (Antigravity et al.) committing unreviewed regressions | Medium | Every external contribution gets a review pass before release — this cycle's review caught 1 critical + 2 high |
| Scope creep toward desktop parity | Medium | Gap-analysis items enter only via a milestone's need |

**Standing rules:** 3-step tool wiring (`ToolsModule` + `AgentToolAccess` + prompts);
commit per discrete step; update `PROGRESS.md` per milestone; never move
`hermes-release.jks`; verify signer `99255c31…` before every release; every release bumps
`gradle.properties` **in the same commit set** and ships a signed `--latest` GitHub
release.

## 5. Success metrics

| Quality | Metric | v0.9.6 today | 1.0 target |
|---|---|---|---|
| Trustworthy | Audit P0/High findings verified on device | Fixes shipped, matrix never run | 0 / 0, evidence recorded |
| Proactive | Useful proactive touches/day | Wake greeting only | 2–4 within budget |
| Ambient | Gestures from lock screen to spoken answer | ~4 | ≤ 2 |
| Knows you | Agent answers from notes/habits with citation | No | Yes (0.12 gate) |
| Autonomous | Background task with ledger + approval gating | Partial | Yes (0.14 gate) |
| Installable | Release APK size | 122 MB | ~30 MB + verified voice pack |
| Reliable | Backup round-trip proven with real PAT | Never | Routinely, per release |
