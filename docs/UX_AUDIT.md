# Jeeves UI/UX Audit — v0.9.0

> **Status update.** JX-01 (dueling updaters), JX-02 (brand fracture), JX-05 (Alarms contrast)
> and JX-06 (Alarms light-only) are FIXED — see commits c815d49, 4d832a4, a0b8d4d.
> JX-03, JX-04, JX-07..JX-16 remain open.

**Scope:** the merged super app as shipped at commit `35dbf89` — the Hermes host (home,
onboarding, chat, settings), the embedded Octo Jotter and Sassy Butler surfaces, and the seams
between them. **Method:** heuristic review (Material guidance, Nielsen heuristics), code
inspection, on-device screenshots from the Pixel_7 emulator runs, and computed WCAG 2.1
contrast ratios. This is not a substitute for a moderated usability test or a full TalkBack
pass (see JX-11).

**Severity scale**
- **Critical** — causes user harm or broken core behaviour; fix before any release.
- **High** — visibly undermines the product promise or excludes users; fix in the next milestone.
- **Medium** — friction/regression risk; schedule.
- **Low** — polish; opportunistic.

---

## Critical

### JX-01 · Two legacy in-app updaters offer "updates" that install *different apps*
**Evidence:** `NoteViewModel.checkForUpdate()` calls
`githubApiService.getLatestRelease("l3ad3r1", "Octo-Jotter")` and compares against
`BuildConfig.VERSION_NAME` — which Jeeves injects as **0.9.0**. The standalone Octo Jotter
repo's latest release is **v2.5**, so the updater will *permanently* report an update, download
the standalone APK, and hand it to the installer. Standalone Jotter has a different
`applicationId`, so the user ends up with a **second, separate notes app** and no update.
Hermes's `OtaUpdateChecker` points at `l3ad3r1/Hermes-Agent-Android/releases/latest` — the same
defect, latent: it fires the moment standalone Hermes ships a version above 0.9.0.
**Impact:** data-splitting confusion (two Jotters with divergent note stores), broken trust in
the update mechanism, wasted 100+ MB downloads.
**Fix (S–M):** add a `JEEVES_BUILD` flag (BuildConfig or resource). When set: hide Jotter's
updater row in its Settings, short-circuit `checkForUpdate()` to `UpToDate`, and do not
schedule `OtaUpdateWorker`. Longer term, point one updater at the Jeeves release channel and
delete the other.

---

## High

### JX-02 · Brand identity fracture: the launcher says Jeeves, everything inside says Hermes
**Evidence:** launcher label "Jeeves"; onboarding headline "Let's set up **your assistant** …
the permissions **Hermes** needs"; home greeting, chat placeholder ("Ask Hermes anything…"),
error toasts and personas all say Hermes; then two more brands (Octo Jotter, Sassy Butler) live
inside. A new user cannot answer "what app did I just install?"
**Fix (S + a product decision):** pick a hierarchy and apply it mechanically — recommended:
**Jeeves** is the app/host brand (onboarding, notification channel group, About, Settings
title); **Hermes** survives only as the agent's persona name inside conversation; Jotter/Butler
are presented as capabilities ("Notes", "Alarms") with their brand as flavour, not navigation.
This is a strings-file change plus onboarding copy.

### JX-03 · Opening Jotter or Butler from Home flashes their standalone splash screens
**Evidence:** both embedded activities keep their cold-start splash themes
(`Theme.OctoJotter.Starting`, `Theme.SassyButler.Splash`); tapping an APPS card plays a branded
splash before content. **Impact:** every entry feels like leaving Jeeves for another app — the
single-APK merge's whole promise, contradicted twice per session, and ~500 ms of dead time.
**Fix (S):** pass `EXTRA_EMBEDDED=true` from the host cards; in each activity's `onCreate`,
when set, apply the post-splash theme directly (skip `installSplashScreen()` handoff). Keep the
splash for genuine cold entries (the ACTION_SEND share target).

### JX-04 · The Home screen ships with zero accessibility semantics
**Evidence:** `HomeScreen.kt` contains **0** `contentDescription`s. The Settings entry is a
circle containing the letter "A" — TalkBack announces it as *"A"*. The ExpressiveEyes "poke"
is `clickable(indication = null)` with no semantics: no ripple, no focus target, undiscoverable
by anyone. The "Open" section action is a bare `Text` well under the 48 dp minimum target. The
two APPS cards render the identical HermesDiamond glyph — nothing but the label distinguishes a
notes app from an alarm clock.
**Fix (S–M):** `contentDescription = "Settings"` (and render a gear or the user avatar, not
"A"); give the eyes `role = Button` and a description or mark them decorative; wrap small
actions in `minimumInteractiveComponentSize()`; give the cards distinct leading icons — Butler
already ships `ic_bowler_hat`, Jotter has its splash mark.

### JX-05 · Butler fails WCAG contrast on an interactive control (measured)
Computed against `cream #F5F2EE`:

| Token | Usage | Ratio | Verdict |
|---|---|---|---|
| `powder_blue #87BDD4` | **"PREVIEW WAKE-UP →" link** | **1.83:1** | FAIL (below even 3:1) |
| `faded_grey #9E9690` | empty-state text ("Remarkably free calendar.") | 2.61:1 | FAIL |
| `apple_green #5C8B3A` | accent text | 3.61:1 | large-text only |
| `warm_grey #6B6560` | muted text | 5.15:1 | pass |
| `ink #1C2127` | body | 14.52:1 | pass |

**Impact:** the *primary demo affordance* of the alarm app is near-invisible in bright light
and to low-vision users.
**Fix (S):** introduce a text-safe accent (e.g. deepen powder_blue toward `#2E6E8E` ≈ 4.6:1)
for links, keep the pastel for decorative fills; promote empty-state copy to `warm_grey`;
reserve `faded_grey` for genuinely disabled controls.

---

## Medium

### JX-06 · Dark-mode discontinuity across the three surfaces
Hermes themes itself; Jotter follows system (with its own override *and* plugin themes); Butler
is light-only. A dark-mode user gets a cream flashbang entering Butler at 6 AM — the exact
moment the alarm app is used. **Fix (M):** define a Butler dark palette (its wake screen is
already dark, 14.5:1 — extend those tokens to the parlour), and honour one theme source of
truth from the host, with Jotter's plugin themes as an explicit opt-out.

### JX-07 · Permission choreography: Butler ambushes on first open
Butler requests notifications + coarse location the instant it first opens, no rationale —
possibly minutes after onboarding already asked for notifications. First-contact permission
walls get denied at much higher rates, and a denied `POST_NOTIFICATIONS` mutes the alarm app.
**Fix (M):** fold permissions into the host onboarding with per-permission rationale; inside
Butler, check-don't-ask, with inline affordances ("Enable weather on your wake-up card →")
that request in context.

### JX-08 · Agent actions confirm in text but give nothing to touch, in the wrong clock
`set_alarm` replies "alarm … set for **07:30**" — `"%02d:%02d"` hardcoded 24-hour regardless of
locale (12-hour users misread early-morning times: the one domain where that matters). No
tappable path to view/edit/undo the alarm; agent-created notes are equally invisible until the
user hunts them down. **Fix (S/M):** format with `DateFormat.getTimeFormat(context)`; append a
deep-link chip to tool confirmations ("View in Butler"); later, a Home row surfacing *next
alarm / latest note* would close the loop.

### JX-09 · Three disconnected settings surfaces
Hermes Settings (bottom nav), Jotter Settings (own theme toggle, updater, backup), Butler's
preferences sheet — no cross-links, duplicate concepts, and the JX-01 updater row lives in one
of them. **Fix (S–M):** host Settings gains an "Apps" section deep-linking into each; scope
labels on per-app toggles ("Theme — applies to Jotter only").

### JX-10 · First spoken reply stalls for seconds with generic feedback
The first `speak` call loads the 92 MB voice on demand (correct engineering trade-off), but the
user just sees a long tool spinner. **Fix (S):** fire `warmUp()` as soon as a speak tool call is
dispatched and show "(preparing voice…)" in the tool status; optionally pre-warm when voice
output is enabled in settings.

### JX-11 · Butler's window returned empty accessibility dumps during testing — verify
`uiautomator dump` repeatedly produced an empty tree for Butler's window (testing fell back to
raw coordinates). Possibly a tooling race — but if the view hierarchy genuinely isn't exposed,
TalkBack users cannot operate the alarm app at all. **Fix (S to verify):** run Accessibility
Scanner and a TalkBack pass over Butler; while there, label its icon-only controls (bell FAB,
HUSH ring) and check the custom NumberPicker.

### JX-12 · The merged app's superpowers are undiscoverable
Onboarding never mentions notes, alarms, or that you can say "wake me at 7 am" or hear replies
in the Butler voice — the differentiators of the merge are invisible until stumbled upon.
**Fix (M):** one onboarding page ("One butler, three trades") or dismissible first-run cards on
Home with example prompts that pre-fill chat.

---

## Low

- **JX-13 · Dev-voiced copy on consumer surfaces.** The hero card leads with "Active model:
  gpt-4o-mini / not configured"; errors read "Cloud is disabled. Enable it and add an API key in
  Settings." Fine for its power-user owner today; soften with a "Set up assistant →" empty state
  if the audience ever widens.
- **JX-14 · Share-sheet identity.** The share target shows the Jeeves icon with the label
  "Octo Jotter". Consider "Save to Jotter" for a clearer verb-first entry.
- **JX-15 · i18n debt.** Home labels and tool confirmations are hardcoded Kotlin strings;
  `supportsRtl` is declared but untested. Acceptable for a personal build; blocks localization.
- **JX-16 · Hardware Enter inserts a newline in chat.** The soft keyboard correctly shows
  Send (`ImeAction.Send`, `maxLines = 6`); physical keyboards get multiline Enter. Consider
  Ctrl+Enter-to-send. (Initially misdiagnosed as a bug — the soft-key path is correct.)

---

## What is already good
Deliberate single-launcher identity with `exported=false` guards on embedded activities; Jotter's
list screen ships real content descriptions ("Search Notes", "Add Note", "Synced"); clear,
warm empty states with primary CTAs ("Create your first note" / "Connect GitHub"); Butler's
wake screen is properly dark with 14.5:1 text; edge-to-edge done in the modern style; chat input
uses `ImeAction.Send`; the alarm lock-screen flow (single-instance, own task affinity,
excluded from recents) is textbook.

## Suggested fix order
1. **JX-01** — disable both legacy updaters (Critical, S). Do this before v0.9.0 ships.
2. **JX-05** — contrast tokens (S, two colour values).
3. **JX-04** — Home semantics + touch targets + distinct card icons (S–M).
4. **JX-03** — splash bypass for embedded opens (S).
5. **JX-02 + JX-08** — brand strings + locale time format (S).
6. Then the M-tier: JX-07 permission hub, JX-06 Butler dark palette, JX-09 settings links,
   JX-12 discovery, with JX-11's TalkBack pass scheduled alongside any Butler work.
