package com.archieapps.exercises

import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archieapps.exercises.core.anatomy.BodyGeometryLoader
import com.archieapps.exercises.core.anatomy.BodyRegion
import com.archieapps.exercises.core.anatomy.BodyView
import com.archieapps.exercises.core.net.ApiJson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BodyGeometryInstrumentedTest {
    private val geometry by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking { BodyGeometryLoader(context, ApiJson).load() }
    }

    private fun interiorPoint(region: BodyRegion): Offset? {
        region.prepareHitTesting()

        val center = Offset(region.bounds.centerX, region.bounds.centerY)
        if (region.contains(center)) return center

        val steps = 12
        for (row in 1 until steps) {
            for (column in 1 until steps) {
                val point = Offset(
                    region.bounds.left + region.bounds.width * column / steps,
                    region.bounds.top + region.bounds.height * row / steps,
                )
                if (region.contains(point)) return point
            }
        }

        return null
    }

    @Test
    fun the_asset_loads_and_covers_both_views() {
        val loaded = assertNotNull(geometry).let { geometry!! }

        assertEquals(89, loaded.regions.size)
        assertEquals(40, loaded.regionsFor(BodyView.FRONT).size)
        assertEquals(49, loaded.regionsFor(BodyView.BACK).size)
        assertTrue(loaded.viewBox(BodyView.FRONT).width > 0f)
        assertEquals(
            loaded.viewBox(BodyView.FRONT).width,
            loaded.viewBox(BodyView.BACK).width,
            0.001f,
        )
    }

    @Test
    fun hit_testing_is_available_on_this_device() {
        val loaded = geometry!!

        loaded.prepareHitTesting(BodyView.FRONT)
        loaded.prepareHitTesting(BodyView.BACK)

        assertTrue(
            "PathHitTester falhou: o mapa deve cair para nao-interativo neste aparelho",
            loaded.hitTestingAvailable,
        )
    }

    @Test
    fun every_region_has_a_reachable_interior_point() {
        val loaded = geometry!!
        val unreachable = loaded.regions.filter { interiorPoint(it) == null }.map { it.id }

        assertEquals("regiões sem ponto interior alcançável", emptyList<String>(), unreachable)
    }

    @Test
    fun a_tap_inside_a_region_resolves_to_that_region() {
        val loaded = geometry!!
        loaded.prepareHitTesting(BodyView.FRONT)

        val quads = loaded.regionsFor(BodyView.FRONT).first { it.id == "quads-left" }
        val point = assertNotNull(interiorPoint(quads)).let { interiorPoint(quads)!! }

        val hit = loaded.regionAt(BodyView.FRONT, point)

        assertNotNull(hit)
        assertTrue("regiao resolvida deve conter o ponto", hit!!.contains(point))
        assertEquals("quads-left", hit.id)
    }

    @Test
    fun neighbouring_thin_regions_are_told_apart() {
        val loaded = geometry!!
        loaded.prepareHitTesting(BodyView.BACK)

        listOf("traps-upper-left", "traps-mid-left", "traps-lower-left").forEach { id ->
            val region = loaded.regionsFor(BodyView.BACK).first { it.id == id }
            val point = interiorPoint(region)

            assertNotNull("$id: sem ponto interior", point)
            assertEquals(id, loaded.regionAt(BodyView.BACK, point!!)?.id)
        }
    }

    @Test
    fun a_tap_outside_the_figure_resolves_to_nothing() {
        val loaded = geometry!!
        loaded.prepareHitTesting(BodyView.FRONT)

        val box = loaded.viewBox(BodyView.FRONT)
        val outside = Offset(box.left - box.width, box.top - box.height)

        assertNull(loaded.regionAt(BodyView.FRONT, outside))
        assertNull(loaded.regionAt(BodyView.FRONT, outside, tolerance = 1f))
    }

    @Test
    fun the_tolerance_ring_recovers_a_tap_that_lands_just_outside_the_figure() {
        val loaded = geometry!!
        loaded.prepareHitTesting(BodyView.FRONT)

        val box = loaded.viewBox(BodyView.FRONT)

        val edge = loaded.regionsFor(BodyView.FRONT).minOf { it.bounds.left }
        val nearMiss = Offset(edge - 0.5f, box.centerY)

        assertNull("sem tolerancia o toque nao resolve", loaded.regionAt(BodyView.FRONT, nearMiss))
        assertNotNull(
            "com tolerancia generosa o toque deveria alcançar a silhueta",
            loaded.regionAt(BodyView.FRONT, nearMiss, tolerance = 3f),
        )
    }
}
