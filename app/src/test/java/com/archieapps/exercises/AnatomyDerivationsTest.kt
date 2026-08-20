package com.archieapps.exercises

import com.archieapps.exercises.core.net.dto.MuscleVolumeDto
import com.archieapps.exercises.ui.feature.anatomy.muscleIntensities
import com.archieapps.exercises.ui.feature.anatomy.setsPerWeekReference
import com.archieapps.exercises.ui.feature.anatomy.shareRows
import com.archieapps.exercises.ui.feature.anatomy.topMuscles
import com.archieapps.exercises.ui.feature.anatomy.trimZero
import com.archieapps.exercises.ui.design.muscleColorFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.archieapps.exercises.core.net.dto.ExerciseMusclesDto
import com.archieapps.exercises.core.net.dto.MuscleDto
import com.archieapps.exercises.ui.component.MuscleActivation
import com.archieapps.exercises.ui.feature.anatomy.activationRegions

class AnatomyDerivationsTest {
    private fun muscle(
        slug: String,
        setsPerWeek: Double,
        regions: List<String> = listOf("$slug-left", "$slug-right"),
    ) = MuscleVolumeDto(
        muscleId = slug.hashCode(),
        slug = slug,
        name = slug,
        svgRegions = regions,
        setsPerWeek = setsPerWeek,
    )

    @Test
    fun `the reference is the ninetieth percentile so one dominant muscle does not flatten the rest`() {
        val muscles = (1..9).map { muscle("m$it", it.toDouble()) } + muscle("dominante", 100.0)

        val reference = setsPerWeekReference(muscles)

        assertTrue("referencia deveria ignorar o dominante, veio $reference", reference < 20.0)
        assertEquals(9.0, reference, 0.01)
    }

    @Test
    fun `a single muscle normalizes to full intensity`() {
        val intensities = muscleIntensities(listOf(muscle("quadriceps", 8.0)))

        assertEquals(1f, intensities["quadriceps"]!!, 0.001f)
    }

    @Test
    fun `zero volume stays at zero and low volume keeps a visible floor`() {
        val muscles = listOf(muscle("forte", 20.0), muscle("fraco", 0.1), muscle("nada", 0.0))

        val intensities = muscleIntensities(muscles)

        assertEquals(0f, intensities["nada"]!!, 0.001f)
        assertTrue("piso deveria manter o fraco visivel", intensities["fraco"]!! >= 0.28f)
        assertTrue(intensities["fraco"]!! < intensities["forte"]!!)
    }

    @Test
    fun `an empty list does not blow up`() {
        assertEquals(emptyMap<String, Float>(), muscleIntensities(emptyList()))
        assertEquals(1.0, setsPerWeekReference(emptyList()), 0.001)
    }

    @Test
    fun `top muscles are ordered by sets per week and tie broken by name`() {
        val muscles = listOf(
            muscle("zebra", 5.0),
            muscle("alfa", 5.0),
            muscle("maior", 9.0),
        )

        assertEquals(listOf("maior", "alfa", "zebra"), topMuscles(muscles).map { it.slug })
        assertEquals(2, topMuscles(muscles, limit = 2).size)
    }

    @Test
    fun `share rows are relative to the strongest muscle`() {
        val rows = shareRows(listOf(muscle("forte", 10.0), muscle("meio", 5.0)))

        assertEquals(2, rows.size)
        assertEquals(1.0, rows[0].share, 0.001)
        assertEquals(0.5, rows[1].share, 0.001)
        assertTrue(rows[0].value.contains("sér/sem"))
    }

    @Test
    fun `muscle color is stable per identity and not per position`() {
        val first = muscleColorFor("gluteo-medio")
        val again = muscleColorFor("gluteo-medio")

        assertEquals(first, again)
        assertNotEquals(muscleColorFor("gluteo-medio"), muscleColorFor("gluteo-medio-x"))
    }


    private fun dto(slug: String, regions: List<String>) =
        MuscleDto(id = slug.hashCode(), slug = slug, name = slug, svgRegions = regions)

    @Test
    fun `activation regions colour every region the exercise touches`() {
        val regions = activationRegions(
            ExerciseMusclesDto(
                primary = listOf(dto("peitoral", listOf("chest-upper-left", "chest-lower-left"))),
                secondary = listOf(dto("deltoide", listOf("shoulder-front-left"))),
                stabilizer = listOf(dto("serratil", listOf("serratus-anterior-left"))),
            )
        )

        assertEquals(4, regions.size)
        val byId = regions.associateBy { it.regionId }
        assertEquals(MuscleActivation.PRIMARY, byId["chest-upper-left"]?.activation)
        assertEquals(MuscleActivation.SECONDARY, byId["shoulder-front-left"]?.activation)
        assertEquals(MuscleActivation.STABILIZER, byId["serratus-anterior-left"]?.activation)
        assertTrue(byId["chest-upper-left"]!!.intensity > byId["shoulder-front-left"]!!.intensity)
    }

    @Test
    fun `the strongest activation wins when two muscles claim the same region`() {
        val regions = activationRegions(
            ExerciseMusclesDto(
                primary = listOf(dto("triceps-lateral", listOf("triceps-lateral-left"))),
                stabilizer = listOf(dto("triceps-medial", listOf("triceps-lateral-left"))),
            )
        )

        assertEquals(1, regions.size)
        assertEquals(MuscleActivation.PRIMARY, regions.first().activation)
    }

    @Test
    fun `an exercise without mapping produces no regions`() {
        assertEquals(emptyList<Any>(), activationRegions(ExerciseMusclesDto()))
    }

    @Test
    fun `numbers drop the decimal when it adds nothing`() {
        assertEquals("8", trimZero(8.0))
        assertEquals("8,4", trimZero(8.42))
        assertEquals("0", trimZero(0.0))
    }
}
