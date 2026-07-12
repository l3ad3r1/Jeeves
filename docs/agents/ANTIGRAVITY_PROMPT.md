# The Antigravity prompt

Paste the block below into Antigravity — either as a **global rule / custom
instruction** (best: it then applies to every session automatically) or at the top of
each task you give it. It forces the AGENTS.md loop by requiring visible artifacts at
each step, so skipping a step is immediately obvious in the transcript.

---

```
MANDATORY WORKFLOW — Jeeves repo (E:\claude-projects\jeeves). Non-negotiable.
This repo has a binding contract in AGENTS.md. You follow it on every task, and you
PROVE you followed it by producing the four artifacts below in your output. Work that
arrives without its artifacts is considered not done, regardless of code quality.

ARTIFACT 1 — Ledger acknowledgment (before you write any code).
Read AGENTS.md, docs/agents/LESSONS.md, and the top of PROGRESS.md. Then output a
line in exactly this form before your first edit:
  LESSONS APPLIED: L-00X (why), L-00Y (why) — or "LESSONS APPLIED: none match, checked all 17+"
If you cannot produce this line, you have not read the ledger. Stop and read it.

ARTIFACT 2 — Preflight proof (before every commit).
Run: bash tools/preflight.sh
Paste its final lines (the "Preflight PASSED" block) into your output before
committing. Exit code must be 0. If it fails, fix the cause — never edit the script,
never skip it, never commit around it.

ARTIFACT 3 — Honesty ledger (in every commit message).
For every feature or fix you claim, state the moment you actually executed that code
path (a test name, a command, a log line). Anything you did not actually run gets the
literal marker "UNVERIFIED:" in the commit message AND an open "- [ ]" item in
PROGRESS.md. History note: the two worst bugs this repo shipped were features that
could not have survived one single manual run (L-001). An honest UNVERIFIED is
respected; a false "works" is the one unforgivable failure.

ARTIFACT 4 — CI verdict (after every push; this is the definition of done).
A push is not completion. Green CI on your commit is. After git push, watch the run:
  gh run watch "$(gh run list --branch master --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status -R l3ad3r1/Jeeves
Then output one line:
  CI VERDICT: <run-id> success
If it is red: stop all new work, read the failure with
  gh run view <run-id> --log-failed -R l3ad3r1/Jeeves
fix the cause, push, watch again — loop until green. Never weaken, skip, or
continue-on-error a check to get green. You do not report a task complete, and you do
not start the next task, without a "CI VERDICT: ... success" line for your last push.

CLOSING THE LOOP (when a review finds a defect in your work).
Append a new L-NNN entry to docs/agents/LESSONS.md in its documented format
(origin, defect, generalized rule, mechanical check) in the same session you learn of
the defect. A defect without a written lesson is wasted tuition; a defect that recurs
after its lesson exists is a process failure.

HARD LIMITS (violating any of these voids the task).
- NEVER create a git tag or a GitHub release, and never run `gh release ...` (L-017).
  You commit and push; releases happen only after a human-directed review.
- NEVER touch hermes-release.jks, hermes.local.properties, or signing config.
- NEVER mark a roadmap milestone, audit finding, or debt-ledger item "done" — only
  the review pass does that.
- NEVER claim device behavior (alarms ringing, TTS audible, widgets rendering,
  sync completing) without a device log or screenshot; otherwise mark it UNVERIFIED.

End every task with this exact checklist, filled in truthfully:
  [ ] LESSONS APPLIED line emitted before first edit
  [ ] preflight PASSED output pasted (exit 0)
  [ ] commit message: every claim has its "moment it ran" or UNVERIFIED marker
  [ ] PROGRESS.md updated (newest-first entry, VERIFIED vs UNVERIFIED split)
  [ ] CI VERDICT: <id> success
  [ ] no tags, no releases, no signing files touched
```

---

## Why it's shaped this way

- **Artifacts, not promises.** Each step demands a visible output (a line, a pasted
  block, a checklist). You can audit compliance in ten seconds by scanning the
  transcript for the four artifacts — a missing artifact IS the violation.
- **CI as the definition of done.** The failure mode to kill is "pushed and walked
  away" (that's how the broken v0.9.5/v0.9.7 releases happened). Making "CI VERDICT:
  success" the completion token converts done from a claim into a checkable fact.
- **UNVERIFIED is framed as honorable.** Agents lie about testing when honesty looks
  like failure. The prompt explicitly prices a truthful UNVERIFIED above a false
  "works", which is the incentive that makes L-001 followable.
- **The learning step is in-session.** Lessons get appended when the defect is fresh,
  not "later".

## Spot-check guide (for the human)

After any Antigravity run, scan for:
1. `LESSONS APPLIED:` before the first edit — missing → it didn't read the ledger.
2. `Preflight PASSED` paste — missing → it didn't run the gate.
3. `UNVERIFIED:` markers where you'd expect them (anything device-facing) — a run
   full of confident claims and zero UNVERIFIED markers on device features is the
   red flag, not the green one.
4. `CI VERDICT: <id> success` as the last artifact — then confirm the Actions tab
   agrees.
