package com.sheev.sheev_vision.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class BoundingBoxView(context: Context) : View(context) {

    private val boundingBoxes = mutableListOf<BoundingBox>()

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        boundingBoxes.forEach { box -> drawBoundingBox(canvas, box, paint) }
    }

    fun drawBoundingBox(
        canvas: Canvas,
        box: BoundingBox,
        paint: Paint
    ) {
        canvas.drawRect(box.rect, paint)
    }

    fun setBoundingBoxes(boxes: List<BoundingBox>) {
        boundingBoxes.clear()
        boundingBoxes.addAll(boxes)
        invalidate()
    }

    data class BoundingBox(
        val rect: Rect,
        val label: String = ""
    )
}