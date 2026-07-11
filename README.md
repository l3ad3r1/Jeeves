# Jeeves: Your AI Assistant & Butler

**Jeeves** is a highly capable, autonomous AI assistant tailored for Android, acting as both a proactive productivity suite and a conversational interface. Powered by advanced Cloud LLM connections, Jeeves can organize your life, automate tasks, and converse intelligently.

## Features

### 🎙 Sassy Butler
Wake up on your own terms. Jeeves acts as an intelligent alarm clock ("Sassy Butler") that greets you with daily updates, customizable honorifics (Sir, Madam, Boss), and dynamic sass levels based on how many times you snooze. 

### 📝 Jotter
Capture ideas on the fly. Jotter allows you to store quick notes, to-dos, and long-form documents which Jeeves can instantly retrieve and incorporate into his context when answering questions or organizing your schedule.

### 🔌 Powerful Integrations
- **Cloud LLMs:** Connect directly to OpenAI, Anthropic, or custom inference endpoints to power Jeeves' brain.
- **Local API Server:** Run a local API endpoint right from your phone so that other applications and scripts on your network can interface with Jeeves.
- **Remote Shell & Docker:** Let Jeeves execute commands via SSH on remote servers to manage deployments, fix servers, or check container statuses right from chat.
- **Messaging App Hooks:** Jeeves can be configured to plug into Telegram, Discord, and WhatsApp, extending his reach beyond your phone.

### 🧠 Agentic Memory & Learning
Jeeves dynamically extracts facts, preferences, and workflows from your conversations. Over time, he creates new **Skills** (reusable logic blocks) and remembers your preferences to provide deeply personalized assistance.

## Getting Started

1. Download the latest APK from the [Releases](#) tab.
2. Grant the necessary permissions (Jeeves will lazily ask for permissions only when he needs them for a specific feature).
3. Navigate to **Settings -> Configuration -> Assistant** to set up your LLM provider and API keys.
4. Set your morning alarm via the **Sassy Butler** feature on the Home screen.

## Advanced Usage

- **CRON Scheduling**: Instruct Jeeves to perform background tasks (e.g., summarizing news, checking a server) using standard 5-field CRON expressions.
- **Self-Evolution Export**: Export your session logs to train and refine local agentic models.
- **Local Backups**: Securely back up your memory, skills, and configuration to a private GitHub Gist.

## Building from Source

Ensure you have Android Studio and the Android SDK installed.
```bash
# Clone the repository
git clone https://github.com/your-username/jeeves.git

# Build the release APK
./gradlew assembleRelease
```
