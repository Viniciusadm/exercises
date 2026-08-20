package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.selected
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronPrimaryFaint
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) IronPrimary else IronTextSecondary,
        maxLines = 1,
        modifier = modifier
            .semantics { this.selected = selected }
            .background(if (selected) IronPrimaryFaint else IronSurfaceHigh, shape)
            .border(1.dp, if (selected) IronPrimary else IronBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}
