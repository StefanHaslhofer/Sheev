package com.sheev.sheev_vision.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class ActionBorderOverlayView(context: Context) : View(context) {
    lateinit var leftBorder: ActionBorder
    lateinit var rightBorder: ActionBorder

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawActionBorders(canvas, leftBorder)
        drawActionBorders(canvas, rightBorder)
    }

    fun setBorders(leftBorder: ActionBorder, rightBorder: ActionBorder) {
        this.leftBorder = leftBorder
        this.rightBorder = rightBorder
        invalidate()
    }

    private fun drawActionBorders(canvas: Canvas, border: ActionBorder) {
        canvas.drawLine(
            border.startX,
            border.startY,
            border.endX,
            border.endY,
            PAINT
        )
    }

    data class ActionBorder(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float
    )

    companion object {
        private val PAINT = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
    }
}