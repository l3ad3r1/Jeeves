# Release Notes

## Version 1.1.0 - The Jeeves Update

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
