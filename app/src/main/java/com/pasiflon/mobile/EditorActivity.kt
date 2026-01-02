package com.pasiflon.mobile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.content.Context
import android.content.Intent
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
            isProcessing = true
            Toast.makeText(this, "מעבד וידאו...", Toast.LENGTH_SHORT).show()
            
            // סימולציית ייצוא
            btnExport.postDelayed({
                isProcessing = false
                Toast.makeText(this, "הוידאו מוכן!", Toast.LENGTH_SHORT).show()
            }, 2000)
        }
    }
}
