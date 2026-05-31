package com.pck.bricks.features.wall

import com.pck.bricks.core.model.TierType

data class BrickLayout(val columns: Int, val rows: Int, val totalBricks: Int)

class BrickLayoutCalculator {

    fun calculateLayout(tier: TierType): BrickLayout {
        val total = brickCountForTier(tier)
        val cols = 6
        return BrickLayout(cols, total / cols, total)
    }

    fun brickCountForTier(tier: TierType): Int = when (tier) {
        TierType.BRONZE -> 30
        TierType.SILVER -> 60
        TierType.GOLD -> 90
    }
}
