package com.pck.bricks.features.wall

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pck.bricks.core.model.TierType
import com.pck.bricks.features.wall.animation.rememberBrickLayScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val bronzeColor = Color(0xFFB85C3C)
private val silverColor = Color(0xFF787878)
private val goldColor   = Color(0xFFC9A227)
private val gapColor    = Color(0xFF2A2A2A)
private val emptyColor  = Color(0x33FFFFFF)
private val gapOverlay  = Color(0xCC000000)

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
    val animScale = brickLayScale.value

    val imageBitmap by produceState<ImageBitmap?>(null, wallModel.imagePath) {
        value = wallModel.imagePath?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }

    val cols = layout.columns
    val rows = layout.rows
    // Bricks are wide horizontal rectangles; canvas aspect ratio follows from brick shape
    val brickAr = 2.5f
    val aspectRatio = cols * brickAr / rows

    val mapper = ImageWallMapper()

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
            // Build bottom-up: index 0 is bottom-left, rows fill upward
            val row = rows - 1 - (brick.index / cols)

            val scale = if (brick.index == newlyAdded) animScale else 1f
            val scaledW = (brickW - mortarPx) * scale
            val scaledH = (brickH - mortarPx) * scale
            val centerX = col * brickW + brickW / 2f
            val centerY = row * brickH + brickH / 2f
            val left = centerX - scaledW / 2f
            val top  = centerY - scaledH / 2f

            if (brick.state == BrickState.EMPTY) {
                drawRoundRect(
                    color = emptyColor,
                    topLeft = Offset(left, top),
                    size = Size(scaledW, scaledH),
                    cornerRadius = CornerRadius(cornerPx * scale)
                )
                continue
            }

            val bitmap = imageBitmap
            if (bitmap != null && scaledW >= 1f && scaledH >= 1f) {
                val src = mapper.brickSrcRect(brick.index, layout, bitmap.width, bitmap.height)
                val brickPath = Path().apply {
                    addRoundRect(RoundRect(
                        rect = Rect(left, top, left + scaledW, top + scaledH),
                        radiusX = cornerPx * scale,
                        radiusY = cornerPx * scale
                    ))
                }
                clipPath(brickPath) {
                    drawImage(
                        image = bitmap,
                        srcOffset = src.offset,
                        srcSize = src.size,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(scaledW.roundToInt().coerceAtLeast(1), scaledH.roundToInt().coerceAtLeast(1))
                    )
                    if (brick.state == BrickState.GAP) {
                        // Darken the slice so missed-day bricks read as distinct from completed ones
                        drawRect(color = gapOverlay, topLeft = Offset(left, top), size = Size(scaledW, scaledH))
                    }
                }
            } else {
                val color = if (brick.state == BrickState.COMPLETED) tierColor(wallModel.tier) else gapColor
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(scaledW, scaledH),
                    cornerRadius = CornerRadius(cornerPx * scale)
                )
            }
        }
    }
}
