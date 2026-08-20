package com.archieapps.exercises

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import com.archieapps.exercises.core.anatomy.BodyBounds
import com.archieapps.exercises.core.anatomy.BodyRegionsAsset
import com.archieapps.exercises.core.anatomy.boundsOf
import com.archieapps.exercises.core.anatomy.fitTransform
import com.archieapps.exercises.core.anatomy.normalizeViewBoxes
import com.archieapps.exercises.core.anatomy.toleranceRing
import com.archieapps.exercises.core.net.ApiJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PathParser().parsePathString(d).toNodes() nao toca android.graphics, entao a matematica
 * de bounds roda em JVM. Ja toPath() e PathHitTester precisam do framework: isso e coberto
 * por BodyGeometryInstrumentedTest.
 */
class BodyGeometryMathTest {
    private val asset: BodyRegionsAsset by lazy {
        val file = File("src/main/assets/anatomy/body_regions_v1.json")
        assertTrue("asset nao encontrado em ${file.absolutePath}", file.exists())

        ApiJson.decodeFromString(file.readText())
    }

    private fun nodes(d: String): List<PathNode> = PathParser().parsePathString(d).toNodes()

    @Test
    fun `bounds of a simple square`() {
        val bounds = boundsOf(nodes("M 0,0 L 10,0 L 10,20 L 0,20 Z"))!!

        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(10f, bounds.right, 0.001f)
        assertEquals(20f, bounds.bottom, 0.001f)
        assertEquals(10f, bounds.width, 0.001f)
        assertEquals(20f, bounds.height, 0.001f)
        assertEquals(200f, bounds.area(), 0.001f)
    }

    @Test
    fun `relative commands accumulate from the current point`() {
        val bounds = boundsOf(nodes("m 5,5 l 10,0 l 0,10 z"))!!

        assertEquals(5f, bounds.left, 0.001f)
        assertEquals(5f, bounds.top, 0.001f)
        assertEquals(15f, bounds.right, 0.001f)
        assertEquals(15f, bounds.bottom, 0.001f)
    }

    @Test
    fun `close returns the current point to the subpath start`() {
        val bounds = boundsOf(nodes("m 0,0 l 10,0 l 0,10 z l -5,-5"))!!

        assertEquals(-5f, bounds.left, 0.001f)
        assertEquals(-5f, bounds.top, 0.001f)
    }

    @Test
    fun `horizontal and vertical commands keep the other axis`() {
        val bounds = boundsOf(nodes("M 2,3 H 12 V 13"))!!

        assertEquals(2f, bounds.left, 0.001f)
        assertEquals(3f, bounds.top, 0.001f)
        assertEquals(12f, bounds.right, 0.001f)
        assertEquals(13f, bounds.bottom, 0.001f)
    }

    @Test
    fun `curve control points are included as a conservative superset`() {
        val bounds = boundsOf(nodes("M 0,0 C 5,-10 15,10 20,0"))!!

        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(20f, bounds.right, 0.001f)
        assertEquals(-10f, bounds.top, 0.001f)
        assertEquals(10f, bounds.bottom, 0.001f)
    }

    @Test
    fun `an empty node list has no bounds`() {
        assertNull(boundsOf(emptyList()))
    }

    @Test
    fun `union and resize keep the geometry consistent`() {
        val a = BodyBounds(0f, 0f, 10f, 10f)
        val b = BodyBounds(5f, -5f, 20f, 5f)

        val union = a.union(b)
        assertEquals(0f, union.left, 0.001f)
        assertEquals(-5f, union.top, 0.001f)
        assertEquals(20f, union.right, 0.001f)
        assertEquals(10f, union.bottom, 0.001f)

        val resized = a.resizedAround(20f, 40f)
        assertEquals(5f, resized.centerX, 0.001f)
        assertEquals(5f, resized.centerY, 0.001f)
        assertEquals(20f, resized.width, 0.001f)
        assertEquals(40f, resized.height, 0.001f)
    }

    @Test
    fun `side by side views get a common size instead of a merged box`() {
        val front = BodyBounds(3f, 6f, 28f, 88f)
        val back = BodyBounds(40f, 6f, 65f, 88f)

        val (normalizedFront, normalizedBack) = normalizeViewBoxes(front, back)

        assertEquals(normalizedFront.width, normalizedBack.width, 0.001f)
        assertEquals(normalizedFront.height, normalizedBack.height, 0.001f)
        assertEquals(front.centerX, normalizedFront.centerX, 0.001f)
        assertEquals(back.centerX, normalizedBack.centerX, 0.001f)
        assertTrue(
            "a caixa nao deve abraçar as duas figuras",
            normalizedFront.width < front.union(back).width,
        )
    }

    @Test
    fun `fit transform centers the box and round trips a point`() {
        val bounds = BodyBounds(0f, 0f, 10f, 20f)
        val transform = fitTransform(200f, 200f, bounds)

        assertEquals(10f, transform.scale, 0.001f)

        val center = Offset(100f, 100f)
        val inViewBox = transform.toViewBox(center)

        assertEquals(bounds.centerX, inViewBox.x, 0.001f)
        assertEquals(bounds.centerY, inViewBox.y, 0.001f)
    }

    @Test
    fun `a degenerate canvas yields a neutral transform instead of dividing by zero`() {
        val transform = fitTransform(0f, 0f, BodyBounds(0f, 0f, 10f, 10f))

        assertEquals(0f, transform.scale, 0.001f)
        assertEquals(Offset.Zero, transform.toViewBox(Offset(5f, 5f)))
    }

    @Test
    fun `the tolerance ring samples two rings around the point`() {
        val ring = toleranceRing(Offset(10f, 10f), 4f)

        assertEquals(16, ring.size)
        assertTrue(ring.all { (it - Offset(10f, 10f)).getDistance() <= 4.01f })
        assertTrue(toleranceRing(Offset.Zero, 0f).isEmpty())
    }

    @Test
    fun `every shipped path parses and produces sane bounds`() {
        assertEquals(89, asset.regions.size)
        assertEquals(1, asset.schemaVersion)
        assertEquals("Apache-2.0", asset.license)
        assertTrue(asset.sourceCommit.length >= 40)

        asset.regions.forEach { region ->
            val parsed = nodes(region.d)
            assertTrue("${region.id}: path vazio", parsed.isNotEmpty())

            val bounds = boundsOf(parsed)
            assertNotNull("${region.id}: sem bounds", bounds)
            assertTrue("${region.id}: largura nao positiva", bounds!!.width > 0f)
            assertTrue("${region.id}: altura nao positiva", bounds.height > 0f)
        }
    }

    @Test
    fun `the shipped figure has a human aspect ratio in both views`() {
        val front = asset.regions.filter { it.view == "front" }
            .mapNotNull { boundsOf(nodes(it.d)) }
            .reduce(BodyBounds::union)
        val back = asset.regions.filter { it.view == "back" }
            .mapNotNull { boundsOf(nodes(it.d)) }
            .reduce(BodyBounds::union)

        listOf("front" to front, "back" to back).forEach { (label, bounds) ->
            val ratio = bounds.width / bounds.height
            assertTrue("$label: proporção $ratio fora do esperado para uma figura humana", ratio in 0.2f..0.7f)
        }
    }

    @Test
    fun `the asset declares which node types it actually uses`() {
        val types = asset.regions
            .flatMap { nodes(it.d) }
            .map { it::class.simpleName }
            .toSet()

        assertTrue("nenhum no reconhecido", types.isNotEmpty())
        assertTrue(
            "tipos inesperados no asset: $types",
            types.all { it in setOf("RelativeMoveTo", "MoveTo", "RelativeLineTo", "LineTo", "Close",
                "RelativeCurveTo", "CurveTo", "RelativeHorizontalTo", "HorizontalTo",
                "RelativeVerticalTo", "VerticalTo", "RelativeQuadTo", "QuadTo") },
        )
    }
}
