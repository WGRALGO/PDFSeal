package org.thewealthgapresolutionalgorithm.pdfseal.engine.io

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Copy [input] into [output], aborting if it does not finish within
 * [timeoutMs]. A coroutine `withTimeout` cannot unblock a thread stuck in a
 * native `InputStream.read()` (e.g. a stalled MediaProvider stream on Fire
 * OS — the cause of the indefinite "Loading…" hang). So the copy runs on a
 * worker thread and, on timeout, the source stream is closed to force the
 * blocked read to throw, then a user-facing [IOException] is raised instead
 * of hanging forever.
 */
object StreamCopy {

    fun copyWithTimeout(
        input: InputStream,
        output: OutputStream,
        timeoutMs: Long,
        bufSize: Int = 64 * 1024,
    ) {
        var failure: Throwable? = null
        val worker = Thread {
            try {
                input.use { ins -> ins.copyTo(output, bufSize) }
            } catch (t: Throwable) {
                failure = t
            }
        }.apply { isDaemon = true; name = "pdfseal-stream-copy" }

        worker.start()
        worker.join(timeoutMs)

        if (worker.isAlive) {
            // Closing the source aborts a blocked read(); interrupt covers
            // streams that honour interruption instead.
            runCatching { input.close() }
            worker.interrupt()
            throw IOException(
                "Opening this file timed out. It may be on slow or remote " +
                    "storage, or the app lost access — reopen it from your " +
                    "file manager.",
            )
        }
        failure?.let { throw it }
    }
}
