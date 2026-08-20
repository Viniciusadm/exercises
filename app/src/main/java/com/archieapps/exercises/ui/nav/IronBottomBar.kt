package com.archieapps.exercises.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.Spacing

private val BarHeight = 74.dp
private val ActionSize = 54.dp

@Composable
fun IronBottomBar(
    current: String?,
    onSelect: (TopLevelDestination) -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(IronSurface, IronBackground)))
            .navigationBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IronBorder)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarTab(TopLevelDestination.HOME, current, onSelect, Modifier.weight(1f))
            BarTab(TopLevelDestination.HISTORY, current, onSelect, Modifier.weight(1f))

            Box(
                Modifier
                    .padding(horizontal = Spacing.sm)
                    .size(ActionSize)
                    .shadow(16.dp, CircleShape, ambientColor = IronPrimary, spotColor = IronPrimary)
                    .clip(CircleShape)
                    .background(IronPrimary)
                    .clickable(onClick = onStartWorkout),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_dumbbell),
                    contentDescription = "Iniciar treino",
                    tint = IronBackground,
                    modifier = Modifier.size(26.dp),
                )
            }

            BarTab(TopLevelDestination.PROGRESS, current, onSelect, Modifier.weight(1f))
            BarTab(TopLevelDestination.CATALOG, current, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BarTab(
    destination: TopLevelDestination,
    current: String?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = current == destination.route
    val tint by animateColorAsState(
        targetValue = if (selected) IronPrimary else IronTextMuted,
        label = "tabTint",
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null) { onSelect(destination) }
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(width = 16.dp, height = 2.dp)
                .clip(CircleShape)
                .background(if (selected) IronPrimary else IronBackground.copy(alpha = 0f))
        )
        Spacer(Modifier.height(Spacing.sm))
        Icon(
            painter = painterResource(destination.iconRes),
            contentDescription = destination.label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
