package com.pasiflon.mobile

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class BlurOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.parseColor("#8800F2FF") // מסגרת טורקיז
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    
    // שינוי: שקיפות 50% (Alpha 128 מתוך 255)
    private val fillPaint = Paint().apply {
        color = Color.argb(128, 0, 0, 0)
        style = Paint.Style.FILL
    }

    var blurRect = RectF()
    // שינוי: משתנה לגודל הריבוע ההתחלתי (חצי רוחב/גובה)
    private var currentHalfSize = 100f 
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // שינוי: גלאי מחוות צביטה
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // הכפלת הגודל הנוכחי בפקטור הצביטה
            currentHalfSize *= detector.scaleFactor
            // הגבלת הגודל (מינימום 30, מקסימום 500) למניעת עיוותים
            currentHalfSize = currentHalfSize.coerceIn(30f, 500f)
            
            // עדכון מיידי של הריבוע סביב נקודת המרכז האחרונה
            updateRectPosition(lastTouchX, lastTouchY)
            invalidate()
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // העברת האירוע לגלאי הצביטה קודם כל
        scaleDetector.onTouchEvent(event)

        // אם אנחנו לא באמצע פעולת צביטה, נאפשר גרירה
        if (!scaleDetector.isInProgress) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    updateRectPosition(event.x, event.y)
                    invalidate()
                }
            }
        }
        return true
    }

    private fun updateRectPosition(centerX: Float, centerY: Float) {
        blurRect.set(
            centerX - currentHalfSize, centerY - currentHalfSize,
            centerX + currentHalfSize, centerY + currentHalfSize
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!blurRect.isEmpty) {
            canvas.drawRect(blurRect, fillPaint)
            canvas.drawRect(blurRect, paint)
        }
    }
}
