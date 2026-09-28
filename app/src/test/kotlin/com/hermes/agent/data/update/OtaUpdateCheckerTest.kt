package com.hermes.agent.data.update

import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.mockk.mockk

class OtaUpdateCheckerTest {

    @Test
    fun isNewer_ignoresDebugSuffix() {
        val client = mockk<OkHttpClient>(relaxed = true)
        val checker = OtaUpdateChecker(client)
        
        // 1.0.4 is NOT newer than 1.0.8-debug
        assertFalse(checker.isNewer("1.0.4", "1.0.8-debug"))
        
        // 1.0.9 is newer than 1.0.8-debug
        assertTrue(checker.isNewer("1.0.9", "1.0.8-debug"))
        
        // 1.0.8 is NOT newer than 1.0.8-debug
        assertFalse(checker.isNewer("1.0.8", "1.0.8-debug"))
        
        // Standard comparisons
        assertTrue(checker.isNewer("1.1.0", "1.0.0"))
        assertFalse(checker.isNewer("1.0.0", "1.1.0"))
    }
}
