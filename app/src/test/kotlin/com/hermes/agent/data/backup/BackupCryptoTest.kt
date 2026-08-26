package com.hermes.agent.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Portable credential encryption for backups.
 *
 * The property that matters is the one AndroidKeyStore cannot provide: a value
 * sealed on one device must open on another given only the passphrase.
 */
class BackupCryptoTest {

    private val passphrase = "abcde-fghij-klmno-pqrst"

    @Test
    fun `round-trips a value`() {
        val sealed = BackupCrypto.encrypt(passphrase, "sk-real-key-value")
        assertNotNull(sealed)
        assertFalse("ciphertext must not contain the plaintext", sealed!!.contains("sk-real-key-value"))
        assertEquals("sk-real-key-value", BackupCrypto.decrypt(passphrase, sealed))
    }

    @Test
    fun `a different passphrase cannot open it`() {
        val sealed = BackupCrypto.encrypt(passphrase, "sk-real-key-value")!!
        assertNull(BackupCrypto.decrypt("wrong-passphrase-here", sealed))
    }

    @Test
    fun `same plaintext seals differently each time`() {
        // Per-value salt and IV: identical keys across two providers must not be
        // recognisable as identical in the archive.
        val a = BackupCrypto.encrypt(passphrase, "same-value")!!
        val b = BackupCrypto.encrypt(passphrase, "same-value")!!
        assertNotEquals(a, b)
        assertEquals("same-value", BackupCrypto.decrypt(passphrase, a))
        assertEquals("same-value", BackupCrypto.decrypt(passphrase, b))
    }

    @Test
    fun `blank input and blank passphrase produce nothing`() {
        assertNull(BackupCrypto.encrypt(passphrase, ""))
        assertNull(BackupCrypto.encrypt("", "value"))
        assertNull(BackupCrypto.decrypt(passphrase, ""))
    }

    @Test
    fun `malformed or truncated values decrypt to null rather than throwing`() {
        assertNull(BackupCrypto.decrypt(passphrase, "not-base64-at-all!!"))
        assertNull(BackupCrypto.decrypt(passphrase, "AAAA"))
        val sealed = BackupCrypto.encrypt(passphrase, "value")!!
        assertNull(BackupCrypto.decrypt(passphrase, sealed.dropLast(4)))
    }

    @Test
    fun `generated passphrases are unique and readable`() {
        val phrases = List(20) { BackupCrypto.generatePassphrase() }
        assertEquals("passphrases must not repeat", 20, phrases.toSet().size)
        phrases.forEach { phrase ->
            // Grouped lowercase, no glyphs that are ambiguous when transcribed.
            assertTrue(phrase.matches(Regex("[a-z2-9]{5}(-[a-z2-9]{5})+")))
            assertFalse(phrase.any { it in "il1o0" })
        }
    }

    @Test
    fun `a passphrase generated here opens a value sealed with it`() {
        val generated = BackupCrypto.generatePassphrase()
        val sealed = BackupCrypto.encrypt(generated, "portable-secret")!!
        assertEquals("portable-secret", BackupCrypto.decrypt(generated, sealed))
    }

    private fun assertNotNull(value: Any?) = assertTrue("expected non-null", value != null)
}
