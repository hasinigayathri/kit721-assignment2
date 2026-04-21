package com.example.interiorquoter

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
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

    data class QuoteItem(
        val type: String, // "room_header", "space_item", "room_total"
        val label: String,
        val cost: Double = 0.0
    )

    private val quoteItems = mutableListOf<QuoteItem>()
    private var grandTotal = 0.0

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

                var roomsProcessed = 0

                for (roomDoc in rooms) {
                    val room = roomDoc.toObject(Room::class.java) ?: continue
                    room.id = roomDoc.id
                    val roomItems = mutableListOf<QuoteItem>()
                    var roomTotal = 200.0 // labour cost per room

                    quoteItems.add(QuoteItem("room_header", room.name ?: "Room"))

                    var windowsDone = false
                    var floorsDone = false

                    fun checkRoomDone() {
                        if (!windowsDone || !floorsDone) return
                        roomItems.forEach { quoteItems.add(it) }
                        quoteItems.add(QuoteItem("room_total", "Room Total", roomTotal))
                        grandTotal += roomTotal
                        roomsProcessed++
                        if (roomsProcessed == rooms.size) {
                            ui.quoteList.adapter = QuoteAdapter(quoteItems)
                            ui.txtGrandTotal.text = "Total Quote: $${"%.2f".format(grandTotal)}"
                        }
                    }

                    db.collection("windows").whereEqualTo("roomId", room.id).get()
                        .addOnSuccessListener { windowResult ->
                            for (doc in windowResult) {
                                val w = doc.toObject(WindowSpace::class.java)
                                val cost = (w.widthMm ?: 0) / 1000.0 * (w.heightMm ?: 0) / 1000.0 * (w.pricePerM2 ?: 50.0) * (w.panelCount ?: 1)
                                roomTotal += cost
                                roomItems.add(QuoteItem("space_item", "${w.name} — ${w.productName ?: "No product"} (${w.colorVariant ?: ""})", cost))
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
                                roomItems.add(QuoteItem("space_item", "${f.name} — ${f.productName ?: "No product"} (${f.colorVariant ?: ""})", cost))
                            }
                            floorsDone = true
                            checkRoomDone()
                        }
                }
            }
    }

    inner class QuoteAdapter(private val items: List<QuoteItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int) = when (items[position].type) {
            "room_header" -> 0
            "room_total" -> 2
            else -> 1
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val tv = TextView(parent.context)
            tv.setPadding(0, 16, 0, 8)
            return object : RecyclerView.ViewHolder(tv) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            val tv = holder.itemView as TextView
            when (item.type) {
                "room_header" -> {
                    tv.text = "\n${item.label}"
                    tv.textSize = 18f
                    tv.setTypeface(null, android.graphics.Typeface.BOLD)
                }
                "space_item" -> {
                    tv.text = "  ${item.label}  —  $${"%.2f".format(item.cost)}"
                    tv.textSize = 14f
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                "room_total" -> {
                    tv.text = "  ${item.label}: $${"%.2f".format(item.cost)}"
                    tv.textSize = 16f
                    tv.setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
        }
    }

    private fun shareQuote() {
        val sb = StringBuilder()
        sb.appendLine("Quote Summary — $houseName")
        sb.appendLine("=".repeat(30))
        for (item in quoteItems) {
            when (item.type) {
                "room_header" -> sb.appendLine("\n${item.label}")
                "space_item" -> sb.appendLine("  ${item.label} — $${"%.2f".format(item.cost)}")
                "room_total" -> sb.appendLine("  Room Total: $${"%.2f".format(item.cost)}")
            }
        }
        sb.appendLine("\nGrand Total: $${"%.2f".format(grandTotal)}")

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