# Jeeves Roadmap — Features & Updates from v0.9.9 to 1.0

**Status:** Plan of record, revised against **v0.9.9** (2026-07-11, third baseline today —
the codebase moved that fast). Predecessors: `SUPER_APP_ROADMAP.md` (merge, complete) and
the v0.9.6 revision of this file.

**What changed since the last revision:** the v0.9.6 plan sequenced features
v0.9.7 → v1.0. In one day, releases v0.9.7–v0.9.9 shipped code from FOUR of those
milestones out of order — the morning-briefing composer (was v0.10), the entire One
Memory layer (was v0.12), ambient-access scaffolding (was v0.11), and tool-confirmation
gates (was v0.14). Almost none of it has passed a device gate, and two of the three
releases shipped bugs a single manual test would have caught (v0.9.7's dead repo sync;
v0.9.5's invisible icon earlier).

**Therefore this revision inverts the priority: the bottleneck is no longer writing
features — two AI agents are producing them faster than they're being verified. The
bottleneck is verification, and the roadmap now treats it as the product.**

---

## 1. Baseline — what exists at v0.9.9

### Shipped and reviewed (code-verified; device status noted)

| Area | State |
|---|---|
| One app, one backup, one update channel | ✅ Mature. Unified gist backup (memories/skills/settings/crons/notes/alarms, schema v4), OTA via foreground service, version discipline holding since 0.9.4 |
| **One Memory** (old v0.12) | ✅ Code complete + reviewed: `search_notes` (3-step wired), `NoteIndexer` (live sync, evicts locked/trashed), relevance-scored injection, conversation summaries (fixed — was dead code), habit snapshots (fixed — was duplicating). **Device gate NOT run** |
| **Morning briefing** (old v0.10) | ⚠ `BriefingComposer` + `PreGenerateBriefingWorker` + settings shipped in v0.9.7. **Never heard on a device**; offline path, PhonemeEncoder pronunciation, post-dismiss timing all unproven |
| **Ambient access** (old v0.11) | ⚠ Scaffolding only: assist-role services, QS tile, 2 widgets, share target, shortcuts, notification quick-reply. Compiles; **flagged experimental; zero device testing** |
| **Autonomy gates** (old v0.14, partial) | ⚠ Tool allowlist enforced; Allow/Deny confirmation dialog with 60 s deny-timeout. No background delegation, no ledger yet |
| Security | ✅ All secrets (cloud/aux keys, PAT, API-server key, SSH password) keystore-encrypted at rest; PAT masked in UI |
| Trust & a11y | ✅ Per-permission onboarding, reduced-motion, screen-reader chat semantics, loading/error states. **Accessibility matrix still never run on hardware** |

### Debt ledger (every unverified claim, in one place)

| # | Item | Owed since |
|---|---|---|
| D1 | Backup+restore round-trip with a real PAT on device | v0.9.4 |
| D2 | E2E LLM tool call ("wake me at 7am" → alarm fires) with a real key | v0.9.0 |
| D3 | Accessibility matrix: TalkBack, Switch Access, 200% font, reduced motion | 07-10 audit |
| D4 | Briefing heard on a device (incl. network-off pre-generated path) | v0.9.7 |
| D5 | One Memory device gate: note-grounded answer with citation | v0.9.8 |
| D6 | Repo sync E2E with a `repo`-scoped token (picker fix shipped in 0.9.9; sync itself unproven) | v0.9.9 |
| D7 | Ambient surfaces: every entry point smoke-tested or feature-flagged off | v0.9.8 |
| D8 | Memory-restore dedupe (restore still duplicates memories) | R1 review |
| D9 | Confirmation service single-slot race (concurrent turns overwrite the pending request) | R2 review |
| D10 | Vault-repo pattern hardcoded as picker *ordering* heuristic → should be a setting | v0.9.9 |
| D11 | No CI — nothing runs the 252-test suite on push; unreviewed agent commits reach master unguarded | always |

---

## 2. North star (unchanged)

A butler is judged on five qualities, in order: **trustworthy** (no surprises),
**proactive** (briefs and nudges), **ambient** (≤ 1 gesture away), **knowing** (one
memory across chat/notes/alarms/habits), **accountable** (background work with receipts
and approval gates). Butler value beats desktop parity.

**New standing rule, earned twice today:** *no release without a review pass, and no
milestone claimed without its device gate.* Antigravity ships code fast and well — but
v0.9.5 and v0.9.7 both went out unreviewed and both shipped user-visible breakage.
Review is not overhead; it is currently the highest-value work in the project.

---

## 3. Milestones

### v0.10.0 — The Great Verification (Effort: M — mostly device time, not code)

**Goal:** every feature that exists either passes its gate on hardware or gets
feature-flagged off. The debt ledger empties. This milestone is deliberately
feature-frozen: the fastest way to a trustworthy butler right now is proving the one
we already built.

- [ ] **CI first (D11):** GitHub Action — `:app:testDebugUnitTest` + compile of all
      modules on every push to master. Agents can't merge red.
- [ ] **Device sweep, one session, results recorded in `PROGRESS.md`:**
      D1 real-PAT backup round-trip · D2 e2e alarm-by-chat · D4 briefing (with airplane
      mode at wake) · D5 note-grounded answer · D6 repo sync with a repo-scoped token ·
      D7 each ambient surface (tile, both widgets, share target, quick-reply, assist
      role in the system picker).
- [ ] **D3 accessibility matrix** per page; file or fix what it finds.
- [ ] Code debt while the device is out: D8 memory-restore dedupe, D9 confirmation
      race (queue or per-turn key), D10 vault pattern → Notes setting.
- [ ] Anything failing its gate ships OFF (settings toggle or build flag), not broken.

**Gate:** debt ledger empty or explicitly deferred with a toggle; CI green badge;
release cut.

### v0.11.0 — Ambient access, finished (Effort: M)

**Goal:** promote the scaffolding from "experimental" to the product's front door.

- [ ] Voice-interaction session UX: long-press-power opens chat with voice armed,
      spoken reply via `ButlerSpeech`; latency measured (sentence-streaming landed
      in 0.9.8 — verify it holds under real network).
- [ ] Widgets actually update: briefing widget refreshes after pre-generation;
      quick-jot round-trips into Notes.
- [ ] Tile → voice capture → spoken answer, from lock screen where policy allows.
- [ ] Share-sheet target: summarize / save note / remind flows complete.
- [ ] Polish pass on notification quick-reply threading.

**Gate (device):** lock screen → spoken answer ≤ 2 gestures, measured; every surface
demo-able cold.

### v0.12.0 — Proactive engine (Effort: L)

Unchanged in content from the previous revision (triggers framework, opt-in
notification digest with the injection boundary through the skills-guard, commitment
nudges via the consolidator, daily digest template) — **plus the annoyance budget ships
in the same release**: hard N-pings/day cap, DND-honoring quiet hours, one-tap "less of
this". Trigger types default off except time-based.

**Gate (device):** each trigger fires; budget suppression unit-tested; consent per
capability.

### v0.13.0 — Autonomy with accountability (Effort: L)

Builds on the 0.9.8 gates (allowlist + confirmation dialog + timeout):

- [ ] Background delegation: subagents on isolated scopes with filtered tool subsets;
      `DelegateTool` → WorkManager; results as chat message + notification.
- [ ] Context-aware confirmation policy: interactive chat = dialog (exists);
      background/proactive = notification-based approval; never-autonomous class for
      `ShellTool`/`TermuxTool`/SMS/device-settings from headless contexts.
- [ ] Activity ledger: `util/audit` surfaced as "What Jeeves did".
- [ ] Fix D9 properly here if deferred: per-request confirmation keys.

**Gate (device):** delegate → pocket the phone → completed note + notification; a
background shell call blocks on approval; ledger shows the trace.

### v0.14.0 — Reach & interop (Effort: M)

Unchanged: Discord + WhatsApp connectors (Telegram is the template), provider
registry + credential pooling, Tasker/intent surface behind the local API token,
documented local API examples.

**Gate:** Discord message → tool-using reply; two providers rotating in tests; Tasker
triggers a briefing.

### v1.0 — Fit & finish (Effort: M)

Unchanged: TTS base model download-on-first-use with **checksum + resume**
(~122 MB APK → ~30 MB), audit re-run to zero High findings, persona/copy unification,
cold-start + battery pass, `ButlerSpeech.release()` on memory pressure.

**Gate (device):** clean install ≤ 35 MB → onboard → verified voice pack → briefing
works; 0 High findings.

---

## 4. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| **Agents outrun verification** (the defining risk of this codebase — 2 broken releases in one day) | High | v0.10.0 is feature-frozen; CI gate (D11); standing review-before-release rule |
| Ambient surfaces advertised before they work | High | "Experimental" label stays until the 0.11 gate; flags default off |
| Prompt injection via notifications/notes into an agent with shell access | High | 0.13's context-aware policy lands before/with 0.12's listener; skills-guard scrubs third-party text |
| Proactivity becomes annoyance | High | Budget ships WITH 0.12, not after |
| Sensitive permissions (assist role, notification access) | Med | Per-capability consent pattern (shipped in 0.9.8's onboarding) reused everywhere |
| Unverified downloads | Med | Checksum+resume are 1.0 gate conditions |
| Hardcoded personal conventions creeping into product code ("second brain") | Med | D10; review flags any user-specific literal |

**Standing rules:** 3-step tool wiring; commit per step; `PROGRESS.md` per milestone;
never move `hermes-release.jks`; signer `99255c31…` verified before every release;
version bumped in `gradle.properties` in the same change-set as the release; **review
pass before any release; device gate before any milestone claim.**

## 5. Success metrics

| Quality | Metric | v0.9.9 today | 1.0 target |
|---|---|---|---|
| Verified | Debt-ledger items open | 11 | 0 |
| Trustworthy | Releases shipped with a user-visible regression (rolling) | 2 of last 5 | 0 |
| Proactive | Useful proactive touches/day | Briefing (unproven) | 2–4 in budget |
| Ambient | Lock screen → spoken answer | ~4 gestures | ≤ 2, measured |
| Knowing | Note-grounded answer with citation | Code yes, device unproven | Gate passed |
| Accountable | Background task with ledger + gated approval | Gates only | Full loop |
| Installable | Release APK | 122 MB | ~30 MB + voice pack |
