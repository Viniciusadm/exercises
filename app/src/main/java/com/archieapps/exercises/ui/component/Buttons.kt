package com.archieapps.exercises.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronBorderStrong
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    iconRes: Int? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "primaryButtonScale")

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 54.dp),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = IronPrimary,
            contentColor = IronBackground,
            disabledContainerColor = IronSurfaceHigh,
            disabledContentColor = IronTextMuted,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = IronBackground,
                strokeWidth = 2.dp,
            )
        } else {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = IronBackground,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = if (iconRes != null) Spacing.sm else 0.dp),
            )
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, IronBorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = IronTextPrimary),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CircleIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = IronTextPrimary,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(IronSurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
