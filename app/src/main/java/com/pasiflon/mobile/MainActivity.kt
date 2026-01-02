package com.pasiflon.mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color

data class TelegramMsg(val id: Long, val sender: String, var text: String, val hasMedia: Boolean = false)

class MainActivity : AppCompatActivity() {
    private val messages = mutableListOf<TelegramMsg>()
    private lateinit var adapter: MessageAdapter

    // מנהל ההרשאות - יבקש גישה לקבצים מיד בהפעלה
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Toast.makeText(this, "הרשאות מדיה אושרו - המערכת מוכנה", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "חובה לאשר הרשאות כדי לערוך וידאו", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. בדיקה ובקשת הרשאות אוטומטית
        checkAndRequestPermissions()

        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val recycler = findViewById<RecyclerView>(R.id.messages_recycler)
        adapter = MessageAdapter(messages)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // 2. בדיקה אם המשתמש מחובר (אם לא - נבקש טלפון)
        checkLoginStatus()

        // נתונים ראשוניים
        messages.add(TelegramMsg(1, "מערכת", "ברוך הבא. אנא וודא שהגדרות ה-API מוזנות.", false))
        adapter.notifyDataSetChanged()
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun checkLoginStatus() {
        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        val apiId = prefs.getString("api_id", "")
        
        if (apiId.isNullOrEmpty()) {
            Toast.makeText(this, "חסרים פרטי API - נא להגדיר בהגדרות", Toast.LENGTH_LONG).show()
            return
        }

        // כאן בעתיד תהיה בדיקה מול TDLib אם ה-Auth State הוא Ready
        // לצורך ההדגמה: אם אין "is_logged_in" בזיכרון, נקפיץ דיאלוג התחברות
        if (!prefs.getBoolean("is_logged_in", false)) {
            showPhoneLoginDialog()
        }
    }

    private fun showPhoneLoginDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("התחברות לטלגרם")
        
        val input = EditText(this)
        input.hint = "מספר טלפון (כולל קידומת +972)"
        input.inputType = android.text.InputType.TYPE_CLASS_PHONE
        builder.setView(input)

        builder.setPositiveButton("שלח קוד") { _, _ ->
            val phone = input.text.toString()
            if (phone.isNotEmpty()) {
                // כאן נשלח את המספר ל-TDLib
                showCodeLoginDialog(phone)
            }
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showCodeLoginDialog(phone: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("הזן קוד אימות")
        builder.setMessage("נשלח קוד ל-$phone")
        
        val input = EditText(this)
        input.hint = "קוד בן 5 ספרות"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        builder.setPositiveButton("התחבר") { _, _ ->
            val code = input.text.toString()
            if (code.isNotEmpty()) {
                // כאן נשלח את הקוד ל-TDLib
                getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_logged_in", true).apply()
                Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setCancelable(false)
        builder.show()
    }

    inner class MessageAdapter(private val list: List<TelegramMsg>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val sender: TextView = v.findViewById(android.R.id.text1)
            val content: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val msg = list[position]
            holder.sender.text = msg.sender
            holder.sender.setTextColor(Color.parseColor("#00F2FF"))
            holder.content.text = msg.text
            holder.content.setTextColor(Color.WHITE)
            
            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, DetailsActivity::class.java)
                intent.putExtra("msg_text", msg.text)
                intent.putExtra("has_media", msg.hasMedia)
                startActivity(intent)
            }
        }
        override fun getItemCount() = list.size
    }
}
