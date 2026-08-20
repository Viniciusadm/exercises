package com.archieapps.exercises.core.anatomy

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.vector.PathParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class BodyView(val asset: String) {
    FRONT("front"),
    BACK("back"),
}

@Serializable
data class BodyRegionsAsset(
    @SerialName("schemaVersion") val schemaVersion: Int = 0,
    val source: String = "",
    @SerialName("sourceCommit") val sourceCommit: String = "",
    val license: String = "",
    val modifications: String = "",
    val regions: List<BodyRegionDto> = emptyList(),
)

@Serializable
data class BodyRegionDto(
    val id: String,
    val name: String,
    val view: String,
    val d: String,
)

class BodyRegion(
    val id: String,
    val name: String,
    val view: BodyView,
    val path: Path,
    val bounds: BodyBounds,
) {
    private var hitTester: PathHitTester? = null

    /**
     * Construir o hit-tester percorre cada segmento do path para montar a arvore de
     * intervalos, entao vale so quando a vista e realmente desenhada.
     */
    fun prepareHitTesting(): Boolean = runCatching {
        if (hitTester == null) hitTester = PathHitTester(path)
        true
    }.getOrDefault(false)

    fun contains(point: Offset): Boolean {
        if (point !in bounds) return false

        val tester = hitTester ?: return false

        return runCatching { tester.contains(point) }.getOrDefault(false)
    }
}

class BodyGeometry(
    val regions: List<BodyRegion>,
    private val viewBoxes: Map<BodyView, BodyBounds>,
    val provenance: BodyRegionsAsset,
) {
    private val byView: Map<BodyView, List<BodyRegion>> = regions.groupBy { it.view }
    private val prepared = mutableSetOf<BodyView>()

    var hitTestingAvailable: Boolean = true
        private set

    fun regionsFor(view: BodyView): List<BodyRegion> = byView[view] ?: emptyList()

    fun viewBox(view: BodyView): BodyBounds = viewBoxes[view] ?: BodyBounds(0f, 0f, 1f, 1f)

    fun prepareHitTesting(view: BodyView) {
        if (view in prepared) return

        val ok = regionsFor(view).all { it.prepareHitTesting() }
        prepared += view
        if (!ok) hitTestingAvailable = false
    }

    /**
     * Dois estagios: contencao exata, e um anel de amostras quando o dedo cai numa fresta.
     * Empate resolve pela MENOR area de bbox, porque a regiao mais especifica e a que o
     * usuario quis (traps-lower dentro da area de traps, por exemplo).
     */
    fun regionAt(view: BodyView, point: Offset, tolerance: Float = 0f): BodyRegion? {
        val candidates = regionsFor(view)
        if (candidates.isEmpty()) return null

        val exact = candidates.filter { it.contains(point) }
        if (exact.isNotEmpty()) return exact.minByOrNull { it.bounds.area() }

        if (tolerance <= 0f) return null

        val hits = mutableMapOf<BodyRegion, Int>()
        toleranceRing(point, tolerance).forEach { sample ->
            candidates.forEach { region ->
                if (region.contains(sample)) hits[region] = (hits[region] ?: 0) + 1
            }
        }

        if (hits.isEmpty()) return null

        val best = hits.values.max()

        return hits.filterValues { it == best }.keys
            .minByOrNull { distanceTo(point, Offset(it.bounds.centerX, it.bounds.centerY)) }
    }
}

class BodyGeometryLoader(context: Context, private val json: Json) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    @Volatile
    private var cached: BodyGeometry? = null

    suspend fun load(): BodyGeometry? {
        cached?.let { return it }

        return mutex.withLock {
            cached ?: withContext(Dispatchers.Default) { parse() }?.also { cached = it }
        }
    }

    private fun parse(): BodyGeometry? = runCatching {
        val raw = appContext.assets.open(ASSET).bufferedReader().use { it.readText() }
        val asset = json.decodeFromString<BodyRegionsAsset>(raw)

        val parsed = asset.regions.mapNotNull { dto ->
            val view = BodyView.entries.firstOrNull { it.asset == dto.view } ?: return@mapNotNull null
            val parser = PathParser().parsePathString(dto.d)
            val bounds = boundsOf(parser.toNodes()) ?: return@mapNotNull null

            BodyRegion(dto.id, dto.name, view, parser.toPath(), bounds)
        }

        if (parsed.isEmpty()) return@runCatching null

        val front = parsed.filter { it.view == BodyView.FRONT }.map { it.bounds }.reduceOrNull(BodyBounds::union)
        val back = parsed.filter { it.view == BodyView.BACK }.map { it.bounds }.reduceOrNull(BodyBounds::union)

        val boxes = when {
            front != null && back != null -> normalizeViewBoxes(front, back)
                .let { mapOf(BodyView.FRONT to it.first, BodyView.BACK to it.second) }

            front != null -> mapOf(BodyView.FRONT to front)
            back != null -> mapOf(BodyView.BACK to back)
            else -> emptyMap()
        }

        BodyGeometry(parsed, boxes, asset)
    }.getOrNull()

    private companion object {
        const val ASSET = "anatomy/body_regions_v1.json"
    }
}
