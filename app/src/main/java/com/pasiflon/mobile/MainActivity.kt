package com.pasiflon.mobile

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.content.res.ColorStateList
import android.graphics.Color

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var inputField: EditText
    private lateinit var actionButton: Button
    private var currentStep = "PHONE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#050505"))
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        statusText = TextView(this).apply {
            text = "הזן מספר טלפון (פורמט בינלאומי):"
            setTextColor(Color.parseColor("#00F2FF"))
            textSize = 18f
            setPadding(0, 0, 0, 40)
            gravity = Gravity.CENTER
        }

        inputField = EditText(this).apply {
            hint = "+972..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
        }

        actionButton = Button(this).apply {
            text = "שלח קוד"
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF00E5"))
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

        if (currentStep == "PHONE") {
            statusText.text = "הזן את הקוד שקיבלת:"
            inputField.text.clear()
            inputField.hint = "12345"
            actionButton.text = "התחבר"
            currentStep = "CODE"
        } else {
            statusText.text = "מתחבר למערכת..."
            actionButton.visibility = View.GONE
            inputField.visibility = View.GONE
            Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_LONG).show()
        }
    }
}
