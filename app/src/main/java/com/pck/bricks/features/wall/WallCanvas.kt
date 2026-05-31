package com.pck.bricks.features.wall

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pck.bricks.core.model.TierType
import com.pck.bricks.features.wall.animation.rememberBrickLayScale

private val bronzeColor = Color(0xFFB85C3C)
private val silverColor = Color(0xFF787878)
private val goldColor   = Color(0xFFC9A227)
private val gapColor    = Color(0xFF2A2A2A)
private val emptyColor  = Color(0x33FFFFFF)

private fun tierColor(tier: TierType) = when (tier) {
    TierType.BRONZE -> bronzeColor
    TierType.SILVER -> silverColor
    TierType.GOLD   -> goldColor
}

@Composable
fun WallCanvas(
    wallModel: WallRenderModel,
    modifier: Modifier = Modifier
) {
    val layout = wallModel.layout
    val newlyAdded = wallModel.newlyAddedIndex
    val brickLayScale = rememberBrickLayScale(newlyAdded)
    // Read animated value in composition scope so recomposition drives Canvas redraws
    val animScale = brickLayScale.value

    val cols = layout.columns
    val rows = layout.rows
    val aspectRatio = cols.toFloat() / rows

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    ) {
        val mortarPx = 3.dp.toPx()
        val brickW = size.width / cols
        val brickH = size.height / rows
        val cornerPx = 3.dp.toPx()

        for (brick in wallModel.bricks) {
            val col = brick.index % cols
            val row = brick.index / cols

            val scale = if (brick.index == newlyAdded) animScale else 1f
            val scaledW = (brickW - mortarPx) * scale
            val scaledH = (brickH - mortarPx) * scale
            val centerX = col * brickW + brickW / 2f
            val centerY = row * brickH + brickH / 2f
            val left = centerX - scaledW / 2f
            val top  = centerY - scaledH / 2f

            val color = when (brick.state) {
                BrickState.COMPLETED -> tierColor(wallModel.tier)
                BrickState.GAP       -> gapColor
                BrickState.EMPTY     -> emptyColor
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(scaledW, scaledH),
                cornerRadius = CornerRadius(cornerPx * scale)
            )
        }
    }
}
