package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronNegative
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun MetricValue(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    color: Color = IronTextPrimary,
    style: androidx.compose.ui.text.TextStyle = IronText.metric,
    unitColor: Color = IronTextMuted,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(text = value, style = style, color = color)
        if (unit != null) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = unitColor,
                modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    spark: List<Double> = emptyList(),
    accent: Color = IronPrimary,
) {
    val shape = MaterialTheme.shapes.medium

    Column(
        modifier = modifier
            .background(IronSurface, shape)
            .border(1.dp, IronBorder, shape)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = IronTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        MetricValue(value = value, unit = unit, style = IronText.metricSmall)
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = IronTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (spark.size > 1) {
            Spacer(Modifier.weight(1f, fill = true).heightIn(min = Spacing.sm))
            Sparkline(
                values = spark,
                color = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
            )
        }
    }
}

@Composable
fun DeltaBadge(delta: Double?, modifier: Modifier = Modifier, suffix: String = "kg") {
    if (delta == null || delta == 0.0) return

    val positive = delta > 0
    val color = if (positive) IronPositive else IronNegative

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = Spacing.sm, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${Format.signed(delta)} $suffix",
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
fun PillTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = IronTextSecondary,
    background: Color = IronSurfaceHigh,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = Spacing.md, vertical = 5.dp),
    )
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(text = value, style = IronText.tabular, color = IronTextSecondary)
        trailing?.invoke()
    }
}
