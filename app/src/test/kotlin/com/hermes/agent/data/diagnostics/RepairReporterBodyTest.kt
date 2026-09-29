package com.hermes.agent.data.diagnostics

import org.junit.Assert.assertTrue
import org.junit.Test

class RepairReporterBodyTest {
    @Test
    fun `a report names the app it came from so the repair pipeline picks the right repo`() {
        val body = RepairReporter.body("Jeeves", "It crashed.", "log line", "1.1.1")
        assertTrue(body, body.startsWith("### App\n\nJeeves\n\n"))
        assertTrue(body.contains("### Component\n\nJeeves"))
    }
}
