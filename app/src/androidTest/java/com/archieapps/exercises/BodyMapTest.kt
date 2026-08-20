package com.archieapps.exercises

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.archieapps.exercises.core.anatomy.BodyGeometryLoader
import com.archieapps.exercises.core.anatomy.BodyRegion
import com.archieapps.exercises.core.anatomy.BodyView
import com.archieapps.exercises.core.net.ApiJson
import com.archieapps.exercises.ui.component.BodyMap
import com.archieapps.exercises.ui.component.BodyMapLegend
import com.archieapps.exercises.ui.component.MuscleRegionValue
import com.archieapps.exercises.ui.design.ExercisesTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class BodyMapTest {
    @get:Rule
    val compose = createComposeRule()

    private val geometry by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking { BodyGeometryLoader(context, ApiJson).load() }
    }

    private val values = listOf(
        MuscleRegionValue("quads-left", "Vasto medial", 1f, detail = "8 sér/sem"),
        MuscleRegionValue("quads-right", "Vasto medial", 1f, detail = "8 sér/sem"),
    )

    private fun interiorPoint(region: BodyRegion): Offset? {
        region.prepareHitTesting()

        val steps = 16
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
    fun the_map_announces_itself_to_accessibility() {
        compose.setContent {
            ExercisesTheme {
                BodyMap(
                    geometry = geometry,
                    values = values,
                    view = BodyView.FRONT,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = "Mapa anatômico, vista frontal",
                )
            }
        }

        compose.onNodeWithContentDescription("Mapa anatômico, vista frontal").assertIsDisplayed()
    }

    @Test
    fun a_tap_on_the_thigh_reports_the_quadriceps_region() {
        var tapped: MuscleRegionValue? = null

        compose.setContent {
            ExercisesTheme {
                BodyMap(
                    geometry = geometry,
                    values = values,
                    view = BodyView.FRONT,
                    modifier = Modifier.fillMaxWidth(),
                    onRegionTap = { tapped = it },
                    contentDescription = "mapa",
                )
            }
        }

        val loaded = assertNotNull(geometry).let { geometry!! }
        val box = loaded.viewBox(BodyView.FRONT)
        val quads = loaded.regionsFor(BodyView.FRONT).first { it.id == "quads-left" }

        quads.prepareHitTesting()
        val interior = requireNotNull(interiorPoint(quads)) { "quads-left sem ponto interior" }

        compose.onNodeWithContentDescription("mapa").performTouchInput {
            val fractionX = (interior.x - box.left) / box.width
            val fractionY = (interior.y - box.top) / box.height

            click(Offset(width * fractionX, height * fractionY))
        }

        compose.waitForIdle()

        assertNotNull("o toque na coxa deveria resolver alguma regiao", tapped)
        assertEquals("quads-left", tapped?.regionId)
    }

    @Test
    fun the_legend_is_focusable_and_selects_the_same_region() {
        var selected: MuscleRegionValue? = null

        compose.setContent {
            ExercisesTheme {
                BodyMapLegend(
                    values = values,
                    onSelect = { selected = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        compose.onNodeWithContentDescription("Vasto medial, 8 sér/sem").performClick()
        compose.waitForIdle()

        assertEquals("quads-left", selected?.regionId)
    }
}
