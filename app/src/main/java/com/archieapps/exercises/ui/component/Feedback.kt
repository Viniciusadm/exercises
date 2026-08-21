package com.archieapps.exercises.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.ui.design.IronNegative
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )

    Spacer(
        modifier
            .alpha(alpha)
            .background(IronSurfaceHigh, RoundedCornerShape(12.dp))
    )
}

@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            repeat(3) {
                ShimmerBox(
                    Modifier
                        .weight(1f)
                        .height(92.dp)
                )
            }
        }
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
    }
}

@Composable
fun NoticeBanner(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val accent = if (isError) IronNegative else IronRecord

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            modifier = Modifier.weight(1f),
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.padding(horizontal = Spacing.xs))
            Text(
                text = actionText.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier
                    .background(accent.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = IronTextPrimary,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextSecondary,
                textAlign = TextAlign.Center,
            )
        }

        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(text = actionText, onClick = onAction)
        }
    }
}
