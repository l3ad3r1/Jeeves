# Code Review R2 — Antigravity's v0.9.7 release + uncommitted v0.12 work

**Date:** 2026-07-11 (second review of the day) · **Scope:** commits `a34d4cd`,
`1ac8392` (released as **v0.9.7**, already tagged Latest on GitHub) plus the large
**uncommitted** working tree (v0.12 "One Memory" + parts of v0.11 ambient access +
security hardening). **Verification:** compile + 252 unit tests green before AND after
the review fixes below.

---

## 1. Scorecard against the claims

### The 10 claimed UX fixes

| # | Claim | Verdict |
|---|-------|---------|
| 1 | Editor `imePadding` (UX-003) | ✅ Real |
| 2 | Toolbar `horizontalScroll` (UX-004) | ✅ Real |
| 3 | Per-permission onboarding cards (UX-001) | ✅ Real (uncommitted) |
| 4 | Repo-sync "second brain" filter | ❌ **BROKE repo sync entirely — see C1** |
| 5 | Chat screen-reader semantics (UX-006) | ✅ Real |
| 6 | Loading/error states (UX-011/013) | ✅ Real |
| 7 | fontScale + reduced motion (UX-008) | ⚠ Half-real: reduced-motion genuinely implemented (ExpressiveEyes, TypingIndicator vs `ANIMATOR_DURATION_SCALE`); **fontScale wiring doesn't exist anywhere in the tree** — harmless, since Compose `sp` already scales, but the claim is false |
| 8 | Embedded splash bypass (UX-009) | ✅ Real |
| 9 | Typography centralized in `:core:theme` (UX-010) | ✅ Real (was largely done in 0.9.2; consolidation extended) |
| 10 | v0.12 compilation errors fixed | ✅ Compiles, 252/0 |

### The 5 claimed architecture features

| # | Claim | Verdict |
|---|-------|---------|
| 1 | `SearchNotesTool` wired | ✅ Wired correctly — 3-step rule followed (ToolsModule + AgentToolAccess + both agents' prompts). Nuance: it's a **keyword** (Room LIKE) search, not the claimed "semantic vector search" — the vector path is the RAG injection, not this tool. Works fine. |
| 2 | `NoteIndexer` live indexing | ✅ Real, well-structured (skip-unmodified, evict-on-delete, started off the app-startup path with a failure guard) — but **indexed locked/private notes: see H2** |
| 3 | Relevance-scored cross-session context | ✅ Real — memory search now uses the actual user message (fixes the old empty-query search), RAG context injected with guards, plus an unclaimed but good **tool-allowlist enforcement** in the tool loop |
| 4 | Background conversation summarization | ❌ **Dead code as shipped — never executed once: see C2** |
| 5 | `HabitExtractor` nightly signals | ⚠ Works, internally guarded, but **duplicated memories every night: see M1** |

### Undisclosed changes found in the same tree (mostly good)

- **Keystore hardening:** aux API key, GitHub PAT, API-server key, and SSH password now
  encrypted at rest alongside the cloud key (previously plaintext DataStore). Migration
  is safe (decrypt falls back to the raw value). 👍
- **Tool-confirmation flow** (a v0.14 item, early): orchestrator now actually blocks on
  user approval with a real Allow/Deny dialog in chat — previously auto-approved. Good
  direction, one serious hole (H1).
- **v0.11 scaffolding:** voice-interaction service (assist role), QS tile, two widgets,
  share-sheet target, app shortcuts, notification quick-reply — manifest declarations
  look correct (bind permissions on exported services, reply receiver not exported).
  Not yet reviewed in depth or device-tested.
- **Sentence-streamed TTS** in chat + chunked synthesis in `ButlerSpeech` (latency win),
  with an offset bug (M2).
- `WebhookTool` gained a "local" platform that posts a reply-able notification.

**Overall:** dramatically better than the last drop — real security work, correct tool
wiring, failure guards in the right places. But the same signature flaw: **plausible
code that was never run.** The summarization feature cannot ever have executed, and the
repo filter cannot ever have matched the user's actual repo — one manual test of either
would have caught both.

---

## 2. Findings by severity (all fixed in this review unless noted)

### CRITICAL

**C1 — Repo sync broken by the "second brain" filter (SHIPPED in v0.9.7).**
`filter { it.contains("second brain", ignoreCase = true) }` — with a **space**. The
actual vault repo is `l3ad3r1/Dronehire-second-brain` — **hyphenated**. The filter
matches zero repositories, so the repo-sync picker is empty and repo sync is dead for
exactly the repo it was built around. Also hardcodes one user's naming into product code.
**Fixed:** separators normalized before matching (`[-_]` → space), so `…-second-brain`,
`…_second_brain`, and `…second brain` all match. Flagged with a TODO to become a setting.
*This is live in the released v0.9.7 APK — ship v0.9.8 promptly if repo sync matters.*

**C2 — "Background conversation summarization" never ran, not even once.**
The trigger is `viewModelScope.launch { … }` inside `ChatViewModel.onCleared()` —
androidx cancels `viewModelScope` **before** `onCleared()` executes, so the coroutine is
cancelled at birth. The flagship claim of the drop was dead on arrival.
**Fixed:** `summarizeConversation` is now fire-and-forget on the singleton repository's
own supervisor scope (interface de-suspended, call site is a plain call), with a
per-conversation message-count guard so closing/reopening an unchanged chat doesn't
write duplicate summaries.

### HIGH

**H1 — Tool confirmation hangs forever in headless contexts.**
`ToolConfirmationService.awaitConfirmation()` awaited a `CompletableDeferred` with no
timeout. The chat screen shows an Allow/Deny dialog, but turns also run where **no UI
exists** — Telegram connector, local API server, cron worker. A confirmation-requiring
tool there would deadlock the turn permanently.
**Fixed:** 60-second timeout, **deny** on expiry (the safe default for a tool that asked
for confirmation). Known residual: concurrent turns share one pending slot
(last-writer-wins) — acceptable while the orchestrator serializes turns; noted for 0.14.

**H2 — Locked/private notes indexed into the vector store.**
`NoteIndexer` indexed every active note, including `locked`/`encrypted` ones — the notes
the user explicitly put behind the biometric app-lock. Indexed content is injected into
system prompts and sent to the configured **cloud LLM**: a straight privacy leak of the
most-private notes.
**Fixed:** locked/encrypted notes are excluded from eligibility, and — because eviction
now keys off eligibility — locking a note **evicts** it from the index on the next sync.
Eviction logic also simplified (the old condition was a tautology).

### MEDIUM

**M1 — HabitExtractor appended a new "Habit insight" memory every night.**
No dedupe: ~30 near-identical memories/month polluting vector search.
**Fixed:** habit insights are a rolling snapshot — previous `Habit insight:` memories
are deleted before fresh ones are written (uses the existing `deleteMemory(id)` API).

**M2 — Sentence-streamed TTS offsets drifted.**
`joinToString(" ")` rebuilt spoken text with different separators than the source
stream, so `spokenTextLength` skewed and later chunks repeated or swallowed words.
**Fixed:** offsets now tracked against the real accumulated string via the last regex
boundary; advance by actual consumed length.

### LOW / notes (not fixed, tracked)

- `trashNotes` param in `NoteIndexer.sync` now unused (kept — it drives re-sync
  reactivity through `combine`).
- `OrchestratorImpl` unauthorized-tool error uses `errorMessage!!` — safe with the
  current `ToolResult.error` shape, brittle if that changes.
- The claimed fontScale wiring doesn't exist; Compose handles it natively. No action.
- v0.11 scaffolding (assist/tile/widgets/share/quick-reply) compiles but is
  **device-untested** — treat as alpha until the 0.11 milestone gate runs.
- v0.9.7 was released without the roadmap's review-first rule; C1 shipped as a result.

---

## 3. State after this review

- All fixes verified: compile green, **252 tests / 0 failures**.
- Working tree contains: Antigravity's v0.12 + ambient work **plus** this review's six
  fixes — committed together (see git log) so the tree ships reviewed-only.
- **Recommended next actions:** (1) cut **v0.9.8** soon — v0.9.7 is live with C1's
  broken repo sync; (2) device-test the ambient scaffolding before advertising it;
  (3) the roadmap's 0.9.7-milestone verification sweep (real-PAT backup, e2e alarm,
  accessibility matrix) is still owed.
