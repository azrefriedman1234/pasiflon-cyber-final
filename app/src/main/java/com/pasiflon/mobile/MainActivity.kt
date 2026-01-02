package com.pasiflon.mobile

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import android.graphics.Color

data class TelegramMsg(val id: Long, val sender: String, var text: String)

class MainActivity : AppCompatActivity() {
    private val messages = mutableListOf<TelegramMsg>()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler = findViewById<RecyclerView>(R.id.messages_recycler)
        adapter = MessageAdapter(messages)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // סימולציה של הודעות נכנסות (במקום ה-TDLib שמתחבר ברקע)
        for (i in 1..20) {
            messages.add(TelegramMsg(i.toLong(), "ערוץ חדשות $i", "דיווח ראשוני מהשטח... הודעה מספר $i"))
        }
        adapter.notifyDataSetChanged()
    }

    inner class MessageAdapter(private val list: List<TelegramMsg>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val sender = v.findViewById<TextView>(android.R.id.text1)
            val content = v.findViewById<TextView>(android.R.id.text2)
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
                Toast.makeText(this@MainActivity, "פותח עורך מדיה להודעה ${msg.id}", Toast.LENGTH_SHORT).show()
                // כאן יפתח מסך הפרטים עם ה-Blur
            }
        }
        override fun getItemCount() = list.size
    }
}
