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
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

class EditorActivity : AppCompatActivity() {
    private var isProcessing = false
    private var dX = 0f
    private var dY = 0f
    private var videoUriString: String? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val watermarkView = findViewById<ImageView>(R.id.watermark_overlay)
        val blurOverlay = findViewById<BlurOverlayView>(R.id.blur_overlay)
        val btnAddBlur = findViewById<Button>(R.id.btn_add_blur)
        val btnExport = findViewById<Button>(R.id.btn_export)

        // אם הגענו לכאן משיתוף חיצוני או מהטבלה, נקבל את ה-URI
        videoUriString = intent.getStringExtra("video_uri")

        blurOverlay.visibility = View.GONE
        blurOverlay.isEnabled = false

        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        val targetUsername = prefs.getString("chat_id", "")
        val logoUriStr = prefs.getString("logo_uri", null)
        
        logoUriStr?.let {
            watermarkView.setImageURI(Uri.parse(it))
            watermarkView.x = 100f
            watermarkView.y = 100f
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
            Toast.makeText(this, "סמן אזור לטשטוש", Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            
            if (targetUsername.isNullOrEmpty()) {
                Toast.makeText(this, "שגיאה: הגדר ערוץ יעד בהגדרות", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (logoUriStr == null) {
                Toast.makeText(this, "שגיאה: חובה לבחור לוגו", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            isProcessing = true
            val finalTarget = if (targetUsername.startsWith("@")) targetUsername else "@$targetUsername"
            Toast.makeText(this, "מכין קבצים לעיבוד...", Toast.LENGTH_SHORT).show()

            // ביצוע הפעולה ברקע כדי לא לתקוע את הממשק
            Thread {
                // 1. המרת ה-URI של הלוגו לקובץ אמיתי
                val logoFile = FileUtils.getFileFromUri(this, Uri.parse(logoUriStr), "logo.png")
                
                // 2. טיפול בוידאו (או שימוש בלוגו כדמה לבדיקה אם אין וידאו)
                val videoFile = if (videoUriString != null) {
                    FileUtils.getFileFromUri(this, Uri.parse(videoUriString), "input_video.mp4")
                } else {
                    // Fallback לבדיקה: משתמשים בלוגו גם כקלט אם אין וידאו
                    logoFile 
                }

                if (logoFile == null || videoFile == null) {
                    runOnUiThread { 
                        Toast.makeText(this, "שגיאה בטעינת הקבצים", Toast.LENGTH_LONG).show()
                        isProcessing = false
                    }
                    return@Thread
                }

                // 3. חישוב קואורדינטות יחסיות (חשוב!)
                // FFmpeg צריך לדעת איפה למקם ביחס לרזולוציה המקורית, לא ביחס למסך הטלפון.
                // לצורך ה-MVP נשתמש בערכים המוחלטים מהמסך, אבל בעתיד נצטרך להכפיל ביחס ההמרה (Scale Factor).
                val wx = watermarkView.x.toInt()
                val wy = watermarkView.y.toInt()
                
                val bx = blurOverlay.blurRect.left.toInt()
                val by = blurOverlay.blurRect.top.toInt()
                val bw = blurOverlay.blurRect.width().toInt()
                val bh = blurOverlay.blurRect.height().toInt()

                val outputFile = File(getExternalFilesDir(null), "pasiflon_export_${System.currentTimeMillis()}.mp4")
                
                // 4. פקודת FFmpeg
                val blurCmd = if (blurOverlay.visibility == View.VISIBLE) "delogo=x=$bx:y=$by:w=$bw:h=$bh[blurred];[blurred]" else "[0:v]"
                val cmd = "-i \"${videoFile.absolutePath}\" -i \"${logoFile.absolutePath}\" -filter_complex \"${blurCmd}[1:v]overlay=x=$wx:y=$wy\" -c:v libx264 -preset ultrafast \"${outputFile.absolutePath}\""

                FFmpegKit.executeAsync(cmd) { session ->
                    runOnUiThread {
                        isProcessing = false
                        if (ReturnCode.isSuccess(session.returnCode)) {
                            Toast.makeText(this, "הוידאו נשלח ל-$finalTarget!", Toast.LENGTH_LONG).show()
                            // כאן תבוא פקודת השליחה ל-TDLib עם outputFile.absolutePath
                            finish()
                        } else {
                            // במקרה של כישלון נציג את הלוג
                            Toast.makeText(this, "שגיאה בעיבוד FFmpeg", Toast.LENGTH_LONG).show()
                            println("FFmpeg Error: ${session.failStackTrace}")
                        }
                    }
                }
            }.start()
        }
    }
}
