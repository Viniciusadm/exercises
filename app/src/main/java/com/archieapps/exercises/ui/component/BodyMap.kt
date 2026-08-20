package com.archieapps.exercises.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.anatomy.BodyGeometry
import com.archieapps.exercises.core.anatomy.BodyView
import com.archieapps.exercises.core.anatomy.fitTransform
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronBorderStrong
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronPrimaryGlow
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MuscleRegionValue(
    val regionId: String,
    val muscleName: String,
    val intensity: Float,
    val activation: MuscleActivation? = null,
    val detail: String? = null,
)

private val TOUCH_TOLERANCE_DP = 6.dp

/**
 * A figura e alta e estreita (proporcao ~0.3), entao o chamador deve restringir a
 * ALTURA: com a largura mandando, a proporcao pediria mais de mil dp de altura.
 */
@Composable
fun BodyMap(
    geometry: BodyGeometry?,
    values: List<MuscleRegionValue>,
    view: BodyView,
    modifier: Modifier = Modifier,
    selectedRegionId: String? = null,
    interactive: Boolean = true,
    onRegionTap: (MuscleRegionValue?) -> Unit = {},
    contentDescription: String? = null,
) {
    if (geometry == null) {
        LoadingSkeleton(modifier)

        return
    }

    val viewBox = geometry.viewBox(view)
    val regions = geometry.regionsFor(view)
    val byId = remember(values) { values.associateBy { it.regionId } }
    val density = LocalDensity.current

    val touchable = interactive && geometry.hitTestingAvailable

    LaunchedEffect(geometry, view, touchable) {
        if (touchable) withContext(Dispatchers.Default) { geometry.prepareHitTesting(view) }
    }

    // Larguras em px de tela. Dentro do withTransform as unidades sao do viewBox, entao
    // cada uma e dividida pela escala real no momento do desenho para a espessura
    // aparente nao mudar junto com o tamanho do canvas.
    val outlinePx = with(density) { 0.75.dp.toPx() }
    val primaryPx = with(density) { 1.5.dp.toPx() }
    val dashedPx = with(density) { 1.dp.toPx() }
    val glowPx = with(density) { 6.dp.toPx() }
    val edgePx = with(density) { 2.dp.toPx() }
    val dashPx = with(density) { 3.dp.toPx() }

    Canvas(
        modifier = modifier
            .aspectRatio(viewBox.width / viewBox.height)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .then(
                if (touchable) {
                    Modifier.pointerInput(geometry, view, values) {
                        detectTapGestures { offset ->
                            val transform = fitTransform(size.width.toFloat(), size.height.toFloat(), viewBox)
                            val point = transform.toViewBox(offset)
                            val tolerance = with(density) { TOUCH_TOLERANCE_DP.toPx() } /
                                transform.scale.coerceAtLeast(0.0001f)
                            val region = geometry.regionAt(view, point, tolerance)

                            onRegionTap(
                                region?.let {
                                    byId[it.id] ?: MuscleRegionValue(it.id, it.name, 0f)
                                }
                            )
                        }
                    }
                } else {
                    Modifier
                }
            ),
    ) {
        val transform = fitTransform(size.width, size.height, viewBox)
        val unit = 1f / transform.scale.coerceAtLeast(0.0001f)

        val outline = Stroke(width = outlinePx * unit)
        val primaryStroke = Stroke(width = primaryPx * unit)
        val dashed = Stroke(
            width = dashedPx * unit,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx * unit, dashPx * unit * 0.8f)),
        )

        withTransform({
            translate(transform.dx, transform.dy)
            scale(transform.scale, transform.scale, pivot = Offset.Zero)
        }) {
            regions.forEach { region ->
                val value = byId[region.id]
                val activation = value?.activation

                drawPath(path = region.path, color = fillFor(value?.intensity ?: 0f, activation))

                strokeFor(activation, outline, primaryStroke, dashed)?.let { (color, stroke) ->
                    drawPath(path = region.path, color = color, style = stroke)
                }
            }

            selectedRegionId?.let { selected ->
                regions.firstOrNull { it.id == selected }?.let { region ->
                    drawPath(path = region.path, color = IronPrimaryGlow, style = Stroke(glowPx * unit))
                    drawPath(path = region.path, color = IronTextPrimary, style = Stroke(edgePx * unit))
                }
            }
        }
    }
}

/**
 * Tres canais independentes em vez de matiz: preenchimento, presenca de contorno e
 * traco. No tema dark, matiz sozinho nao distingue de forma legivel, e falha para quem
 * tem daltonismo.
 */
private fun fillFor(intensity: Float, activation: MuscleActivation?): Color = when (activation) {
    MuscleActivation.PRIMARY -> IronPrimary.copy(alpha = 0.95f)
    MuscleActivation.SECONDARY -> IronPrimary.copy(alpha = 0.40f)
    MuscleActivation.STABILIZER -> IronSurfaceHigh
    null -> if (intensity <= 0f) IronSurface else heatColor(intensity)
}

private fun strokeFor(
    activation: MuscleActivation?,
    outline: Stroke,
    primary: Stroke,
    dashed: Stroke,
): Pair<Color, Stroke>? = when (activation) {
    MuscleActivation.PRIMARY -> IronPrimary to primary
    MuscleActivation.SECONDARY -> null
    MuscleActivation.STABILIZER -> IronBorderStrong to dashed
    null -> IronBorder to outline
}

@Composable
fun BodyViewToggle(
    view: BodyView,
    onChange: (BodyView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(IronSurfaceHigh, RoundedCornerShape(999.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        BodyView.entries.forEach { entry ->
            val selected = entry == view
            val label = if (entry == BodyView.FRONT) "Frente" else "Costas"

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) IronTextPrimary else IronTextSecondary,
                modifier = Modifier
                    .background(
                        if (selected) IronPrimary.copy(alpha = 0.22f) else Color.Transparent,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable(onClickLabel = "Ver vista $label") { onChange(entry) }
                    .semantics { role = Role.Tab }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
    }
}

/**
 * A legenda e a afordancia acessivel primaria: um Canvas unico nao consegue expor um no
 * de semantics por regiao, e 89 nos fantasma seria absurdo. Aqui cada musculo com dado e
 * uma linha focavel e clicavel com a mesma selecao do mapa.
 */
@Composable
fun BodyMapLegend(
    values: List<MuscleRegionValue>,
    onSelect: (MuscleRegionValue) -> Unit,
    modifier: Modifier = Modifier,
    selectedRegionId: String? = null,
) {
    val rows = remember(values) {
        values.filter { it.intensity > 0f }
            .distinctBy { it.muscleName }
            .sortedByDescending { it.intensity }
    }

    if (rows.isEmpty()) return

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        rows.forEach { row ->
            val selected = row.regionId == selectedRegionId

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = row.muscleName) { onSelect(row) }
                    .semantics {
                        role = Role.Button
                        contentDescription = listOfNotNull(row.muscleName, row.detail).joinToString(", ")
                    }
                    .background(
                        if (selected) IronPrimary.copy(alpha = 0.12f) else Color.Transparent,
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Box(
                        Modifier
                            .padding(top = 5.dp)
                            .drawBehind {
                                drawRoundRect(
                                    color = heatColor(row.intensity),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                                )
                            }
                            .padding(horizontal = 5.dp, vertical = 5.dp),
                    )
                    Text(
                        text = row.muscleName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) IronTextPrimary else IronTextSecondary,
                    )
                }
                row.detail?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = IronTextSecondary,
                    )
                }
            }
        }
    }
}
