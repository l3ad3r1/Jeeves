# Code Review — Antigravity's v0.9.5 work + codebase sweep

**Date:** 2026-07-11 · **Scope:** commits `1d88108`, `5aca482`, `9f57163` (unpushed at
review time) against the four reported issues, plus a general defect sweep.
**Verification:** all three modules compile; unit suite 252 tests, 0 failures — both
before and after the review fixes described below.

---

## 1. Verdict on Antigravity's work, issue by issue

| # | Reported issue | Antigravity's result |
|---|----------------|---------------------|
| 1 | Hermes GitHub backup/restore doesn't work | **Not diagnosed.** No defect was identified (by it or by this review) in the gist request path itself — auth headers, create/PATCH/GET flow, and error hints are all correct. It unified the backup instead (see #2). The original failure is most likely environmental (PAT missing the `gist` scope, expired token, or network) — the UI does surface those errors. **Needs one on-device test with a real PAT to close.** |
| 2 | All three modules should back up together | **Done, with two real bugs.** `BackupData` schema v3 adds `notes` + `alarms`; backup collects from `NoteRepository` + `AlarmStore`; restore imports both and reports counts. Bugs found: restored alarms were never scheduled (HIGH-1) and note restore duplicated on every tap (MED-1) — both fixed in this review. |
| 3 | Editor: toolbar behind keyboard, dead undo/redo, tags, NotebookLM | **Three of four done.** `imePadding()` on the bottom bar is correct (Jotter runs edge-to-edge, so IME insets are delivered); undo/redo genuinely wired (two-stack snapshot model); tag/folder rows removed and the tag button re-wired to insert `#` at the cursor. **NotebookLM untouched** — the actual complaint. See HIGH-2. |
| 4 | Daybook clock alignment/size, remove Preview Wake-Up | **Half done.** Preview Wake-Up fully removed (UI + dead `previewWakeUp()` service code — clean). Seconds enlarged, but box-bottom alignment ≠ baseline alignment, so the misalignment the user reported was still there (MED-2); day/date block untouched. Fixed in this review. |

Also correct in its commits: version bumped to 65/0.9.5 (respects the drift rule from
v0.9.4), a stray indent fixed, and the icon geometry it committed was the
measured-from-reference vector. Overall: **competent on the mechanical items, but it
didn't run the app or reason about runtime behaviour** — every bug below is of the
"compiles fine, misbehaves live" kind, and it left the one investigative item (NotebookLM)
untouched while the commit message claimed the editor was fixed.

---

## 2. Findings, by severity

### CRITICAL

**C1 — Launcher icon invisible: cream mark on white background.**
`1d88108` changed `ic_launcher_background` from navy `#0B132B` to white `#FFFFFF` (to
match the black-on-white reference) but left the vector's fill at cream `#F5F0E6`.
Cream-on-white ≈ invisible: the launcher would show a blank white circle.
**Fixed:** foreground fill → near-black `#14181F` on the white background, matching the
reference artwork. (The regenerated `mipmap-*/ic_launcher_foreground.png` files it also
committed are dead resources — minSdk 29 means the anydpi-v26 adaptive XML always wins —
harmless but noted.)

### HIGH

**H1 — Restored alarms never ring.**
Restore called `AlarmStore.upsert()` only. Butler's own flow (and the repo's documented
`set_alarm` rule) is persist **and** `AlarmScheduler.schedule()`; the cron loop ten lines
up even re-enqueues WorkManager jobs for exactly this reason. As committed, a restored
enabled alarm stays silent until the next reboot (when `AlarmReceiver` re-registers) or a
manual toggle.
**Fixed:** restore now schedules each enabled alarm via `AlarmScheduler(context)`.

**H2 — NotebookLM actions fail silently (the user's actual complaint).**
The actions *are* wired end-to-end (`NoteApp` → `NoteViewModel` → Hilt entry point →
`JotterAiProviderImpl` → `CloudLlmProvider`). The failure mode: `aiProvider ?: return`
made the buttons no-ops when the entry point lookup failed, and no `try/catch` around the
LLM stream meant any error (no API key, offline, HTTP 401) either left the "AI is
thinking…" dialog stuck forever or crashed the ViewModel scope. If the Cloud LLM isn't
configured, every action "does nothing" — precisely what was reported.
**Fixed:** all four actions run through a shared harness that surfaces a null provider as
an actionable message ("configure a Cloud LLM in Jeeves Settings → Assistant"), catches
stream failures into the result dialog, and always resets the in-progress flag.

### MEDIUM

**M1 — Note restore duplicated every note on each tap.**
`NoteEntity.id` is auto-generate; `insertNote()` always creates a new row, so Restore was
not idempotent (cron restore, by contrast, upserts by id).
**Fixed:** restore now skips notes that already exist, matched by `gistId` when present,
else exact title+content.

**M2 — Clock seconds still misaligned after the "fix".**
`Alignment.Bottom` aligns text *boxes*; with different font sizes the larger text's
descent pushes its baseline up, so the seconds visibly sag — the complaint survived the
commit. **Fixed:** both time texts use `alignByBaseline()`; seconds sized
`headlineMedium`; day/date bumped to `labelMedium`/`titleMedium` per the size complaint.

**M3 — Memory restore is not idempotent (pre-existing, same class as M1).**
`memoryRepository.addMemory(content)` appends unconditionally — restoring twice
duplicates every memory. Not introduced by Antigravity; left unfixed here because dedupe
semantics for memories (similarity vs. exact match) deserve a deliberate decision.
**Recommendation:** exact-content skip, same shape as the note fix.

### LOW

- **L1 — Unbounded undo stack.** One full-text snapshot per keystroke → O(n²) memory
  growth over a long session. **Fixed:** capped at 100 steps (`takeLast`).
- **L2 — Backup card description stale** — didn't mention notes/alarms. **Fixed.**
- **L3 — Unused import** `PlayCircleOutline` left behind by the Preview removal. **Fixed.**
- **L4 — "Options" (⋮) button in the editor top bar is still a dead `TODO`** — same
  class of dead control the UX audit flagged (H-09). Left open: wire it or remove it.
- **L5 — Restored alarms aren't mirrored to the calendar** (`CalendarSyncManager` is only
  invoked from Butler's own UI paths). Cosmetic inconsistency; left open.
- **L6 — Restored notes lose timestamps** (`lastModifiedLocally` resets to "now"); the
  backup schema doesn't carry creation dates. Left open; add to schema v4 if it matters.
- **L7 — Title edits aren't captured in the undo stack** (content only). Left open.

### Pre-existing items re-confirmed during the sweep (no change)

- `VoiceDownloader` still verifies nothing it downloads (roadmap B7 owns this).
- Device-validation matrix (TalkBack, 200 % font, reduced motion) still never run
  (roadmap B0).
- `OtaUpdateChecker.isNewer` is a proper numeric semver compare — with the version now
  actually bumped per release, OTA detection is sound. PAT field is properly masked
  (`PasswordVisualTransformation`) — the old H-07 concern doesn't apply here.

---

## 3. What shipped in this review's fix commit

C1, H1, H2, M1, M2, L1, L2, L3 — verified by full compile of `:app`,
`:feature:jotter`, `:feature:butler` and the 252-test unit suite (0 failures).

**Still open / needs a device:** one real-PAT backup+restore round-trip (issue 1's
close-out), M3, L4–L7, and an on-device look at the corrected icon before cutting the
v0.9.5 release that Antigravity's version bump has already staged.
