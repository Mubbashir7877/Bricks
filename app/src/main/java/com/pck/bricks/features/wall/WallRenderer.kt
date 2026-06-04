package com.pck.bricks.features.wall

import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.TierType

enum class BrickState { COMPLETED, GAP, EMPTY }

data class BrickRenderData(val index: Int, val state: BrickState)

data class WallRenderModel(
    val bricks: List<BrickRenderData>,
    val layout: BrickLayout,
    val tier: TierType,
    val newlyAddedIndex: Int? = null,
    val imagePath: String? = null
)

class WallRenderer {

    fun renderWall(
        progress: HabitProgress,
        layout: BrickLayout,
        newlyAddedIndex: Int? = null,
        imagePath: String? = null
    ): WallRenderModel {
        val bricks = (0 until layout.totalBricks).map { index ->
            val state = when {
                index < progress.completedBrickCount -> BrickState.COMPLETED
                index < progress.completedBrickCount + progress.missedGapCount -> BrickState.GAP
                else -> BrickState.EMPTY
            }
            BrickRenderData(index, state)
        }
        return WallRenderModel(bricks, layout, progress.currentTier, newlyAddedIndex, imagePath)
    }
}
