# Jeeves audit remediation plan — 2026-07-14

**Status:** implementation in progress. Findings remain open until a separate review pass
closes them. Device-only behavior stays `UNVERIFIED` until exercised on hardware.

**2026-07-14 checkpoint:** the privacy/backup/delegation foundation, confirmation
serialization and lifecycle events, plugin instruction budget, native error/accounting
fixes, prompt-format/native-build preflight gates, and atomic local-model installation are
implemented and pass the full local preflight. Remaining work is explicitly retained below,
including standard-executor reuse for child agents, durable WorkManager download ownership,
full local conversation/tool support, event-sequence coverage, and ambient surfaces.

**2026-07-14 tranche 2 checkpoint:** child calls now reuse the standard executor; local
requests preserve a bounded role-labelled transcript and share textual tool-call parsing with
the cloud fallback; and model acquisition is owned by a resumable foreground WorkManager job
whose state survives process loss. Focused tests cover those boundaries. Hardware execution,
catalog checksums, the validated directory picker, orchestration event-sequence tests, and
ambient surfaces remain open.

## Principles and gates

- Privacy is enforced at shared data boundaries, not only at individual callers (L-009).
- Restore operations preserve identity/state and remain idempotent (L-005, L-006).
- User-visible operations surface actionable failures and always reset progress state (L-007).
- Human approval is keyed per request, times out, and fails closed in headless flows (L-008).
- Agent capabilities use explicit allowlists; new tools still require three-step wiring (L-014).
- Native changes are covered by an APK build, not Kotlin compilation alone (L-018, L-020).
- Host code passes raw prompts; GGUF/Jinja owns chat formatting (L-021).
- `bash tools/preflight.sh` must exit 0 before any commit. Device-only work is recorded as
  `UNVERIFIED` in `PROGRESS.md` and the commit message (L-001).

## Tranche 1 — privacy, backup, and delegated capabilities

### Private-note boundary

- Add repository methods that return only prompt-safe notes (`!locked && !encrypted`),
  including search and recent-note queries.
- Route `search_notes`, habit extraction, briefings, and future prompt/network consumers
  through those methods.
- Keep ordinary Jotter UI search unchanged: locked notes may remain visible as locked cards
  inside the local notes UI, but content must not cross into prompts, ambient speech, plugins,
  or network calls.
- Add regression coverage for locked and encrypted notes.

### Android and Gist backup

- Correct Android backup domains/paths for Preferences DataStore.
- Exclude the separate Jotter database and Keystore-bound encrypted preferences from cloud
  backup/device transfer.
- Advance the Gist backup schema while retaining backward-compatible defaults.
- Preserve note repository identity, trash state, encryption metadata, timestamps, and sync
  metadata required for a faithful restore.
- Do not resurrect trashed notes. Keep repeat restores idempotent.
- Decide and document the trust boundary for API keys and locked notes. Until payload-level
  encryption exists, exclude secrets and private-note content from new backups rather than
  representing a private Gist as end-to-end encryption.
- Add backup encode/decode and restore regression tests.

### Delegated agents

- Replace the negative child-tool blocklist with a small explicit read-only allowlist.
- Execute child calls through the standard executor so authorization, confirmation policy,
  redaction, telemetry, and error shaping cannot drift.
- Add a test proving write/scheduling/device tools never reach a child agent.

## Tranche 2 — truthful orchestration and approval

- Key confirmation requests by tool-call/request id so concurrent turns cannot overwrite one
  another; retain the 60-second deny timeout and headless safe default.
- Emit `ToolCallResult` for allowed, denied, failed, and unauthorized calls.
- Make tool cards reach terminal states and persist with the completed turn where appropriate.
- Replace the fictional `step-0` dependency with real execution-step ids or remove unused
  dependency state.
- Restore actual token streaming where providers support it; do not label a whole response as
  a token.
- Add concurrent confirmation and event-sequence tests.

## Tranche 3 — on-device agent and native inference

- Preserve the full supported conversation sequence instead of only the first system and last
  user message.
- Parse the same textual tool-call envelope used by cloud-compatible providers, or explicitly
  report that the selected local model cannot use tools.
- Propagate native prompt-processing failures and move the engine to an actionable error/retry
  state rather than returning an empty success.
- Correct prompt token accounting after truncation and generation stop-position math.
- Remove the unused Kotlin `PromptFormatter` hard-coded tokens and enforce L-021 in preflight.
- Define an explicit fallback for metadata-less custom GGUF models.
- Add provider-context/tool tests and an APK/native build gate. Real GGUF inference remains
  `UNVERIFIED` until exercised on a device.

## Tranche 4 — durable model acquisition

- Move download ownership to WorkManager or a foreground service and persist the download id.
- Recover state after process death and stop polling when DownloadManager has no matching row.
- Validate destination writability and available space before enqueue.
- Download/copy to a temporary filename, verify expected size and preferably SHA-256, then
  atomically promote it. Never treat any non-empty partial file as ready.
- Close every cursor with `use`, reset state in `finally`, and surface actionable errors.
- Replace broad raw-path editing with a validated directory picker where feasible.
- Add tests for enqueue/query failures, interrupted copies, invalid custom URIs, and recovery.

## Tranche 5 — ambient surfaces and security claims

- Either finish Ask Jeeves, briefing playback, share actions, voice start, notification reply,
  and Quick Jot navigation, or default each unverified surface off behind a capability flag.
- Add intent-routing tests and retain hardware smoke gates for widgets, tile, assist role, and
  notification replies.
- Change the Settings security panel from hard-coded assertions to evidence-backed states.
- Mark remote community JavaScript execution accurately and repair the Rhino instruction
  budget using a cumulative counter/deadline.
- Add plugin sandbox tests, including an infinite-loop termination test.

## Tranche 6 — repository gates and architecture debt

- Extend preflight with `:app:assembleDebug`, L-021 prompt-tag detection, and feature-module
  coverage for personal literals and privacy-sensitive call sites.
- Update the stated test count dynamically or remove hard-coded counts.
- Re-baseline the roadmap, progress log, and UI audit without marking review-owned findings
  closed.
- Decompose `NoteViewModel` infrastructure construction into injected repositories/services.
- Split `NoteApp` by screen/feature and split `SettingsViewModel` by settings domain.
- Move Android/data-layer dependencies out of domain packages behind focused interfaces.

## Verification sequence

1. Focused unit tests for each tranche.
2. `bash tools/preflight.sh` with exit code 0.
3. Clean `:app:assembleDebug` for native/resource/manifest coverage.
4. Full diff review against every lesson check.
5. `PROGRESS.md` newest-first entry separating `VERIFIED` from `UNVERIFIED`.
6. Commit one logical tranche at a time; push and watch CI to a successful conclusion.
7. Hardware sweep for backup/restore, local GGUF, downloads, alarms, widgets, tile, voice,
   share target, notification reply, and accessibility. Findings remain open until review.
