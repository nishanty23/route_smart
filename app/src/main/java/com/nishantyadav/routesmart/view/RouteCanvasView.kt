package com.nishantyadav.routesmart.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class RouteCanvasView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = Color.BLUE
        paint.strokeWidth = 10f

        // Draw simple route line
        canvas.drawLine(100f, 200f, width - 100f, height - 200f, paint)
    }
}