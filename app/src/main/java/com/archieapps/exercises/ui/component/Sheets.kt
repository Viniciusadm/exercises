package com.archieapps.exercises.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun IronBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.92f,
    bodyScrollable: Boolean = true,
    onBack: (() -> Unit)? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        onBack?.let { handler -> BackHandler(onBack = handler) }

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(IronBackground.copy(alpha = 0.6f))
                    .clickable(onClick = onDismissRequest)
            )

            Column(
                modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(IronSurface)
            ) {
                header?.let { content ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .padding(top = Spacing.lg),
                        content = content,
                    )
                }

                Box(Modifier.weight(1f)) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .then(if (bodyScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(horizontal = Spacing.lg)
                            .padding(top = Spacing.md, bottom = if (bodyScrollable) Spacing.lg else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        content = body,
                    )

                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(20.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, IronSurface)))
                    )
                }

                footer?.let { content ->
                    HairLine()
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(IronSurface)
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        content = content,
                    )
                }
            }
        }
    }
}
