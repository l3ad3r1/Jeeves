package com.hermes.agent.data.backup

import kotlinx.serialization.Serializable

/**
 * The credential half of a backup archive, stored as the `secrets.json` entry.
 *
 * Kept separate from the copied DataStore file on purpose. That file holds
 * secrets encrypted against the source install's keystore key, which no other
 * device can open, so those values are stripped from the copy and re-encrypted
 * here with a passphrase-derived key instead.
 *
 * Every value is `BackupCrypto`-encrypted; nothing in this object is readable
 * without the passphrase.
 */
@Serializable
data class BackupSecrets(
    val version: Int = CURRENT_VERSION,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = BackupCrypto.ITERATIONS,
    val cloudApiKey: String = "",
    val auxApiKey: String = "",
    val githubPat: String = "",
    val apiServerKey: String = "",
    val sshPassword: String = "",
    /** Provider id → encrypted API key, so profiles survive a rename or reorder. */
    val providerKeys: Map<String, String> = emptyMap(),
) {
    companion object {
        const val CURRENT_VERSION = 1

        /** Zip entry name; also the marker that an archive carries credentials. */
        const val ENTRY_NAME = "secrets.json"

        /**
         * Written to `filesDir` by a restore and consumed on the next start.
         *
         * A restore replaces the DataStore file on disk while DataStore still
         * has the old one open, so writing settings in the same pass races the
         * copy. Handing the work to the next process start sidesteps that
         * entirely.
         */
        const val PENDING_FILE = "pending_restore_secrets.json"
    }
}
