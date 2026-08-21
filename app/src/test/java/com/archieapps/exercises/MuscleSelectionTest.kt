package com.archieapps.exercises

import com.archieapps.exercises.core.net.ApiJson
import com.archieapps.exercises.core.net.dto.ExerciseMusclesDto
import com.archieapps.exercises.core.net.dto.ExerciseMusclesRequest
import com.archieapps.exercises.core.net.dto.MuscleDto
import com.archieapps.exercises.ui.component.MuscleActivation
import com.archieapps.exercises.ui.component.MuscleSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleSelectionTest {
    @Test
    fun `a muscle only ever holds one activation`() {
        val selection = MuscleSelection()
            .with(7, MuscleActivation.SECONDARY)
            .with(7, MuscleActivation.PRIMARY)

        assertEquals(listOf(7), selection.primary)
        assertTrue(selection.secondary.isEmpty())
        assertTrue(selection.stabilizer.isEmpty())
        assertEquals(MuscleActivation.PRIMARY, selection.activationOf(7))
        assertEquals(1, selection.total)
    }

    @Test
    fun `moving through every activation never duplicates the muscle`() {
        var selection = MuscleSelection().with(3, MuscleActivation.PRIMARY)

        MuscleActivation.entries.forEach { activation ->
            selection = selection.with(3, activation)
            assertEquals(1, selection.total)
            assertEquals(activation, selection.activationOf(3))
        }
    }

    @Test
    fun `a null activation removes the muscle`() {
        val selection = MuscleSelection()
            .with(1, MuscleActivation.PRIMARY)
            .with(2, MuscleActivation.STABILIZER)
            .with(1, null)

        assertNull(selection.activationOf(1))
        assertEquals(MuscleActivation.STABILIZER, selection.activationOf(2))
        assertEquals(1, selection.total)
    }

    @Test
    fun `ids stay sorted so the dirty check ignores selection order`() {
        val forward = MuscleSelection()
            .with(9, MuscleActivation.PRIMARY)
            .with(2, MuscleActivation.PRIMARY)
        val backward = MuscleSelection()
            .with(2, MuscleActivation.PRIMARY)
            .with(9, MuscleActivation.PRIMARY)

        assertEquals(listOf(2, 9), forward.primary)
        assertEquals(forward, backward)
    }

    @Test
    fun `seeding from the mapping round trips into the request`() {
        val selection = MuscleSelection.from(
            ExerciseMusclesDto(
                primary = listOf(muscle(4), muscle(1)),
                secondary = listOf(muscle(8)),
                stabilizer = emptyList(),
            )
        )

        assertEquals(ExerciseMusclesRequest(listOf(1, 4), listOf(8), emptyList()), selection.toRequest())
    }

    @Test
    fun `the request keeps the activation keys the backend validates`() {
        val body = ApiJson.encodeToString(ExerciseMusclesRequest(listOf(1), listOf(2), listOf(3)))

        assertEquals("""{"primary":[1],"secondary":[2],"stabilizer":[3]}""", body)
    }

    @Test
    fun `an empty request still sends every key so clearing the mapping works`() {
        val body = ApiJson.encodeToString(ExerciseMusclesRequest())

        assertEquals("""{"primary":[],"secondary":[],"stabilizer":[]}""", body)
    }

    private fun muscle(id: Int) = MuscleDto(id = id, slug = "m$id", name = "Músculo $id")
}
