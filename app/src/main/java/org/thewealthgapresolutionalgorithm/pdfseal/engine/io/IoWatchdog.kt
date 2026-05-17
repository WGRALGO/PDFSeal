package org.thewealthgapresolutionalgorithm.pdfseal.engine.io

import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

/**
 * Run a blocking [producer] on a throw-away daemon thread and give up on it if
 * it does not finish within [timeoutMs].
 *
 * Why a thread and not coroutine `withTimeout`: the open path used to hang on
 * "Loading…" forever because a Fire OS MediaProvider call
 * (`openInputStream` / `query`) never returned. `withTimeout` cannot unblock a
 * thread parked in a blocking platform call — cancellation only takes effect
 * at suspension points. By running the whole content-URI intake here, a stuck
 * provider call leaves a parked daemon thread (harmless, GC-isolated) while
 * the caller gets a clear [IOException] instead of an indefinite hang, and the
 * engine's single MuPDF thread is never the one that blocks.
 */
object IoWatchdog {

    fun <T> callWithTimeout(timeoutMs: Long, producer: () -> T): T {
        val result = AtomicReference<T>()
        val failure = AtomicReference<Throwable>()
        val worker = Thread {
            try {
                result.set(producer())
            } catch (t: Throwable) {
                failure.set(t)
            }
        }.apply { isDaemon = true; name = "pdfseal-io-watchdog" }

        worker.start()
        worker.join(timeoutMs)

        if (worker.isAlive) {
            worker.interrupt() // best effort; the thread is abandoned regardless
            throw IOException(
                "Opening this file timed out. It may be on slow or remote " +
                    "storage, or the app lost access — reopen it from your " +
                    "file manager.",
            )
        }
        failure.get()?.let { throw it }
        return result.get()
    }
}
