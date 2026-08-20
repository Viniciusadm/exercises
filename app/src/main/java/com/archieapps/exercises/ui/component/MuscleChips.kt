package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.archieapps.exercises.core.net.dto.MuscleDto
import com.archieapps.exercises.ui.design.IronBorderStrong
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

enum class MuscleActivation { PRIMARY, SECONDARY, STABILIZER }

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
fun MuscleActivationSection(
    muscles: ExerciseMusclesDto,
    modifier: Modifier = Modifier,
    unmappedReason: String? = null,
    emptyMessage: String = "Este exercício ainda não tem mapeamento anatômico.",
) {
    Column(modifier = modifier) {
        if (muscles.isEmpty) {
            Text(
                text = unmappedReason ?: emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextMuted,
            )

            return@Column
        }

        val groups = listOf(
            Triple("Principais", muscles.primary, MuscleActivation.PRIMARY),
            Triple("Auxiliares", muscles.secondary, MuscleActivation.SECONDARY),
            Triple("Estabilizadores", muscles.stabilizer, MuscleActivation.STABILIZER),
        ).filter { it.second.isNotEmpty() }

        groups.forEachIndexed { index, (label, list, activation) ->
            if (index > 0) Spacer(Modifier.height(Spacing.md))

            SectionLabel(text = label)
            Spacer(Modifier.height(Spacing.sm))
            MuscleList(list, activation)
        }
    }
}

@Composable
private fun MuscleList(muscles: List<MuscleDto>, activation: MuscleActivation) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(muscles, key = { it.id }) { muscle ->
            MuscleChip(name = muscle.name, activation = activation)
        }
    }
}

@Composable
private fun MuscleChip(name: String, activation: MuscleActivation) {
    val base = Modifier.padding(horizontal = Spacing.md, vertical = 5.dp)

    when (activation) {
        MuscleActivation.PRIMARY -> Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = IronTextPrimary,
            maxLines = 1,
            modifier = Modifier
                .background(IronPrimary.copy(alpha = 0.18f), PillShape)
                .border(1.dp, IronPrimary, PillShape)
                .then(base),
        )

        MuscleActivation.SECONDARY -> Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = IronTextSecondary,
            maxLines = 1,
            modifier = Modifier
                .background(IronSurfaceHigh, PillShape)
                .then(base),
        )

        MuscleActivation.STABILIZER -> Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = IronTextMuted,
            maxLines = 1,
            modifier = Modifier.dashedOutline().then(base),
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
