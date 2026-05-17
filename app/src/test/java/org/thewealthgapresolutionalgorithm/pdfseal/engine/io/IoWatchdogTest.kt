package org.thewealthgapresolutionalgorithm.pdfseal.engine.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class IoWatchdogTest {

    @Test
    fun foreverBlockingProducer_throwsTimeoutWithinBound() {
        val start = System.currentTimeMillis()
        try {
            IoWatchdog.callWithTimeout(timeoutMs = 200) {
                Thread.sleep(Long.MAX_VALUE); "never"
            }
            fail("expected IOException on timeout")
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - start
            assertTrue("must abort near timeout, was ${elapsed}ms", elapsed < 2_000)
            assertTrue(
                "user-facing message, was: ${e.message}",
                (e.message ?: "").contains("timed out", ignoreCase = true),
            )
        }
    }

    @Test
    fun fastProducer_returnsItsValue() {
        val v = IoWatchdog.callWithTimeout(timeoutMs = 5_000) { 21 * 2 }
        assertEquals(42, v)
    }

    @Test
    fun producerException_isPropagated() {
        try {
            IoWatchdog.callWithTimeout(timeoutMs = 5_000) {
                throw IllegalStateException("boom")
            }
            fail("expected the producer's exception")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }
    }
}
