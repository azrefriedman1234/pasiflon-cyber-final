package com.pasiflon.mobile

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BlurOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.parseColor("#8800F2FF") // טורקיז חצי שקוף לסימון
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    
    private val fillPaint = Paint().apply {
        color = Color.argb(150, 0, 0, 0) // שחור חצי שקוף לאזור המטושטש
        style = Paint.Style.FILL
    }

    var blurRect = RectF()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val size = 200f // גודל ריבוע הטשטוש
                blurRect.set(
                    event.x - size, event.y - size,
                    event.x + size, event.y + size
                )
                invalidate() // עדכון המסך
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!blurRect.isEmpty) {
            canvas.drawRect(blurRect, fillPaint)
            canvas.drawRect(blurRect, paint)
        }
    }
}
