package com.example.interiorquoter

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.interiorquoter.databinding.ActivityQuoteBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class QuoteActivity : AppCompatActivity() {

    private lateinit var ui: ActivityQuoteBinding
    private val db = Firebase.firestore
    private var houseId: String? = null
    private var houseName: String? = null
    private var currentRoom = ""

    data class QuoteItem(
        val type: String,
        val label: String,
        val cost: Double = 0.0,
        val roomName: String = ""
    )

    private val quoteItems = mutableListOf<QuoteItem>()
    private var grandTotal = 0.0
    private val excludedRooms = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityQuoteBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Quote Summary"

        houseId = intent.getStringExtra(HOUSE_ID)
        houseName = intent.getStringExtra("HOUSE_NAME")

        ui.quoteList.layoutManager = LinearLayoutManager(this)

        loadQuote()

        ui.btnShare.setOnClickListener {
            shareQuote()
        }
    }

    private fun loadQuote() {
        houseId ?: return
        quoteItems.clear()
        grandTotal = 0.0

        db.collection("rooms").whereEqualTo("houseId", houseId).get()
            .addOnSuccessListener { roomResult ->
                val rooms = roomResult.documents
                if (rooms.isEmpty()) {
                    ui.txtGrandTotal.text = "Total Quote: $0.00"
                    return@addOnSuccessListener
                }

                val allRoomData = mutableListOf<Pair<String, MutableList<QuoteItem>>>()
                var roomsProcessed = 0

                for (roomDoc in rooms) {
                    val room = roomDoc.toObject(Room::class.java) ?: continue
                    room.id = roomDoc.id
                    val roomName = room.name ?: "Room"
                    val roomItems = mutableListOf<QuoteItem>()
                    var roomTotal = 200.0
                    var windowsDone = false
                    var floorsDone = false

                    val roomEntry = Pair(roomName, roomItems)
                    allRoomData.add(roomEntry)

                    fun checkRoomDone() {
                        if (!windowsDone || !floorsDone) return
                        roomItems.add(0, QuoteItem("room_header", roomName, roomName = roomName))
                        roomItems.add(QuoteItem("room_total", "Room Total", roomTotal, roomName = roomName))
                        grandTotal += roomTotal
                        roomsProcessed++

                        if (roomsProcessed == rooms.size) {
                            quoteItems.clear()
                            for (entry in allRoomData) {
                                quoteItems.addAll(entry.second)
                            }
                            ui.quoteList.adapter = QuoteAdapter(quoteItems)
                            updateGrandTotal()
                        }
                    }

                    db.collection("windows").whereEqualTo("roomId", room.id).get()
                        .addOnSuccessListener { windowResult ->
                            for (doc in windowResult) {
                                val w = doc.toObject(WindowSpace::class.java)
                                val cost = (w.widthMm ?: 0) / 1000.0 * (w.heightMm ?: 0) / 1000.0 * (w.pricePerM2 ?: 50.0) * (w.panelCount ?: 1)
                                roomTotal += cost
                                roomItems.add(QuoteItem("space_item", "${w.name} — ${w.productName ?: "No product"} (${w.colorVariant ?: ""})", cost, roomName))
                            }
                            windowsDone = true
                            checkRoomDone()
                        }

                    db.collection("floors").whereEqualTo("roomId", room.id).get()
                        .addOnSuccessListener { floorResult ->
                            for (doc in floorResult) {
                                val f = doc.toObject(FloorSpace::class.java)
                                val cost = (f.widthMm ?: 0.0) / 1000.0 * (f.depthMm ?: 0.0) / 1000.0 * (f.pricePerM2 ?: 100.0)
                                roomTotal += cost
                                roomItems.add(QuoteItem("space_item", "${f.name} — ${f.productName ?: "No product"} (${f.colorVariant ?: ""})", cost, roomName))
                            }
                            floorsDone = true
                            checkRoomDone()
                        }
                }
            }
    }

    private fun updateGrandTotal() {
        var total = 0.0
        for (item in quoteItems) {
            if (item.type == "room_total" && !excludedRooms.contains(item.roomName)) {
                total += item.cost
            }
        }
        ui.txtGrandTotal.text = "Total Quote: $${"%.2f".format(total)}"
    }

    inner class QuoteAdapter(private val items: List<QuoteItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int) = when (items[position].type) {
            "room_header" -> 0
            "room_total" -> 2
            else -> 1
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 -> {
                    val layout = LinearLayout(parent.context)
                    layout.orientation = LinearLayout.HORIZONTAL
                    layout.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val cb = CheckBox(parent.context)
                    cb.isChecked = true
                    val tv = TextView(parent.context)
                    tv.setPadding(16, 16, 0, 8)
                    tv.textSize = 18f
                    tv.setTypeface(null, android.graphics.Typeface.BOLD)
                    layout.addView(cb)
                    layout.addView(tv)
                    object : RecyclerView.ViewHolder(layout) {}
                }
                else -> {
                    val tv = TextView(parent.context)
                    tv.setPadding(0, 16, 0, 8)
                    object : RecyclerView.ViewHolder(tv) {}
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            when (item.type) {
                "room_header" -> {
                    val layout = holder.itemView as LinearLayout
                    val cb = layout.getChildAt(0) as CheckBox
                    val tv = layout.getChildAt(1) as TextView
                    tv.text = item.label
                    cb.isChecked = !excludedRooms.contains(item.roomName)
                    cb.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) excludedRooms.remove(item.roomName)
                        else excludedRooms.add(item.roomName)
                        updateGrandTotal()
                    }
                }
                "space_item" -> {
                    val tv = holder.itemView as TextView
                    tv.text = "  ${item.label}  —  $${"%.2f".format(item.cost)}"
                    tv.textSize = 14f
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                "room_total" -> {
                    val tv = holder.itemView as TextView
                    tv.text = "  ${item.label}: $${"%.2f".format(item.cost)}"
                    tv.textSize = 16f
                    tv.setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
        }
    }

    private fun shareQuote() {
        val sb = StringBuilder()
        sb.appendLine("House,Room,Item,Cost")
        for (item in quoteItems) {
            if (excludedRooms.contains(item.roomName)) continue
            when (item.type) {
                "room_header" -> currentRoom = item.label
                "space_item" -> sb.appendLine("${houseName},${currentRoom},${item.label},${"%.2f".format(item.cost)}")
                "room_total" -> sb.appendLine("${houseName},${currentRoom},Room Total,${"%.2f".format(item.cost)}")
            }
        }
        sb.appendLine("${houseName},,,Grand Total,${"%.2f".format(quoteItems.filter { it.type == "room_total" && !excludedRooms.contains(it.roomName) }.sumOf { it.cost })}")

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(shareIntent, "Share Quote"))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}