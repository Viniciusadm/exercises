package com.archieapps.exercises

import com.archieapps.exercises.core.session.WeightLadder
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightLadderTest {
    private val machine = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    private val stack = listOf(35.0, 38.0, 41.0)

    @Test
    fun `the ladder walks to the next available weight`() {
        assertEquals(5.0, WeightLadder.up(4.0, machine), 0.0)
        assertEquals(4.0, WeightLadder.down(5.0, machine), 0.0)
        assertEquals(38.0, WeightLadder.up(35.0, stack), 0.0)
        assertEquals(35.0, WeightLadder.down(38.0, stack), 0.0)
    }

    @Test
    fun `a weight outside the ladder lands on the neighbour it is heading to`() {
        assertEquals(38.0, WeightLadder.up(37.5, stack), 0.0)
        assertEquals(35.0, WeightLadder.down(37.5, stack), 0.0)
        assertEquals(1.0, WeightLadder.up(0.0, machine), 0.0)
    }

    @Test
    fun `the ladder stops at its ends instead of inventing a weight`() {
        assertEquals(10.0, WeightLadder.up(10.0, machine), 0.0)
        assertEquals(1.0, WeightLadder.down(1.0, machine), 0.0)
        assertEquals(50.0, WeightLadder.up(50.0, stack), 0.0)
        assertEquals(41.0, WeightLadder.down(50.0, stack), 0.0)
    }

    @Test
    fun `without a ladder the step is one kilo snapped to whole numbers`() {
        assertEquals(48.0, WeightLadder.up(47.25, emptyList()), 0.0)
        assertEquals(47.0, WeightLadder.down(47.25, emptyList()), 0.0)
        assertEquals(36.0, WeightLadder.up(35.0, emptyList()), 0.0)
        assertEquals(34.0, WeightLadder.down(35.0, emptyList()), 0.0)
    }

    @Test
    fun `steps stay inside the allowed range`() {
        assertEquals(0.0, WeightLadder.down(0.0, emptyList()), 0.0)
        assertEquals(0.0, WeightLadder.down(0.5, emptyList()), 0.0)
        assertEquals(999.0, WeightLadder.up(999.0, emptyList()), 0.0)
        assertEquals(1.0, WeightLadder.up(null, machine), 0.0)
    }

    @Test
    fun `a messy ladder is sorted and deduplicated before use`() {
        val messy = listOf(10.0, 2.5, 2.5, -4.0, 5.0)

        assertEquals(listOf(2.5, 5.0, 10.0), WeightLadder.sanitize(messy))
        assertEquals(5.0, WeightLadder.up(2.5, messy), 0.0)
    }
}
