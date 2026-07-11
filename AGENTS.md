# AGENTS.md — working rules for AI agents in this repo

You are building **Jeeves**, a merged Android super-app (agent + notes + alarms).
These rules exist because agent-written code here has repeatedly been *plausible but
never run* — two of five recent releases shipped user-visible breakage a single manual
test would have caught. Follow the loop below and that stops.

## The self-learning loop (mandatory, every session)

**1. BEFORE writing code — load the ledger.**
Read, in order:
- `docs/agents/LESSONS.md` — every defect class this repo has already paid for.
- `PROGRESS.md` (top entries) — current state; it is the source of truth, not your
  memory of the codebase.
- `docs/DIGITAL_BUTLER_ROADMAP.md` — what the current milestone is; don't build ahead
  of it without being asked.
In your first output, name the lesson IDs that apply to the task (e.g. "touching
restore → L-005, L-006").

**2. WHILE coding — obey the standing rules** (bottom of this file).

**3. BEFORE committing — the preflight, no exceptions.**
- Run `bash tools/preflight.sh` (compiles all modules, runs the 252-test suite, greps
  for known anti-patterns from the ledger). It must exit 0.
- Self-review your full diff against LESSONS.md. For each lesson whose **Check** your
  diff triggers, state in the commit message how you satisfied it.
- Honesty markers: any feature you did not actually execute gets `UNVERIFIED:` in the
  commit message AND a `- [ ]` line in `PROGRESS.md`. Claiming untested behavior as
  working is the single worst failure mode in this repo's history (L-001).

**4. AFTER a review finds a defect in your work — close the loop.**
Append a new `L-NNN` entry to `docs/agents/LESSONS.md` in the given format:
what shipped, why it was wrong, the generalized rule, and a mechanical check. This is
not punishment — it is how the next session gets smarter. A defect that recurs after
its lesson exists is a process failure; a defect with no lesson written is a wasted
tuition payment.

**5. NEVER — hard limits.**
- Never create a git tag or GitHub release (L-017). Releases happen only after a
  human-directed review pass. You may commit and push to master; CI gates you.
- Never touch `hermes-release.jks`, `hermes.local.properties`, or signing config
  values.
- Never delete user data paths or migrations without an explicit instruction.
- Never mark a roadmap milestone or audit finding "done" — only the review pass does.

## Build & verify commands

```bash
# Env (Windows dev box): JAVA_HOME = Android Studio JBR 21, ANDROID_HOME = user SDK
./gradlew :app:compileDebugKotlin :feature:jotter:compileDebugKotlin :feature:butler:compileDebugKotlin
./gradlew :app:testDebugUnitTest      # 252 tests, must stay 0 failures
bash tools/preflight.sh               # all of the above + ledger greps
```

CI (`.github/workflows/ci.yml`) runs compile + tests on every push to master.
A red CI on your commit means you stop feature work and fix it first.

## Standing rules (the short list)

- **3-step tool wiring** (L-014): `ToolsModule` + `AgentToolAccess` + system prompts.
- **Persist+schedule together** for alarms/crons/work (L-005).
- **Idempotent restores; replace-don't-append for recurring writers** (L-006).
- **No silent failure on user actions**; errors name the fix; flags reset in
  `finally` (L-007).
- **Human-input awaits: timeout + headless default** (L-008).
- **Privacy flags (`locked`/`encrypted`) filter every flow toward prompts/network**
  (L-009).
- **No personal literals in product code** (L-004); heuristics may order, never hide.
- **No side-effectful Kotlin default args** (L-013).
- **fg/bg visual pairs re-verified together** (L-011).
- **Version bump ships with the release change-set** (L-012) — but you don't release.
- Update `PROGRESS.md` per completed task, newest-first, with what was VERIFIED vs
  UNVERIFIED. Keep entries factual; the file has been stale before and it cost days.
- Commit style: imperative subject, body explains WHY, one logical change per commit.

## Architecture anchors (read before touching these areas)

- Agent core: `app/src/main/kotlin/com/hermes/agent/` (namespace stays `com.hermes.agent`;
  applicationId is `com.jeeves.app` — do not "fix" this).
- Cross-module access goes through Hilt (`di/FeatureBridge.kt` pattern), feature
  modules keep their original packages (`com.l3ad3r1.octojotter`, `com.sassybutler.alarm`).
- Jotter's DB and Hermes's DB are deliberately separate; the agent reaches notes via
  `NoteRepository`/RAG index, never raw SQL.
- TTS: `TtsEngine` is NOT a Hilt binding (92 MB synchronous load) — always go through
  `ButlerSpeech`.
- The ~89 MB TTS models are gitignored but required at `feature/butler/src/main/assets/tts/`
  to package a runnable APK.
