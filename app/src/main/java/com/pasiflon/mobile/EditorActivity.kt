package com.pasiflon.mobile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.content.Context

class EditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val watermarkView = findViewById<ImageView>(R.id.watermark_overlay)
        val previewImage = findViewById<ImageView>(R.id.editor_image_preview)
        val btnExport = findViewById<Button>(R.id.btn_export)

        // טעינת הלוגו ששמרת בהגדרות
        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        val logoUriString = prefs.getString("logo_uri", null)
        
        if (logoUriString != null) {
            watermarkView.setImageURI(Uri.parse(logoUriString))
        }

        btnExport.setOnClickListener {
            Toast.makeText(this, "מעבד וידאו עם סימן מים...", Toast.LENGTH_LONG).show()
            // כאן יופעל ה-Media3 Transformer
        }
    }
}
