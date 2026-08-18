package com.hermes.agent.data.backup

import android.content.Context
import com.hermes.agent.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies credentials staged by a restore, on the next app start.
 *
 * A restore replaces the DataStore file on disk while DataStore still holds the
 * old one open, so settings written during the restore itself race the copy.
 * Staging the credentials to a file and applying them in a later process avoids
 * that entirely — the same reason the unreadable-secret sweep runs at startup.
 *
 * Values are decrypted with the backup passphrase, then written through
 * [SettingsRepository], which re-seals them under *this* install's keystore key.
 * That is the step that makes a restored key usable rather than inert.
 */
@Singleton
class RestoredSecretsApplier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    sealed class Outcome {
        /** No archive was restored, or it carried no credentials. */
        object Nothing : Outcome()
        data class Applied(val count: Int) : Outcome()

        /**
         * The staged file is present but this device's passphrase does not open
         * it — the usual case on a *new* device. The file is kept so the user
         * can supply the passphrase later; see [applyWith].
         */
        object NeedsPassphrase : Outcome()
    }

    private val pending: File get() = File(context.filesDir, BackupSecrets.PENDING_FILE)

    /** Tolerates archives written by a newer build that added fields. */
    private val LENIENT = Json { ignoreUnknownKeys = true }

    /** True when a restore left credentials waiting for a passphrase. */
    fun hasPending(): Boolean = pending.isFile

    /** Try the passphrase this device already remembers. */
    suspend fun applyPending(): Outcome = withContext(Dispatchers.IO) {
        if (!pending.isFile) return@withContext Outcome.Nothing
        applyWith(settingsRepository.current().backupPassphrase)
    }

    /**
     * Apply the staged credentials using [passphrase]. On success the passphrase
     * is remembered, so this device's own future backups round-trip without
     * asking again, and the staged file is deleted.
     */
    suspend fun applyWith(passphrase: String): Outcome = withContext(Dispatchers.IO) {
        if (!pending.isFile) return@withContext Outcome.Nothing
        if (passphrase.isBlank()) return@withContext Outcome.NeedsPassphrase

        val secrets = runCatching {
            LENIENT.decodeFromString(BackupSecrets.serializer(), pending.readText())
        }.getOrElse {
            // Unparseable rather than merely locked: keeping it would prompt for
            // a passphrase that can never work.
            Timber.tag("RestoreSecrets").w(it, "staged secrets unreadable; discarding")
            pending.delete()
            return@withContext Outcome.Nothing
        }

        var applied = 0
        suspend fun open(encoded: String, set: suspend (String) -> Unit) {
            if (encoded.isBlank()) return
            BackupCrypto.decrypt(passphrase, encoded)?.let { plain ->
                set(plain)
                applied++
            }
        }

        open(secrets.cloudApiKey) { settingsRepository.setCloudApiKey(it) }
        open(secrets.auxApiKey) { settingsRepository.setAuxApiKey(it) }
        open(secrets.githubPat) { settingsRepository.setGithubPat(it) }
        open(secrets.apiServerKey) { settingsRepository.setApiServerKey(it) }
        open(secrets.sshPassword) { settingsRepository.setSshPassword(it) }

        if (secrets.providerKeys.isNotEmpty()) {
            val profiles = settingsRepository.current().cloudProviderProfiles
            val restored = profiles.map { profile ->
                val encoded = secrets.providerKeys[profile.id]
                val plain = encoded?.let { BackupCrypto.decrypt(passphrase, it) }
                if (plain != null) {
                    applied++
                    profile.copy(apiKey = plain)
                } else {
                    profile
                }
            }
            if (restored.any { it.apiKey.isNotBlank() }) {
                settingsRepository.setCloudProviderProfiles(restored)
            }
        }

        if (applied == 0) {
            // Nothing opened: the passphrase belongs to a different device.
            Timber.tag("RestoreSecrets").i("staged secrets need a passphrase from the source device")
            return@withContext Outcome.NeedsPassphrase
        }

        settingsRepository.setBackupPassphrase(passphrase)
        pending.delete()
        Timber.tag("RestoreSecrets").i("restored %d credential(s) from backup", applied)
        Outcome.Applied(applied)
    }
}
