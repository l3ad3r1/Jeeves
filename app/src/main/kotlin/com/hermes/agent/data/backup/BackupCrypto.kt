package com.hermes.agent.data.backup

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase-based encryption for credentials inside a backup archive.
 *
 * Deliberately *not* AndroidKeyStore. A keystore key cannot be exported and is
 * bound to the install that created it, which is exactly the property that
 * makes it right for storage at rest and useless for a backup: ciphertext
 * written on one device can never be opened on another. Deriving the key from
 * a passphrase moves the secret into something the user can carry, so an
 * archive stays portable while remaining unreadable to anyone who only has
 * the file.
 *
 * Format per value: `base64(salt || iv || ciphertext)`. The salt travels with
 * the value rather than the archive so each entry is self-contained and a
 * partially recovered backup still decrypts what it has.
 */
object BackupCrypto {

    /**
     * OWASP's floor for PBKDF2-HMAC-SHA256. High enough to make an offline
     * guess expensive, low enough that a backup does not visibly stall — and
     * paid once per archive, not per value, since callers derive one key.
     */
    const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private val random = SecureRandom()

    /** A fresh passphrase for a device that has none: 160 bits, no ambiguous glyphs. */
    fun generatePassphrase(): String {
        val alphabet = "abcdefghjkmnpqrstuvwxyz23456789"
        val bytes = ByteArray(32).also(random::nextBytes)
        return bytes.take(20)
            .map { alphabet[(it.toInt() and 0xFF) % alphabet.length] }
            .chunked(5) { it.joinToString("") }
            .joinToString("-")
    }

    /** Encrypt [plaintext]; returns null for blank input so callers can skip it. */
    fun encrypt(passphrase: String, plaintext: String): String? {
        if (plaintext.isBlank() || passphrase.isBlank()) return null
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt))
        }
        val payload = salt + cipher.iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(payload)
    }

    /**
     * Decrypt a value produced by [encrypt]. Returns null when the passphrase is
     * wrong or the value is malformed — the caller cannot tell those apart, and
     * should not: both mean "these credentials are not available".
     */
    fun decrypt(passphrase: String, encoded: String): String? {
        if (encoded.isBlank() || passphrase.isBlank()) return null
        return runCatching {
            val blob = Base64.getDecoder().decode(encoded)
            require(blob.size > SALT_BYTES + IV_BYTES) { "backup value too short" }
            val salt = blob.copyOfRange(0, SALT_BYTES)
            val iv = blob.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
            val ct = blob.copyOfRange(SALT_BYTES + IV_BYTES, blob.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            String(cipher.doFinal(ct), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KDF = "PBKDF2WithHmacSHA256"
}
