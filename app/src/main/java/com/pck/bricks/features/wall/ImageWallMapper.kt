package com.pck.bricks.features.wall

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

data class BrickSrcRect(val offset: IntOffset, val size: IntSize)

private const val BRICK_AR = 2.0f  // width : height of one brick

class ImageWallMapper {

    /**
     * Returns the source region of the image that maps to the given brick.
     *
     * The image is first center-cropped to the wall's natural aspect ratio
     * (cols × brickAr / rows) so that each slice fills its brick without
     * distortion. Bricks fill bottom-up (index 0 = bottom-left).
     */
    fun brickSrcRect(
        brickIndex: Int,
        layout: BrickLayout,
        imageWidth: Int,
        imageHeight: Int
    ): BrickSrcRect {
        // Center-crop image to the wall's aspect ratio so slices are undistorted
        val wallAr = layout.columns.toFloat() * BRICK_AR / layout.rows
        val imageAr = imageWidth.toFloat() / imageHeight

        val cropLeft: Int
        val cropTop: Int
        val cropW: Int
        val cropH: Int

        if (imageAr > wallAr) {
            // Image wider than wall — crop left/right sides
            cropH = imageHeight
            cropW = (imageHeight * wallAr).roundToInt()
            cropLeft = (imageWidth - cropW) / 2
            cropTop = 0
        } else {
            // Image taller than wall — crop top/bottom
            cropW = imageWidth
            cropH = (imageWidth / wallAr).roundToInt()
            cropLeft = 0
            cropTop = (imageHeight - cropH) / 2
        }

        val col = brickIndex % layout.columns
        val canvasRow = layout.rows - 1 - (brickIndex / layout.columns)

        val left   = cropLeft + (col.toFloat()             / layout.columns * cropW).roundToInt()
        val top    = cropTop  + (canvasRow.toFloat()        / layout.rows   * cropH).roundToInt()
        val right  = cropLeft + ((col + 1).toFloat()       / layout.columns * cropW).roundToInt()
        val bottom = cropTop  + ((canvasRow + 1).toFloat() / layout.rows   * cropH).roundToInt()

        return BrickSrcRect(
            offset = IntOffset(left, top),
            size   = IntSize((right - left).coerceAtLeast(1), (bottom - top).coerceAtLeast(1))
        )
    }
}
