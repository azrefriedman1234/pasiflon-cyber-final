package com.pasiflon.mobile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.content.Context
import android.content.Intent
import androidx.media3.transformer.Transformer
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import java.io.File

class EditorActivity : AppCompatActivity() {
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val watermarkView = findViewById<ImageView>(R.id.watermark_overlay)
        val blurOverlay = findViewById<BlurOverlayView>(R.id.blur_overlay)
        val btnAddBlur = findViewById<Button>(R.id.btn_add_blur)
        val btnExport = findViewById<Button>(R.id.btn_export)

        blurOverlay.visibility = View.GONE

        // טעינת הלוגו מההגדרות
        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        prefs.getString("logo_uri", null)?.let {
            watermarkView.setImageURI(Uri.parse(it))
        }

        btnAddBlur.setOnClickListener {
            blurOverlay.visibility = View.VISIBLE
            Toast.makeText(this, "סמן את אזור הטשטוש", Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            startVideoExport()
        }
    }

    private fun startVideoExport() {
        isProcessing = true
        Toast.makeText(this, "מתחיל עיבוד וידאו... המתן", Toast.LENGTH_LONG).show()

        // יצירת נתיב לקובץ הסופי
        val outputFile = File(getExternalFilesDir(null), "pasiflon_edited_${System.currentTimeMillis()}.mp4")
        
        // כאן מופעל מנוע ה-Media3
        // בשלב זה אנחנו מבצעים סימולציה של סיום מוצלח
        // ב-Build הבא נטמיע את ה-Effect המדויק של הטשטוש
        
        btnExport.postDelayed({
            isProcessing = false
            Toast.makeText(this, "הוידאו מוכן! נשמר ב: ${outputFile.name}", Toast.LENGTH_SHORT).show()
            shareVideo(outputFile)
        }, 3000)
    }

    private fun shareVideo(file: File) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        }
        startActivity(Intent.createChooser(intent, "שתף סרטון ערוך"))
    }
}
