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

### Shared modules

Jeeves uses the same verified module repository contract as the public Hermes app. To
download a module, open **Settings → Features → Modules**, enter the public catalog URL,
load the catalog, and choose **Download**. HTTPS, catalog schema, artifact size, and
SHA-256 are checked before the APK is saved privately. The installer and approval flow
then remain gated by the host's security policy.

Starter catalog URL: `https://raw.githubusercontent.com/l3ad3r1/hermes-jeeves-modules/main/catalog-v1.json`

Module authors can publish through the [Hermes/Jeeves Modules repository](https://github.com/l3ad3r1/hermes-jeeves-modules);
its README explains the manifest, service, signing, catalog, and release steps.

## Getting Started

1. Download the latest APK from the [Jeeves Releases](https://github.com/l3ad3r1/Jeeves/releases) tab.
2. Grant the necessary permissions (Jeeves will lazily ask for permissions only when he needs them for a specific feature).
3. Navigate to **Settings -> Configuration -> Assistant** to set up your LLM provider and API keys.
4. Set your morning alarm via the **Sassy Butler** feature on the Home screen.

## Advanced Usage

- **CRON Scheduling**: Instruct Jeeves to perform background tasks (e.g., summarizing news, checking a server) using standard 5-field CRON expressions.
- **Self-Improvement**: Jeeves reflects on how its skills and agents actually performed on your
  device and proposes improvements. Every change is gated, needs your approval, and is version
  history you can roll back. (The older offline session-export path is retired.)
- **Local Backups**: Securely back up your memory, skills, and configuration to a private GitHub Gist.

## Building from Source

Ensure you have Android Studio and the Android SDK installed.
```bash
# Clone the repository
git clone https://github.com/your-username/jeeves.git

# Build the release APK
./gradlew assembleRelease
```

The current release line is **v0.16.1**. Hermes and Jeeves share the module contracts
from the `agent-core` checkout, while the private Jeeves product keeps its own app
permissions, branding, and release signing.
