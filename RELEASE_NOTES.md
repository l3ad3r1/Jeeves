# Release Notes

## Version 0.11.9 - Chat connection recovery

### Fixed
* **Transient cloud chat aborts recover automatically:** Jeeves retries one safe
  cloud request when Android reports a temporary socket failure such as
  `Software caused connection abort`.
* **Connection failures are actionable:** if the retry also fails, chat asks the
  user to check the internet connection and try again instead of showing raw
  operating-system socket text.
* **HTTP errors remain immediate:** authentication, rate-limit, and server responses
  are not blindly replayed by the transport retry policy.
* **Regression protection:** focused tests cover abort-then-success and repeated
  abort behavior; the full repository preflight passes.

> A real network-switch recovery remains device-unverified because no Android device
> was connected during the final verification pass.
## Version 0.11.8 - Local model lifecycle and reboot recovery

### Fixed
* **Clear custom model no longer crashes Settings:** leaving Settings no longer
  destroys the application-scoped inference engine, and clearing or switching a
  model unloads it safely before the selection changes.
* **Model changes cannot race active inference:** generation, model selection, and
  download-folder changes now share one serialized lifecycle. Failures produce an
  actionable Settings message instead of disappearing in a coroutine.
* **Reboot recovery follows current Android restrictions:** Jeeves no longer tries
  to launch a prohibited data-sync foreground service from the boot receiver.
  Persisted queued tickets are reconciled with finite, unique WorkManager work.
* **Regression protection:** lifecycle and boot-action tests plus ledger preflight
  checks cover both shipped defect classes.

> Device verification is still outstanding for clearing a real GGUF during active
> inference and rebooting with queued tickets.

## Version 0.11.6 - LLM Prompt Formatting Decoupled & Stability Fixes

*   **Gibberish Output Fix**: Fixed an issue where the local Llama 3 model produced total gibberish. The Kotlin layer was manually wrapping prompts in `<|begin_of_text|>` tags, while the C++ engine's Jinja templates applied its own tags. This caused double-wrapping and corrupted the KV cache. Prompt string formatting has now been fully delegated to the native C++ engine.
*   **Vulkan Disabled for Release**: The engine now forces CPU-only inference (`DGGML_VULKAN=OFF`) to ensure reliable stability across Adreno GPUs and avoid host compiler issues on Windows during local release builds.
*   **Prompt State Cleanup**: Removed the rigid `_readyForSystemPrompt` state gate in `InferenceEngineImpl.kt` which was causing prompt delivery failures.

## Version 0.10.0 - On-device AI, and builds you can trust

Jeeves can now run a language model entirely on your phone - no cloud key, no
network - and every commit to the app is now built and tested by CI, including
the native engine, so "it built on one machine" can never again masquerade as
"it works."

### On-device AI (new)
*   **Native llama.cpp engine** replaces the MediaPipe runtime: faster,
    open, and under our control down to the C++ bindings.
*   **In-app model download:** grab `Llama 3.2 1B Instruct` from Settings with
    a live progress bar - no more ADB side-loading. The model unpacks
    automatically and is ready to chat.
*   **Offline by default when cloud is off:** if Cloud LLM is disabled or no
    API key is set, chats route straight to the local model instead of asking
    you to enable cloud.
*   GPU offload plumbing (Vulkan/Adreno) is compiled in but still
    experimental - the engine falls back to CPU where Vulkan isn't available.
    Not yet device-verified; treat CPU as the supported path this release.

### Reliability (the real headline)
*   **CI gate on every push:** all three modules compile, the full native
    engine builds, the APK assembles, and the 252-test suite runs - on every
    single commit. Red means it doesn't merge.
*   **Reproducible builds:** the llama.cpp source is now a pinned submodule
    with our local patches tracked in-repo and applied by script. Any machine
    (and CI) can build Jeeves from a bare checkout - previously the native
    engine only built on one specific PC, and nothing could detect that.
*   Fixed a native logging call that broke builds for Android 10 devices.

### Coming next (v0.10.x, per the roadmap)
This release is the gateway to "The Great Verification": the on-device sweep
of everything shipped in 0.9.7-0.9.9 (briefing, One Memory, ambient surfaces,
backup round-trip, accessibility). Features are frozen until the debt ledger
in `docs/DIGITAL_BUTLER_ROADMAP.md` empties.

## Version 0.9.9 - Repo picker shows all repositories

v0.9.8's repo-sync fix corrected the name matching but kept a filter that HID
every repository except vault-named ones - so when the vault repo wasn't visible
to the token (a private repo with a gist-only PAT returns nothing from GitHub),
the picker looked broken with no explanation.

*   **The picker now lists every repository the token can access** - vault-looking
    repos (second-brain / vault / notes) float to the top, nothing is hidden.
*   **Clear empty-state message:** if the list is empty, the app now says why -
    private repos need a token with the `repo` scope (or, for fine-grained
    tokens, explicit access to that repository).

> If your vault repo still doesn't appear: it's private and your saved GitHub
> token only has the `gist` scope (enough for Gist backup, not for private
> repos). Create a classic PAT with `repo` + `gist` scopes (or a fine-grained
> token granting the vault repo) and paste it in Notes → Settings.

## Version 0.9.8 - One Memory, repo-sync fix, smarter context

Jeeves now actually knows what you know: your notes feed the agent's memory, and
every reply is grounded in the most relevant context. Plus an urgent fix for repo
sync, which v0.9.7 broke.

### Fixed
*   **Repo sync works again** (broken in v0.9.7): the repository picker filter
    matched "second brain" with a space, but real repos are hyphenated - it
    matched nothing. Separators are now normalized before matching.
*   **Locked notes stay private:** notes behind the biometric app-lock are never
    indexed into the agent's memory or sent to the cloud LLM - and locking a note
    now removes it from the index.
*   **Tool approvals can't hang the agent:** confirmation requests time out after
    60 seconds (denied by default) so Telegram/API/scheduled turns never stall.
*   **Spoken replies no longer stutter:** sentence-streamed speech tracked its
    place incorrectly and could repeat or swallow words.

### One Memory (new)
*   **The agent can search your notes** (`search_notes` tool) and cites them when
    answering questions about your projects and documents.
*   **Live note indexing:** creating, editing, or deleting a note updates the
    agent's memory immediately.
*   **Relevance-scored context:** every reply pulls the most relevant memories and
    note passages for your actual message, instead of a generic memory dump.
*   **Conversation summaries:** leaving a chat saves a concise summary to long-term
    memory (once per conversation - no duplicates).
*   **Habit signals:** nightly analysis of your alarms and note tags keeps a
    rolling snapshot of your routines in memory.

### Security & polish
*   API keys, GitHub PAT, API-server key, and SSH password are now encrypted at
    rest with the hardware keystore (previously only the cloud key was).
*   Tools the current agent isn't granted are refused outright (allowlist).
*   Sensitive tools now show an Allow/Deny dialog in chat before running.
*   Onboarding asks for each permission individually, with an explanation.
*   Reduced-motion setting disables the blinking eyes and typing animations.
*   Early scaffolding for assistant-role/tile/widgets/share-target shipped but
    still experimental - full ambient access lands in a later release.

## Version 0.9.6 - Unified backup, working editor, reliable updates

One backup for the whole app, a Notes editor that behaves, and update downloads
that survive the screen. (0.9.5 was staged internally and never shipped; this
release includes that work.)

### Backup & Restore
*   **One backup, all three modules:** the GitHub Gist backup now bundles notes and
    alarms alongside memories, skills, settings, and cron jobs - one Backup button,
    one Gist, one Restore.
*   **Restored alarms actually ring:** restore now schedules alarms with the system,
    not just saves them (previously they stayed silent until reboot).
*   **Restore is safe to repeat:** notes are no longer duplicated when Restore is
    tapped more than once.

### Notes editor
*   **Undo/Redo work** (capped history so long sessions stay lean).
*   **Format bar stays above the keyboard** instead of hiding behind it.
*   **Tag and folder rows removed** from the editor; the tag button now inserts a
    hashtag at the cursor (tags are parsed from `#hashtags` in the note).
*   **NotebookLM actions now tell you what's wrong:** if no Cloud LLM is configured
    or a request fails, you get an actionable message instead of a silent nothing or
    a stuck "AI is thinking..." dialog.

### Daybook
*   **Clock fixed:** seconds sit on the same baseline as the time; day/date resized.
*   **Preview Wake-Up removed.**

### Updates
*   **Screen stays awake during update downloads** - previously the download stalled
    when the screen turned off.

### Icon
*   **Launcher icon matches the reference artwork:** black-tie tuxedo mark on white.

## Version 0.9.4 - Icon, Notes & Daybook fixes

Fixes to regressions introduced by the v0.9.2/v0.9.3 UI overhaul, plus a rename for
the Alarms module now that it carries weather and calendar alongside wake-ups.

*   **New launcher icon:** replaced the placeholder mark with the tuxedo/bow-tie icon.
*   **Notes:** the hamburger button opens the folder drawer again (the drawer wrapper
    was dropped during the Notesnook-style redesign, leaving the button connected to
    nothing); the editor now has a visible Edit/Preview toggle — previously there was
    no way back into edit mode once a note opened in read-only preview.
*   **Daybook (formerly "Alarms"):** renamed to reflect that it now covers wake-ups,
    weather, and calendar, not just alarms. Weather, today's calendar, and scheduled
    alarms are now three distinct cards instead of one crowded block; the "Prefs"
    button is a settings cog; the header no longer misaligns when the greeting wraps
    to two lines.
*   **Add Alarm sheet:** the day-of-week selector no longer clips on narrower screens
    (seven fixed-size circles could add up to wider than the sheet itself).
*   **Versioning:** `versionCode`/`versionName` had been stuck at 60/0.9.0 since the
    v0.9.0 tag despite three releases (v0.9.1-v0.9.3) shipping on top of it — the
    in-app "About" version and OTA User-Agent were wrong for all of them. Fixed going
    forward.

## Version 0.9.2 - The Jeeves Update

*(Originally logged here as "1.1.0" - that didn't match the versionName scheme used
by every git tag and GitHub release; corrected to the version this actually shipped
as.)*

Welcome to the largest update to our AI assistant yet! This update completely revamps the application's user interface, navigation, and settings, while introducing a major rebranding to **Jeeves**.

### 🌟 Major Highlights

*   **App Rebranding:** The app has been officially rebranded to **Jeeves**. Enjoy our brand-new, modern app icon across your launcher and adaptive icon environments.
*   **Settings Architecture Overhaul:** We've retired the massive monolithic settings screen. Settings are now cleanly organized into specialized subpages:
    *   **Assistant Settings:** Manage your Cloud LLM connections and chat behaviors.
    *   **Appearance Settings:** Control theme and dark mode toggles.
    *   **Alarms (Sassy Butler):** Customize Jeeves' morning wake-up calls, honorifics, and snoozing attitude.
    *   **Connections:** Configure Local API servers and remote SSH targets for agentic shell operations.
    *   **Advanced:** Handle OTA updates, GitHub Gist backups, and session exports for self-evolution.
    *   **About & Security:** View app info and hardware Keystore diagnostics.

### 🛠 UI / UX Improvements

*   **Destructive Actions:** All deletion operations (plugins, skills, sessions) now utilize a standardized, safe confirmation dialog to prevent accidental data loss.
*   **Permissions:** We've removed the aggressive onboarding permission walls. Jeeves now politely requests permissions *only* when a specific feature requires them.
*   **Secondary Navigation:** All secondary screens (Documents, Memory, Skills, Plugins, Connections, CRON) now feature proper Top App Bars with a visible back navigation button for an intuitive flow.
*   **Kanban Chips:** Kanban boards now use non-interactive, properly styled status chips for better readability.
*   **Search & Sessions:** The "Search" tab has been sensibly renamed to "Chats". Session searches are now appropriately debounced for a fluid typing experience.
*   **Accessibility:** Screen-readers will now properly announce loading states, empty screens, and new chat messages via `LiveRegionMode.Polite` semantics. Important touch targets (like the settings avatar) have been enlarged for better accessibility.
*   **UI States & Resilience:** Loading, Error, and Empty states have been unified across the app. When plugins or skills fail to load, you'll now see a helpful "Retry" button.

### ⏰ Feature Refinements

*   **CRON Scheduling:** Added strict regex-based validation for custom CRON expressions. The 'Schedule' button will intelligently disable if your 5-field CRON format is incorrect.
*   **Recent Threads:** The Home screen's recent threads section will now navigate directly to the specific conversation instead of dropping you onto the generic Chats tab.

---

*Thank you for trusting Jeeves to be your personal AI assistant!*
