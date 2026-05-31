package com.pck.bricks.features.wall

data class ImageFragment(val imagePath: String, val brickIndex: Int)

class ImageWallMapper {
    // Phase 1 placeholder — image-to-brick mapping is a Phase 2 feature
    fun mapImageToBrick(imagePath: String, brickIndex: Int, layout: BrickLayout): ImageFragment? = null
}
