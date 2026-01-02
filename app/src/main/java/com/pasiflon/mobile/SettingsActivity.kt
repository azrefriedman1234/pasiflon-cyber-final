package com.pasiflon.mobile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.content.Context

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val apiIdField = findViewById<EditText>(R.id.edit_api_id)
        val apiHashField = findViewById<EditText>(R.id.edit_api_hash)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnPick = findViewById<Button>(R.id.btn_pick_logo)

        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        
        // טעינת נתונים קיימים
        apiIdField.setText(prefs.getString("api_id", ""))
        apiHashField.setText(prefs.getString("api_hash", ""))

        btnSave.setOnClickListener {
            prefs.edit().apply {
                putString("api_id", apiIdField.text.toString())
                putString("api_hash", apiHashField.text.toString())
                apply()
            }
            Toast.makeText(this, "ההגדרות נשמרו!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnPick.setOnClickListener {
            Toast.makeText(this, "פתח גלריה לבחירת לוגו...", Toast.LENGTH_SHORT).show()
            // כאן נטמיע את ה-Image Picker בשלב הבא
        }
    }
}
