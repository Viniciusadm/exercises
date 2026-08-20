package com.archieapps.exercises

import com.archieapps.exercises.core.anatomy.BodyRegionsAsset
import com.archieapps.exercises.core.net.ApiJson
import com.archieapps.exercises.core.net.Envelope
import com.archieapps.exercises.core.net.dto.CatalogMetaDto
import com.archieapps.exercises.core.net.dto.ExerciseDto
import com.archieapps.exercises.core.net.dto.ExerciseMuscleMappingDto
import com.archieapps.exercises.core.net.dto.MuscleAnalysisDto
import com.archieapps.exercises.core.net.dto.MuscleAttributionDto
import com.archieapps.exercises.core.net.dto.MuscleDto
import com.archieapps.exercises.core.net.dto.MuscleHeatmapDto
import com.archieapps.exercises.core.net.dto.MuscleRecommendationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.io.File
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Test

/**
 * Fixtures gerados pelo próprio código do backend (rotas reais, resource() real),
 * então este teste trava o contrato anatômico entre o Laravel e os DTOs do app.
 */
class AnatomyContractTest {
    private val activationTypes = setOf("primary", "secondary", "stabilizer")

    @Test
    fun `exercise payload keeps muscle group and adds the anatomical mapping`() {
        val exercises = decode<List<ExerciseDto>>("exercises_with_muscles_response.json").body!!
        val legPress = exercises.first { it.name == "Leg press" }

        assertEquals("Pernas", legPress.muscleGroup)
        assertTrue(legPress.musclesMapped)
        assertNull(legPress.musclesUnmappedReason)
        assertEquals(listOf("Vasto medial"), legPress.muscles.primary.map { it.name })
        assertEquals(listOf("Glúteo máximo"), legPress.muscles.secondary.map { it.name })
        assertEquals(listOf("Reto abdominal"), legPress.muscles.stabilizer.map { it.name })
        assertEquals(3, legPress.muscles.all.size)
        assertFalse(legPress.muscles.isEmpty)
    }

    @Test
    fun `an exercise left unmapped on purpose carries the reason instead of an empty gap`() {
        val exercises = decode<List<ExerciseDto>>("exercises_with_muscles_response.json").body!!
        val cardio = exercises.first { it.name == "Esteira" }

        assertFalse(cardio.musclesMapped)
        assertTrue(cardio.muscles.isEmpty)
        assertNotNull(cardio.musclesUnmappedReason)
    }

    @Test
    fun `every mapped muscle carries the regions the map needs to paint`() {
        val exercises = decode<List<ExerciseDto>>("exercises_with_muscles_response.json").body!!

        val mapped = exercises.flatMap { it.muscles.all }
        assertTrue(mapped.isNotEmpty())
        assertTrue(mapped.all { it.svgRegions.isNotEmpty() })
        assertTrue(mapped.all { it.slug.isNotBlank() })
        assertTrue(mapped.all { it.region.isNotBlank() })
        assertTrue(mapped.all { !it.regionLabel.isNullOrBlank() })
    }

    @Test
    fun `a finer muscle points at the region of its aggregate`() {
        val exercises = decode<List<ExerciseDto>>("exercises_with_muscles_response.json").body!!
        val vasto = exercises.flatMap { it.muscles.all }.first { it.slug == "vasto-medial" }

        assertNotNull(vasto.parentId)
        assertEquals(listOf("quads-left", "quads-right"), vasto.svgRegions)
        assertEquals("lower_body", vasto.region)
        assertEquals("Membros inferiores", vasto.regionLabel)
        assertEquals("Vastus medialis", vasto.anatomicalName)
    }

    @Test
    fun `the muscle catalog exposes the hierarchy and the legacy bridge`() {
        val muscles = decode<List<MuscleDto>>("muscles_response.json").body!!

        assertTrue(muscles.isNotEmpty())
        assertTrue(muscles.all { it.slug.isNotBlank() })

        val root = muscles.first { it.slug == "quadriceps" }
        val child = muscles.first { it.slug == "vasto-medial" }

        assertNull(root.parentSlug)
        assertEquals("quadriceps", child.parentSlug)
        assertEquals(root.id, child.parentId)
        assertEquals("Pernas", child.legacyMuscleGroup)
    }

    @Test
    fun `the per exercise mapping endpoint groups muscles by activation`() {
        val mapping = decode<ExerciseMuscleMappingDto>("exercise_muscles_response.json").body!!

        assertEquals("Leg press", mapping.exercise.name)
        assertEquals("Pernas", mapping.exercise.muscleGroup)
        assertTrue(mapping.musclesMapped)
        assertEquals(1, mapping.muscles.primary.size)
        assertEquals(1, mapping.muscles.secondary.size)
        assertEquals(1, mapping.muscles.stabilizer.size)
    }

    @Test
    fun `catalog meta keeps the old keys and adds the muscle vocabulary`() {
        val meta = decode<CatalogMetaDto>("catalog_meta_response.json").body!!

        assertTrue(meta.muscleGroups.isNotEmpty())
        assertTrue(meta.executionTypes.isNotEmpty())
        assertTrue(meta.workoutTypes.isNotEmpty())

        assertTrue(meta.muscles.isNotEmpty())
        assertEquals(activationTypes, meta.activationTypes.toSet())
        assertTrue(meta.muscleRegions.contains("lower_body"))
        assertEquals("Membros inferiores", meta.muscleRegionLabels["lower_body"])
        assertTrue(meta.muscleRegions.all { meta.muscleRegionLabels.containsKey(it) })
        assertEquals(0, meta.unmappedExercisesCount)
    }

    @Test
    fun `the muscle vocabulary in meta covers every region used by the catalog`() {
        val meta = decode<CatalogMetaDto>("catalog_meta_response.json").body!!
        val catalog = decode<List<MuscleDto>>("muscles_response.json").body!!

        assertEquals(catalog.map { it.slug }.toSet(), meta.muscles.map { it.slug }.toSet())
        assertTrue(catalog.all { meta.muscleRegions.contains(it.region) })
    }


    @Test
    fun `the analysis payload maps into the muscle dashboard`() {
        val analysis = decode<MuscleAnalysisDto>("muscle_analysis_response.json").body!!

        assertEquals("2026-06-01", analysis.range.from)
        assertEquals("2026-06-30", analysis.range.to)
        assertEquals(5, analysis.range.weeks)
        assertEquals("as_mapped", analysis.granularity)
        assertTrue(analysis.muscles.isNotEmpty())
        assertTrue(analysis.regions.isNotEmpty())
        assertTrue(analysis.muscles.all { it.relativeStatus.isNotBlank() })
        assertTrue(analysis.muscles.all { it.absoluteStatus.isNotBlank() })
    }

    @Test
    fun `muscle rows never carry a globally additive share`() {
        val analysis = decode<MuscleAnalysisDto>("muscle_analysis_response.json").body!!
        val total = analysis.muscles.sumOf { it.shareOfRegion }

        assertTrue("share_of_region soma por regiao, nao globalmente", total > 1.0)
        analysis.regions.forEach { region ->
            val inRegion = analysis.muscles.filter { it.region == region.region }
            if (inRegion.any { it.volumeKg > 0.0 }) {
                assertEquals(1.0, inRegion.sumOf { it.shareOfRegion }, 0.02)
            }
        }
    }

    @Test
    fun `a muscle only recruited as stabilizer is not reported as untrained`() {
        val analysis = decode<MuscleAnalysisDto>("muscle_analysis_response.json").body!!
        val erectors = analysis.muscles.first { it.slug == "eretores-da-espinha" }

        assertEquals("stabilizer_only", erectors.relativeStatus)
        assertTrue("recebeu series de verdade", erectors.sets > 0)
        assertEquals(0.0, erectors.setsPerWeek, 0.001)
        assertTrue(erectors.volumeKg > 0.0)

        assertEquals(
            listOf("eretores-da-espinha"),
            analysis.highlights.stabilizerOnly.map { it.slug },
        )
    }

    @Test
    fun `a muscle with no exercise at all is reported as untrained`() {
        val analysis = decode<MuscleAnalysisDto>("muscle_analysis_response.json").body!!
        val tibial = analysis.muscles.first { it.slug == "tibial-anterior" }

        assertEquals("untrained", tibial.relativeStatus)
        assertEquals(0, tibial.sets)
        assertEquals(listOf("tibial-anterior"), analysis.highlights.untrained.map { it.slug })
    }

    @Test
    fun `an aggregate parent is absent when its portions carry the volume`() {
        val analysis = decode<MuscleAnalysisDto>("muscle_analysis_response.json").body!!

        assertNull(analysis.muscles.firstOrNull { it.slug == "quadriceps" })
        assertTrue(analysis.muscles.any { it.slug == "vasto-medial" && it.parentSlug == "quadriceps" })
    }

    @Test
    fun `a time based exercise contributes sets and seconds but no tonnage`() {
        val analysis = decode<MuscleAnalysisDto>("muscle_analysis_response.json").body!!
        val abs = analysis.muscles.first { it.slug == "reto-abdominal" }

        assertTrue(abs.sets > 0)
        assertEquals(0.0, abs.volumeKg, 0.001)
        assertTrue(abs.setsPerWeek > 0.0)
    }

    @Test
    fun `the attribution block reaches the app through misc`() {
        val envelope = decode<MuscleAnalysisDto>("muscle_analysis_response.json")
        val misc = requireNotNull(envelope.misc) { "misc foi descartado pelo Envelope" }
        val attribution = ApiJson.decodeFromJsonElement<MuscleAttributionDto>(misc)

        assertTrue(attribution.doubleCounted)
        assertTrue(attribution.attributedVolumeKg > attribution.sessionVolumeKg)
        assertEquals(4, attribution.unmapped.sets)
        assertEquals(listOf("Esteira"), attribution.unmapped.exerciseNames)
    }


    @Test
    fun `every region the backend asks to paint exists in the shipped asset`() {
        val asset = File("src/main/assets/anatomy/body_regions_v1.json")
        assertTrue("asset nao encontrado em ${asset.absolutePath}", asset.exists())

        val shipped = ApiJson.decodeFromString<BodyRegionsAsset>(asset.readText())
            .regions.map { it.id }.toSet()

        val fromAnalysis = decode<MuscleAnalysisDto>("muscle_analysis_response.json").body!!
            .muscles.flatMap { it.svgRegions }.toSet()
        val fromCatalog = decode<List<MuscleDto>>("muscles_response.json").body!!
            .flatMap { it.svgRegions }.toSet()
        val fromHeatmap = decode<MuscleHeatmapDto>("muscle_heatmap_response.json").body!!
            .regions.map { it.svgRegion }.toSet()

        assertTrue(fromAnalysis.isNotEmpty())
        assertTrue(fromHeatmap.isNotEmpty())

        listOf("analysis" to fromAnalysis, "catalog" to fromCatalog, "heatmap" to fromHeatmap)
            .forEach { (source, referenced) ->
                assertEquals(
                    "regiões que o backend ($source) manda pintar e o asset não conhece",
                    emptySet<String>(),
                    referenced - shipped,
                )
            }
    }

    @Test
    fun `the heatmap covers every region so the app never needs absence logic`() {
        val heatmap = decode<MuscleHeatmapDto>("muscle_heatmap_response.json").body!!

        assertEquals(89, heatmap.regions.size)
        assertEquals("sets", heatmap.metric)
        assertEquals("sets_per_week", heatmap.legend.unit)
        assertTrue(heatmap.maxValue > 0.0)
        assertTrue(heatmap.regions.all { it.intensity in 0.0..1.0 })
        assertTrue(heatmap.regions.any { it.intensity == 0.0 })
        assertTrue(heatmap.regions.any { it.intensity > 0.0 })
    }

    @Test
    fun `a region painted by several portions reports the dominant one first`() {
        val heatmap = decode<MuscleHeatmapDto>("muscle_heatmap_response.json").body!!
        val quads = heatmap.regions.first { it.svgRegion == "quads-left" }

        assertTrue("a região deveria ter mais de um músculo anatômico", quads.muscles.size > 1)
        assertEquals(quads.muscles.maxOf { it.value }, quads.muscles.first().value, 0.001)
        assertEquals(quads.value, quads.muscles.first().value, 0.001)
        assertTrue(
            "o valor da região é o máximo, nunca a soma",
            quads.value < quads.muscles.sumOf { it.value } + 0.001 &&
                quads.value <= quads.muscles.maxOf { it.value } + 0.001,
        )
    }


    @Test
    fun `recommendations map into the actionable list`() {
        val recommendations = decode<List<MuscleRecommendationDto>>(
            "muscle_recommendations_response.json"
        ).body!!

        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.all { it.muscle.slug.isNotBlank() })
        assertTrue(recommendations.all { it.message.isNotBlank() })
        assertTrue(recommendations.all { it.relativeStatus.isNotBlank() })
        assertEquals(10.0, recommendations.first().targetSetsPerWeek, 0.001)
    }

    @Test
    fun `every recommendation offers at least one way out`() {
        val recommendations = decode<List<MuscleRecommendationDto>>(
            "muscle_recommendations_response.json"
        ).body!!

        recommendations.forEach { recommendation ->
            assertTrue(
                "${recommendation.muscle.slug} veio sem sugestão nenhuma",
                recommendation.hasSuggestions,
            )
        }
    }

    @Test
    fun `an exercise from the catalog is told apart from a new suggestion`() {
        val recommendations = decode<List<MuscleRecommendationDto>>(
            "muscle_recommendations_response.json"
        ).body!!

        val withHistory = recommendations.first { it.fromYourHistory.isNotEmpty() }
        val known = withHistory.fromYourHistory.first()

        assertTrue("exercício do histórico precisa de id para virar ação", known.inCatalog)
        assertNotNull(known.exerciseId)
        assertTrue(known.sessionsCount > 0)
        assertNotNull(known.lastPerformedAt)
        assertNotNull(known.daysSince)

        val fresh = withHistory.newOptions.first()
        assertFalse("sugestão nova não pode fingir estar no catálogo", fresh.inCatalog)
        assertNull(fresh.exerciseId)
        assertEquals(0, fresh.sessionsCount)
    }

    @Test
    fun `suggestions are ranked from the most isolated`() {
        val recommendations = decode<List<MuscleRecommendationDto>>(
            "muscle_recommendations_response.json"
        ).body!!

        val multiple = recommendations.first { it.newOptions.size > 1 }

        assertEquals(
            listOf(1, 2),
            multiple.newOptions.take(2).map { it.isolationRank },
        )
        assertTrue(
            "o mais isolado vem primeiro",
            multiple.newOptions[0].secondaryCount <= multiple.newOptions[1].secondaryCount,
        )
    }

    @Test
    fun `a muscle only recruited as stabilizer also gets a recommendation`() {
        val recommendations = decode<List<MuscleRecommendationDto>>(
            "muscle_recommendations_response.json"
        ).body!!

        val stabilizerOnly = recommendations.filter { it.relativeStatus == "stabilizer_only" }

        assertTrue(stabilizerOnly.isNotEmpty())
        assertTrue(stabilizerOnly.all { it.newOptions.isNotEmpty() })
        assertTrue(stabilizerOnly.all { it.setsPerWeek == 0.0 })
    }

    private inline fun <reified T> decode(fixture: String): Envelope<T> {
        val json = requireNotNull(javaClass.classLoader?.getResourceAsStream(fixture)) {
            "fixture $fixture não encontrado"
        }.bufferedReader().use { it.readText() }

        return ApiJson.decodeFromString(json)
    }
}
