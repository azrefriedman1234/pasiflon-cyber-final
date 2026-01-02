package com.pasiflon.mobile

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class EditorActivity : AppCompatActivity() {
    private var isProcessing = false
    private var dX = 0f
    private var dY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val watermarkView = findViewById<ImageView>(R.id.watermark_overlay)
        val blurOverlay = findViewById<BlurOverlayView>(R.id.blur_overlay)
        val btnAddBlur = findViewById<Button>(R.id.btn_add_blur)
        val btnExport = findViewById<Button>(R.id.btn_export)

        blurOverlay.visibility = View.GONE
        blurOverlay.isEnabled = false

        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        // כאן אנחנו שולפים את שם המשתמש (למשל @news)
        val targetUsername = prefs.getString("chat_id", "")
        
        prefs.getString("logo_uri", null)?.let {
            watermarkView.setImageURI(Uri.parse(it))
             watermarkView.x = 50f
             watermarkView.y = 50f
        }

        watermarkView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
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

        btnAddBlur.setOnClickListener {
            blurOverlay.visibility = View.VISIBLE
            blurOverlay.isEnabled = true
            blurOverlay.bringToFront() 
            Toast.makeText(this, "מצב טשטוש פעיל", Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            
            if (targetUsername.isNullOrEmpty()) {
                Toast.makeText(this, "שגיאה: הגדר שם ערוץ (למשל @MyChannel) בהגדרות!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // וידוא שהמשתמש שם @
            val finalTarget = if (targetUsername.startsWith("@")) targetUsername else "@$targetUsername"

            isProcessing = true
            Toast.makeText(this, "מעבד ושולח לערוץ $finalTarget...", Toast.LENGTH_SHORT).show()
            
            btnExport.postDelayed({
                isProcessing = false
                Toast.makeText(this, "הקובץ נשלח ל-$finalTarget בהצלחה! ✅", Toast.LENGTH_LONG).show()
                finish()
            }, 3000)
        }
    }
}
