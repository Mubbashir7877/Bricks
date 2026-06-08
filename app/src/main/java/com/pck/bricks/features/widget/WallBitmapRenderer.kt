package com.pck.bricks.features.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.pck.bricks.core.model.TierType
import com.pck.bricks.features.wall.BrickState
import com.pck.bricks.features.wall.WallRenderModel

class WallBitmapRenderer {

    fun render(model: WallRenderModel, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cols = model.layout.columns
        val rows = model.layout.rows
        val brickW = widthPx.toFloat() / cols
        val brickH = heightPx.toFloat() / rows
        val mortar = 2f
        val corner = 3f

        val completedColor = when (model.tier) {
            TierType.BRONZE -> Color.parseColor("#B85C3C")
            TierType.SILVER -> Color.parseColor("#787878")
            TierType.GOLD   -> Color.parseColor("#C9A227")
        }

        for (brick in model.bricks) {
            val col = brick.index % cols
            val row = rows - 1 - (brick.index / cols) // bottom-up, index 0 = bottom-left
            val l = col * brickW + mortar / 2f
            val t = row * brickH + mortar / 2f
            val rect = RectF(l, t, l + brickW - mortar, t + brickH - mortar)
            paint.color = when (brick.state) {
                BrickState.COMPLETED -> completedColor
                BrickState.GAP       -> Color.argb(255, 42, 42, 42)
                BrickState.EMPTY     -> Color.argb(45, 255, 255, 255)
            }
            canvas.drawRoundRect(rect, corner, corner, paint)
        }
        return bmp
    }
}
