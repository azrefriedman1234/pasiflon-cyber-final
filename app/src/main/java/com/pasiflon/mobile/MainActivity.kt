package com.pasiflon.mobile

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var inputField: EditText
    private lateinit var actionButton: Button
    private var currentStep = "PHONE" // PHONE -> CODE -> PASSWORD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // עיצוב סייבר מהיר בקוד
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF050505.toInt())
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        statusText = TextView(this).apply {
            text = "הזן מספר טלפון (פורמט בינלאומי):"
            setTextColor(0xFF00F2FF.toInt())
            textSize = 18f
            setPadding(0, 0, 0, 40)
        }

        inputField = EditText(this).apply {
            hint = "+972..."
            setHintTextColor(0x44FFFFFF.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundResource(android.R.drawable.edit_box_background_dark)
        }

        actionButton = Button(this).apply {
            text = "שלח קוד"
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF00E5.toInt())
            setOnClickListener { handleLogin() }
        }

        layout.addView(statusText)
        layout.addView(inputField)
        layout.addView(actionButton)
        setContentView(layout)
    }

    private fun handleLogin() {
        val input = inputField.text.toString()
        if (input.isEmpty()) return

        when (currentStep) {
            "PHONE" -> {
                // כאן תהיה הקריאה ל-TdJson.sendPhoneNumber(input)
                statusText.text = "הזן את הקוד שקיבלת מטלגרם:"
                inputField.text.clear()
                inputField.hint = "12345"
                actionButton.text = "התחבר"
                currentStep = "CODE"
                Toast.makeText(this, "שולח מספר...", Toast.LENGTH_SHORT).show()
            }
            "CODE" -> {
                // כאן תהיה הקריאה ל-TdJson.checkCode(input)
                statusText.text = "מתחבר למערכת..."
                actionButton.visibility = View.GONE
                inputField.visibility = View.GONE
                Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_LONG).show()
                // מעבר לטבלה הראשי אחרי שנייה
            }
        }
    }
}
