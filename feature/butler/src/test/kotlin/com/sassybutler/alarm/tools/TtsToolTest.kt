package com.sassybutler.alarm.tools

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sassybutler.alarm.ButlerSpeech
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TtsToolTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val butlerSpeech = mockk<ButlerSpeech>(relaxed = true)
    private val tool = TtsTool(context, butlerSpeech)

    private fun args(vararg pairs: Pair<String, String>): Map<String, JsonElement> =
        pairs.associate { it.first to JsonPrimitive(it.second) }

    @Test
    fun `speaks with Butler voice by default`() = runTest {
        coEvery { butlerSpeech.speak(any(), any()) } returns ButlerSpeech.SpeakResult.SPOKEN

        val result = tool.execute(args("text" to "good morning"))

        assertTrue(result.errorMessage ?: "", result.success)
        assertTrue(result.output, result.output.contains("Butler"))
        coVerify { butlerSpeech.speak("good morning", any()) }
    }

    @Test
    fun `returns error when butler speech is unavailable`() = runTest {
        coEvery { butlerSpeech.speak(any(), any()) } returns ButlerSpeech.SpeakResult.UNAVAILABLE

        val result = tool.execute(args("text" to "hello"))

        assertFalse(result.success)
        assertTrue(result.errorMessage!!.contains("unavailable"))
    }

    @Test
    fun `returns stopped result when speech is cancelled`() = runTest {
        coEvery { butlerSpeech.speak(any(), any()) } returns ButlerSpeech.SpeakResult.STOPPED

        val result = tool.execute(args("text" to "hello"))

        assertTrue(result.success)
        assertTrue(result.output.contains("stopped", ignoreCase = true))
    }

    @Test
    fun `stop halts speech`() = runTest {
        val result = tool.execute(args("action" to "stop"))

        assertTrue(result.success)
        verify { butlerSpeech.stop() }
    }

    @Test
    fun `missing text is a tool error`() = runTest {
        val result = tool.execute(args("action" to "speak"))
        assertFalse(result.success)
        assertTrue(result.errorMessage!!.contains("text"))
    }
}
