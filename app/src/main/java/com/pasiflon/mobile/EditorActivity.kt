package com.pasiflon.mobile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.content.Context

class EditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val watermarkView = findViewById<ImageView>(R.id.watermark_overlay)
        val blurOverlay = findViewById<View>(R.id.blur_overlay)
        val btnAddBlur = findViewById<Button>(R.id.btn_add_blur)
        val btnExport = findViewById<Button>(R.id.btn_export)

        // התחלה במצב מוסתר
        blurOverlay.visibility = View.GONE

        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        prefs.getString("logo_uri", null)?.let {
            watermarkView.setImageURI(Uri.parse(it))
        }

        btnAddBlur.setOnClickListener {
            blurOverlay.visibility = View.VISIBLE
            Toast.makeText(this, "גרור את האצבע למיקום הטשטוש", Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener {
            Toast.makeText(this, "מייצר וידאו סופי עם לוגו וטשטוש...", Toast.LENGTH_LONG).show()
            // כאן נחבר את ה-Media3 Transformer בשלב הבא
        }
    }
}
