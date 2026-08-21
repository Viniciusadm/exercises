package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.net.dto.ExerciseMusclesDto
import com.archieapps.exercises.core.net.dto.ExerciseMusclesRequest
import com.archieapps.exercises.ui.design.IronBorderStrong
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

enum class MuscleActivation(val label: String, val shortLabel: String) {
    PRIMARY("Principais", "Principal"),
    SECONDARY("Auxiliares", "Auxiliar"),
    STABILIZER("Estabilizadores", "Estabilizador"),
}

data class MuscleSelection(
    val primary: List<Int> = emptyList(),
    val secondary: List<Int> = emptyList(),
    val stabilizer: List<Int> = emptyList(),
) {
    val total: Int get() = primary.size + secondary.size + stabilizer.size

    val isEmpty: Boolean get() = total == 0

    fun of(activation: MuscleActivation): List<Int> = when (activation) {
        MuscleActivation.PRIMARY -> primary
        MuscleActivation.SECONDARY -> secondary
        MuscleActivation.STABILIZER -> stabilizer
    }

    fun activationOf(muscleId: Int): MuscleActivation? = when (muscleId) {
        in primary -> MuscleActivation.PRIMARY
        in secondary -> MuscleActivation.SECONDARY
        in stabilizer -> MuscleActivation.STABILIZER
        else -> null
    }

    fun with(muscleId: Int, activation: MuscleActivation?): MuscleSelection {
        val cleared = MuscleSelection(
            primary = primary - muscleId,
            secondary = secondary - muscleId,
            stabilizer = stabilizer - muscleId,
        )

        return when (activation) {
            null -> cleared
            MuscleActivation.PRIMARY -> cleared.copy(primary = (cleared.primary + muscleId).sorted())
            MuscleActivation.SECONDARY -> cleared.copy(secondary = (cleared.secondary + muscleId).sorted())
            MuscleActivation.STABILIZER -> cleared.copy(stabilizer = (cleared.stabilizer + muscleId).sorted())
        }
    }

    fun toRequest() = ExerciseMusclesRequest(primary, secondary, stabilizer)

    companion object {
        fun from(muscles: ExerciseMusclesDto) = MuscleSelection(
            primary = muscles.primary.map { it.id }.sorted(),
            secondary = muscles.secondary.map { it.id }.sorted(),
            stabilizer = muscles.stabilizer.map { it.id }.sorted(),
        )
    }
}

private val PillShape = RoundedCornerShape(999.dp)

@Composable
fun MuscleChipsRow(
    muscles: ExerciseMusclesDto,
    modifier: Modifier = Modifier,
    fallback: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
) {
    if (muscles.isEmpty) {
        if (!fallback.isNullOrBlank()) {
            PillTag(text = fallback, modifier = modifier)
        }

        return
    }

    val entries = muscles.primary.map { it to MuscleActivation.PRIMARY } +
        muscles.secondary.map { it to MuscleActivation.SECONDARY } +
        muscles.stabilizer.map { it to MuscleActivation.STABILIZER }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = contentPadding,
    ) {
        items(entries, key = { (muscle, activation) -> "${activation.name}:${muscle.id}" }) { entry ->
            MuscleChip(name = entry.first.name, activation = entry.second)
        }
    }
}

@Composable
fun MuscleSelectionSummary(
    selection: MuscleSelection,
    names: Map<Int, String>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Nenhum músculo escolhido ainda.",
) {
    if (selection.isEmpty) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextMuted,
            modifier = modifier,
        )

        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        MuscleActivation.entries.forEach { activation ->
            val ids = selection.of(activation)
            if (ids.isEmpty()) return@forEach

            SectionLabel(text = activation.label)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ids.forEach { id ->
                    MuscleChip(name = names[id] ?: "Músculo $id", activation = activation)
                }
            }
        }
    }
}

@Composable
fun MuscleChip(name: String, activation: MuscleActivation, modifier: Modifier = Modifier) {
    val base = Modifier.padding(horizontal = Spacing.md, vertical = 5.dp)

    when (activation) {
        MuscleActivation.PRIMARY -> Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = IronTextPrimary,
            maxLines = 1,
            modifier = modifier
                .background(IronPrimary.copy(alpha = 0.18f), PillShape)
                .border(1.dp, IronPrimary, PillShape)
                .then(base),
        )

        MuscleActivation.SECONDARY -> Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = IronTextSecondary,
            maxLines = 1,
            modifier = modifier
                .background(IronSurfaceHigh, PillShape)
                .then(base),
        )

        MuscleActivation.STABILIZER -> Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = IronTextMuted,
            maxLines = 1,
            modifier = modifier.dashedOutline().then(base),
        )
    }
}

private fun Modifier.dashedOutline(color: Color = IronBorderStrong) = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
        ),
    )
}
