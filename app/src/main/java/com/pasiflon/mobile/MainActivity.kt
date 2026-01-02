package com.pasiflon.mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color

// הוספנו שדה 'time' לשימוש עתידי
data class TelegramMsg(val id: Long, val sender: String, var text: String, val hasMedia: Boolean = false, val time: Long = System.currentTimeMillis())

class MainActivity : AppCompatActivity(), TelegramManager.AuthListener {
    private val messages = mutableListOf<TelegramMsg>()
    private lateinit var adapter: MessageAdapter
    private var loginDialog: AlertDialog? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val recycler = findViewById<RecyclerView>(R.id.messages_recycler)
        adapter = MessageAdapter(messages)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        TelegramManager.listener = this
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
        val apiId = prefs.getString("api_id", "") ?: ""
        val apiHash = prefs.getString("api_hash", "") ?: ""
        
        if (apiId.isNotEmpty()) {
            TelegramManager.initClient(this, apiId, apiHash)
        } else {
             showMissingApiDialog()
        }
    }

    // --- האזנה להודעות חדשות ---
    override fun onNewMessage(msg: TelegramMsg) {
        runOnUiThread {
            messages.add(0, msg) // הוספה לראש הרשימה
            if (messages.size > 100) messages.removeAt(messages.size - 1) // שמירה על גודל קבוע
            adapter.notifyItemInserted(0)
            findViewById<RecyclerView>(R.id.messages_recycler).scrollToPosition(0)
        }
    }

    override fun onNeedPhone() { runOnUiThread { showPhoneLoginDialog() } }
    override fun onNeedCode() { runOnUiThread { loginDialog?.dismiss(); showCodeLoginDialog() } }
    
    override fun onLoginSuccess() {
        runOnUiThread {
            loginDialog?.dismiss()
            // שומרים סטטוס אבל לא מציגים הודעה מציקה כל פעם
            getSharedPreferences("pasiflon_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("is_logged_in", true).apply()
        }
    }

    override fun onError(msg: String) { runOnUiThread { Toast.makeText(this, "שגיאה: $msg", Toast.LENGTH_LONG).show() } }

    private fun showMissingApiDialog() {
        if (loginDialog?.isShowing == true) return 
        AlertDialog.Builder(this)
            .setTitle("הגדרות נדרשות")
            .setMessage("נא להזין API ID ו-Hash")
            .setPositiveButton("הגדרות") { _, _ -> startActivity(Intent(this, SettingsActivity::class.java)) }
            .setCancelable(false).show()
    }

    private fun showPhoneLoginDialog() {
        if (loginDialog?.isShowing == true) return
        val builder = AlertDialog.Builder(this)
        builder.setTitle("התחברות לטלגרם")
        val input = EditText(this)
        input.hint = "מספר טלפון (+972...)"
        input.inputType = android.text.InputType.TYPE_CLASS_PHONE
        builder.setView(input)
        builder.setPositiveButton("שלח") { _, _ ->
            val phone = input.text.toString()
            if (phone.isNotEmpty()) TelegramManager.sendPhoneNumber(phone)
        }
        builder.setNeutralButton("הגדרות") { _, _ -> startActivity(Intent(this, SettingsActivity::class.java)) }
        builder.setCancelable(false)
        loginDialog = builder.show()
    }

    private fun showCodeLoginDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("קוד אימות")
        val input = EditText(this)
        input.hint = "קוד 5 ספרות"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        builder.setView(input)
        builder.setPositiveButton("אמת") { _, _ ->
            val code = input.text.toString()
            if (code.isNotEmpty()) TelegramManager.sendCode(code)
        }
        builder.setCancelable(false)
        loginDialog = builder.show()
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(permissions)
        }
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
