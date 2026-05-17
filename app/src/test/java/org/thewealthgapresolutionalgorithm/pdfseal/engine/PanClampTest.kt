package org.thewealthgapresolutionalgorithm.pdfseal.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanClampTest {

    @Test
    fun zeroPan_staysZero() {
        assertEquals(0f, PanClamp.clampAxis(0f, 1000f, 800f), 0.001f)
    }

    @Test
    fun smallPan_passesThrough() {
        // Within limits — unchanged.
        assertEquals(50f, PanClamp.clampAxis(50f, 1000f, 800f), 0.001f)
    }

    @Test
    fun hugePan_isClampedSymmetrically() {
        val pos = PanClamp.clampAxis(100_000f, 1000f, 800f)
        val neg = PanClamp.clampAxis(-100_000f, 1000f, 800f)
        // keep = min(1000,800)*0.2 = 160; limit = 500+400-160 = 740
        assertEquals(740f, pos, 0.001f)
        assertEquals(-740f, neg, 0.001f)
    }

    @Test
    fun tinyPageInBigViewport_keepsNonNegativeLimit() {
        // scaled page tiny vs viewport — limit must never go negative.
        val v = PanClamp.clampAxis(10_000f, 10f, 2000f)
        assertTrue("limit must be >= 0", v >= 0f)
    }
}
