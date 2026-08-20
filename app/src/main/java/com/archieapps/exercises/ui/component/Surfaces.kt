package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun IronCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val base = modifier
        .clip(shape)
        .background(IronSurface)
        .border(1.dp, IronBorder, shape)

    Column(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base).padding(contentPadding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, accent: Boolean = true) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (accent) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(IronPrimary)
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = IronTextMuted,
        )
    }
}

@Composable
fun CardHeader(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IronTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun HeroSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(IronSurfaceHigh, IronSurface))
            )
            .border(1.dp, IronBorder, shape)
            .padding(Spacing.xl),
        content = content,
    )
}

@Composable
fun HairLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(IronBorder)
    )
}
