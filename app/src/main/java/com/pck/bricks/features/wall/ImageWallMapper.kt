package com.pck.bricks.features.wall

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

data class BrickSrcRect(val offset: IntOffset, val size: IntSize)

class ImageWallMapper {

    /**
     * Returns the source region of the image that maps to the given brick.
     *
     * The wall is treated as a single canvas covering the whole image. Bricks fill
     * bottom-up (index 0 = bottom-left), so the canvas row for a brick is:
     *   canvasRow = rows - 1 - (index / cols)   (0 = top of canvas)
     * This means the bottom of the image aligns with the first bricks placed, and
     * the image is progressively revealed upward as the wall grows.
     */
    fun brickSrcRect(
        brickIndex: Int,
        layout: BrickLayout,
        imageWidth: Int,
        imageHeight: Int
    ): BrickSrcRect {
        val col = brickIndex % layout.columns
        val canvasRow = layout.rows - 1 - (brickIndex / layout.columns)

        val left   = (col.toFloat()         / layout.columns * imageWidth).roundToInt()
        val top    = (canvasRow.toFloat()    / layout.rows   * imageHeight).roundToInt()
        val right  = ((col + 1).toFloat()   / layout.columns * imageWidth).roundToInt()
        val bottom = ((canvasRow + 1).toFloat() / layout.rows * imageHeight).roundToInt()

        return BrickSrcRect(
            offset = IntOffset(left, top),
            size   = IntSize((right - left).coerceAtLeast(1), (bottom - top).coerceAtLeast(1))
        )
    }
}
