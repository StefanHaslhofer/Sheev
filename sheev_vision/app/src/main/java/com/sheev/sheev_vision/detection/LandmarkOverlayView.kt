package com.sheev.sheev_vision.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class LandmarkOverlayView(context: Context) : View(context) {

    private val poseLandmarks = mutableListOf<PoseLandmark>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        poseLandmarks.forEach { box -> drawPoseLandmark(canvas, box) }
    }

    private fun drawPoseLandmark(
        canvas: Canvas,
        pl: PoseLandmark
    ) {
        val paint = Paint().apply {
            color = pl.color
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
            textSize = 48f
        }

        canvas.drawCircle(pl.x.toFloat(), pl.y.toFloat(), 6f, paint)

        paint.strokeWidth = 4f

        canvas.drawText(
            "${pl.id}: ${pl.label}",
            pl.x.toFloat(),
            pl.y.toFloat() - MARGIN,
            paint
        )
    }

    fun setPoseLandemarks(boxes: List<PoseLandmark>) {
        poseLandmarks.clear()
        poseLandmarks.addAll(boxes)
        invalidate()
    }

    data class PoseLandmark(
        val x: Float,
        val y: Float,
        val color: Int,
        val label: String?,
        val confidence: Float?,
        val id: Int?
    )

    companion object {
        private const val MARGIN = 8f
    }
}