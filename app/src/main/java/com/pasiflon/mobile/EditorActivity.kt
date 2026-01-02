package com.pasiflon.mobile

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import com.google.common.collect.ImmutableList
import java.io.File

class EditorActivity : AppCompatActivity() {
    private var isProcessing = false
    private var videoUriString: String? = null
    
    // משתנים לגרירה
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
        val container = findViewById<View>(R.id.media_container)

        videoUriString = intent.getStringExtra("video_uri")
        
        blurOverlay.visibility = View.GONE
        blurOverlay.isEnabled = false

        // טעינת הגדרות
        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        val targetUsername = prefs.getString("chat_id", "")
        val logoUriStr = prefs.getString("logo_uri", null)

        logoUriStr?.let {
            watermarkView.setImageURI(Uri.parse(it))
            // מיקום התחלתי
            watermarkView.x = 100f
            watermarkView.y = 100f
        }

        // --- 1. החזרת הגרירה של הלוגו ---
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

        // --- 2. החזרת הטשטוש (צביטה) ---
        btnAddBlur.setOnClickListener {
            blurOverlay.visibility = View.VISIBLE
            blurOverlay.isEnabled = true
            blurOverlay.bringToFront()
            Toast.makeText(this, "הזז את הריבוע וצבוט לשינוי גודל", Toast.LENGTH_SHORT).show()
        }

        // --- 3. ייצוא חכם עם Media3 ---
        btnExport.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            
            if (targetUsername.isNullOrEmpty()) {
                Toast.makeText(this, "שגיאה: הגדר ערוץ יעד", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // נתוני המסך לחישוב יחסי
            val parentWidth = container.width.toFloat()
            val parentHeight = container.height.toFloat()
            
            if (parentWidth == 0f || parentHeight == 0f) {
                Toast.makeText(this, "שגיאה בחישוב מימדי מסך", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isProcessing = true
            Toast.makeText(this, "מייצר שכבות צנזורה ולוגו...", Toast.LENGTH_SHORT).show()

            // נתונים לוגו
            val logoX = watermarkView.x / parentWidth
            val logoY = watermarkView.y / parentHeight
            // חישוב גודל יחסי של הלוגו (בערך)
            val logoScale = watermarkView.width.toFloat() / parentWidth

            // נתוני טשטוש (אם פעיל)
            var blurBitmap: Bitmap? = null
            var blurX = 0f
            var blurY = 0f
            var blurScaleX = 0f
            var blurScaleY = 0f
            
            if (blurOverlay.visibility == View.VISIBLE) {
                // יצירת ביטמפ שחור חצי שקוף (צנזורה)
                blurBitmap = createCensoredBitmap()
                val rect = blurOverlay.blurRect
                
                // המרת מיקום למספרים בין 0 ל-1 (נורמליזציה)
                // ב-OverlaySettings, העוגן הוא המרכז, אז צריך לחשב את מרכז הריבוע
                val centerX = rect.centerX()
                val centerY = rect.centerY()
                
                blurX = centerX / parentWidth
                blurY = centerY / parentHeight
                blurScaleX = rect.width() / parentWidth
                blurScaleY = rect.height() / parentHeight
            }

            startMedia3Export(
                Uri.parse(videoUriString), 
                if (logoUriStr != null) Uri.parse(logoUriStr) else null,
                targetUsername,
                logoX, logoY, logoScale,
                blurBitmap, blurX, blurY, blurScaleX, blurScaleY
            )
        }
    }

    // יצירת ריבוע "צנזורה" (שחור 50%)
    private fun createCensoredBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.color = Color.BLACK
        paint.alpha = 128 // 50% שקיפות
        canvas.drawRect(0f, 0f, 100f, 100f, paint)
        return bmp
    }

    private fun startMedia3Export(
        videoUri: Uri, 
        logoUri: Uri?, 
        target: String,
        logoX: Float, logoY: Float, logoScale: Float,
        blurBitmap: Bitmap?, blurX: Float, blurY: Float, blurW: Float, blurH: Float
    ) {
        val outputFile = File(getExternalFilesDir(null), "pasiflon_export_${System.currentTimeMillis()}.mp4")
        val overlays =  ImmutableList.builder<BitmapOverlay>()

        // 1. הוספת שכבת צנזורה (Blur/Redaction)
        if (blurBitmap != null) {
            val blurSettings = OverlaySettings.Builder()
                .setAlpha(0.5f) // שקיפות נוספת
                // ב-Media3 קואורדינטות נעות מ -1 ל 1. צריך המרה: (val * 2) - 1
                .setBackgroundFrameAnchor(
                    (blurX * 2) - 1f, // X
                    (blurY * 2) - 1f  // Y
                )
                .setScale(blurW * 2, blurH * 2) // סקייל יחסי
                .build()
                
            val blurOverlay = BitmapOverlay.createStaticBitmapOverlay(blurBitmap, blurSettings)
            overlays.add(blurOverlay)
        }

        // 2. הוספת שכבת לוגו
        if (logoUri != null) {
            val logoBmp = getBitmapFromUri(logoUri)
            if (logoBmp != null) {
                // התאמת מיקום הלוגו (המרת קואורדינטות מסך 0..1 לקואורדינטות OpenGL -1..1)
                // הוספת אופסט קטן כי העוגן הוא המרכז
                val normalizedX = (logoX * 2) - 1f + 0.1f // תיקון קטן ליישור
                val normalizedY = (logoY * 2) - 1f + 0.1f 

                val logoSettings = OverlaySettings.Builder()
                    .setAlpha(0.9f)
                    .setBackgroundFrameAnchor(normalizedX, normalizedY)
                    .setScale(0.2f, 0.2f) // גודל קבוע ללוגו בינתיים, או לפי logoScale
                    .build()

                val logoOverlay = BitmapOverlay.createStaticBitmapOverlay(logoBmp, logoSettings)
                overlays.add(logoOverlay)
            }
        }

        val overlayEffect = OverlayEffect(overlays.build())

        // 3. בניית המדיה
        val mediaItem = MediaItem.fromUri(videoUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(ImmutableList.of(overlayEffect))
            .build()

        // 4. ייצוא
        val transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    runOnUiThread {
                        isProcessing = false
                        Toast.makeText(this@EditorActivity, "הוידאו מוכן! נשלח ל-$target", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                    runOnUiThread {
                        isProcessing = false
                        Toast.makeText(this@EditorActivity, "שגיאה: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
            .build()

        transformer.start(editedMediaItem, outputFile.absolutePath)
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) { e.printStackTrace(); null }
    }
}
