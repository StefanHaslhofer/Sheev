package com.sheev.sheev_vision.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class BoundingBoxView(context: Context) : View(context) {

    private val boundingBoxes = mutableListOf<BoundingBox>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        boundingBoxes.forEach { box -> drawBoundingBox(canvas, box) }
    }

    private fun drawBoundingBox(
        canvas: Canvas,
        box: BoundingBox
    ) {
        val paint = Paint().apply {
            color = box.color
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
            textSize = 48f
        }

        canvas.drawRect(box.rect, paint)

        paint.strokeWidth = 4f

        canvas.drawText(
            "${box.id}: TEST",
            box.rect.left.toFloat(),
            box.rect.top.toFloat() - MARGIN,
            paint
        )
    }

    fun setBoundingBoxes(boxes: List<BoundingBox>) {
        boundingBoxes.clear()
        boundingBoxes.addAll(boxes)
        invalidate()
    }

    data class BoundingBox(
        val rect: Rect,
        val color: Int,
        val label: String?,
        val confidence: Float?,
        val id: Int?
    )

    companion object {
        private const val MARGIN = 8f
    }
}