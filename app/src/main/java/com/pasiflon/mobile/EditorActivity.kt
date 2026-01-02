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
import androidx.media3.common.Effect 
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.TextureOverlay
import androidx.media3.effect.OverlaySettings
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.common.Effects
import com.google.common.collect.ImmutableList
import java.io.File

@UnstableApi // סימון נדרש לשימוש ב-Transformer
class EditorActivity : AppCompatActivity() {
    private var isProcessing = false
    private var videoUriString: String? = null
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
                    view.animate().x(event.rawX + dX).y(event.rawY + dY).setDuration(0).start()
                    true
                }
                else -> false
            }
        }

        btnAddBlur.setOnClickListener {
            blurOverlay.visibility = View.VISIBLE
            blurOverlay.isEnabled = true
            blurOverlay.bringToFront()
            Toast.makeText(this, "הזז את הריבוע וצבוט לשינוי גודל", Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            if (targetUsername.isNullOrEmpty()) {
                Toast.makeText(this, "שגיאה: הגדר ערוץ יעד", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val parentWidth = container.width.toFloat()
            val parentHeight = container.height.toFloat()
            if (parentWidth == 0f) return@setOnClickListener

            isProcessing = true
            Toast.makeText(this, "מייצר שכבות צנזורה ולוגו...", Toast.LENGTH_SHORT).show()

            val logoX = watermarkView.x / parentWidth
            val logoY = watermarkView.y / parentHeight
            
            var blurBitmap: Bitmap? = null
            var blurX = 0f
            var blurY = 0f
            var blurW = 0f
            var blurH = 0f
            
            if (blurOverlay.visibility == View.VISIBLE) {
                blurBitmap = createCensoredBitmap()
                val rect = blurOverlay.blurRect
                blurX = rect.centerX() / parentWidth
                blurY = rect.centerY() / parentHeight
                blurW = rect.width() / parentWidth
                blurH = rect.height() / parentHeight
            }

            startMedia3Export(
                Uri.parse(videoUriString ?: ""), 
                if (logoUriStr != null) Uri.parse(logoUriStr) else null,
                targetUsername,
                logoX, logoY,
                blurBitmap, blurX, blurY, blurW, blurH
            )
        }
    }

    private fun createCensoredBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.color = Color.BLACK
        paint.alpha = 128
        canvas.drawRect(0f, 0f, 100f, 100f, paint)
        return bmp
    }

    private fun startMedia3Export(
        videoUri: Uri, logoUri: Uri?, target: String,
        logoX: Float, logoY: Float,
        blurBitmap: Bitmap?, blurX: Float, blurY: Float, blurW: Float, blurH: Float
    ) {
        val outputFile = File(getExternalFilesDir(null), "pasiflon_export_${System.currentTimeMillis()}.mp4")
        
        // שימוש ב-TextureOverlay במקום BitmapOverlay הגנרי כדי לרצות את הקומפיילר
        val overlays = ImmutableList.builder<TextureOverlay>()

        if (blurBitmap != null) {
            val blurSettings = OverlaySettings.Builder()
                .setAlpha(0.5f)
                .setBackgroundFrameAnchor((blurX * 2) - 1f, (blurY * 2) - 1f)
                .setScale(blurW * 2, blurH * 2)
                .build()
            overlays.add(BitmapOverlay.createStaticBitmapOverlay(blurBitmap, blurSettings))
        }

        if (logoUri != null) {
            val logoBmp = getBitmapFromUri(logoUri)
            if (logoBmp != null) {
                val normalizedX = (logoX * 2) - 1f + 0.1f
                val normalizedY = (logoY * 2) - 1f + 0.1f 
                val logoSettings = OverlaySettings.Builder()
                    .setAlpha(0.9f)
                    .setBackgroundFrameAnchor(normalizedX, normalizedY)
                    .setScale(0.2f, 0.2f)
                    .build()
                overlays.add(BitmapOverlay.createStaticBitmapOverlay(logoBmp, logoSettings))
            }
        }

        // עטיפת ה-OverlayEffect ברשימה מפורשת
        val overlayEffect = OverlayEffect(overlays.build())
        
        // יצירת רשימת אפקטים לוידאו בלבד
        val videoEffects = ImmutableList.of<Effect>(overlayEffect)
        
        // שימוש באובייקט Effects החדש של Media3 1.2.0
        val effects = Effects(ImmutableList.of(), videoEffects)

        val mediaItem = MediaItem.fromUri(videoUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(effects) // העברת אובייקט Effects התקין
            .build()

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
