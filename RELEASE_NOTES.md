# Release Notes

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
