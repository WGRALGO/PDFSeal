package org.thewealthgapresolutionalgorithm.pdfseal.engine.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class StreamCopyTest {

    /** A stream whose read() blocks forever — models the Fire OS MediaProvider stall. */
    private fun foreverBlockingStream(): InputStream = object : InputStream() {
        override fun read(): Int {
            Thread.sleep(Long.MAX_VALUE); return -1
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            Thread.sleep(Long.MAX_VALUE); return -1
        }
    }

    @Test
    fun blockedSource_throwsIOExceptionWithinTimeout() {
        val start = System.currentTimeMillis()
        try {
            StreamCopy.copyWithTimeout(
                foreverBlockingStream(), ByteArrayOutputStream(), timeoutMs = 200,
            )
            fail("expected IOException on timeout")
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - start
            assertTrue("must abort near the timeout, was ${elapsed}ms", elapsed < 2_000)
            assertTrue(
                "message must be user-facing, was: ${e.message}",
                (e.message ?: "").contains("timed out", ignoreCase = true),
            )
        }
    }

    @Test
    fun fastSource_copiesFullyAndDoesNotFalseTimeout() {
        val data = ByteArray(256 * 1024) { (it % 251).toByte() }
        val out = ByteArrayOutputStream()
        StreamCopy.copyWithTimeout(data.inputStream(), out, timeoutMs = 5_000)
        assertEquals(data.size, out.size())
        assertTrue("content must be intact", data.contentEquals(out.toByteArray()))
    }
}
