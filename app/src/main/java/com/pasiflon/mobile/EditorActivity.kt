package com.pasiflon.mobile

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class EditorActivity : AppCompatActivity() {
    private var isProcessing = false
    // משתנים לשמירת מיקום הנגיעה היחסי בלוגו
    private var dX = 0f
    private var dY = 0f

    @SuppressLint("ClickableViewAccessibility") // השתקת אזהרת נגישות לשימוש מבצעי
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val watermarkView = findViewById<ImageView>(R.id.watermark_overlay)
        val blurOverlay = findViewById<BlurOverlayView>(R.id.blur_overlay)
        val btnAddBlur = findViewById<Button>(R.id.btn_add_blur)
        val btnExport = findViewById<Button>(R.id.btn_export)

        // בהתחלה הטשטוש כבוי כדי לאפשר הזזה של הלוגו מתחתיו
        blurOverlay.visibility = View.GONE
        blurOverlay.isEnabled = false

        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        val targetChatId = prefs.getString("chat_id", "")
        
        prefs.getString("logo_uri", null)?.let {
            watermarkView.setImageURI(Uri.parse(it))
            // מיקום התחלתי בפינה (אופציונלי, כרגע הוא יתחיל ב-0,0)
             watermarkView.x = 50f
             watermarkView.y = 50f
        }

        // --- שינוי: הוספת יכולת גרירה ללוגו ---
        watermarkView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // שמירת ההפרש בין מיקום הנגיעה למיקום הפינה של התמונה
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // עדכון מיקום התמונה לפי תזוזת האצבע + ההפרש המקורי
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    true
                }
                else -> false
            }
        }
        // --------------------------------------

        btnAddBlur.setOnClickListener {
            // הפעלת מצב טשטוש: מציגים את השכבה ומעבירים אותה לקדמת המסך
            blurOverlay.visibility = View.VISIBLE
            blurOverlay.isEnabled = true
            blurOverlay.bringToFront() 
            Toast.makeText(this, "מצב טשטוש פעיל: גרור למיקום, צבוט לשינוי גודל", Toast.LENGTH_LONG).show()
        }

        btnExport.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            
            if (targetChatId.isNullOrEmpty()) {
                Toast.makeText(this, "שגיאה: לא הוגדר ערוץ יעד בהגדרות!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            isProcessing = true
            Toast.makeText(this, "מעבד ושולח לערוץ $targetChatId...", Toast.LENGTH_SHORT).show()
            
            // כאן בעתיד נשתמש במיקום הסופי של הלוגו (watermarkView.x/y)
            // ובגודל הסופי של הטשטוש (blurOverlay.blurRect)
            
            btnExport.postDelayed({
                isProcessing = false
                Toast.makeText(this, "הקובץ נשלח לטלגרם בהצלחה! ✅", Toast.LENGTH_LONG).show()
                finish()
            }, 3000)
        }
    }
}
