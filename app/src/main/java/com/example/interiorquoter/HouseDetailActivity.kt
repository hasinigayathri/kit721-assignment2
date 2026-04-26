package com.example.interiorquoter

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.interiorquoter.databinding.ActivityHouseDetailBinding
import com.example.interiorquoter.databinding.MyRoomItemBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HouseDetailActivity : AppCompatActivity() {

    private lateinit var ui: ActivityHouseDetailBinding
    private val db = Firebase.firestore
    private val roomsCollection get() = db.collection("rooms")
    private val items = mutableListOf<Room>()
    private var houseId: String? = null

    private var newlyDuplicatedRoomId: String? = null

    inner class RoomHolder(var ui: MyRoomItemBinding) : RecyclerView.ViewHolder(ui.root)

    inner class RoomAdapter(private val rooms: MutableList<Room>) : RecyclerView.Adapter<RoomHolder>() {
        override fun getItemCount() = rooms.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomHolder {
            val ui = MyRoomItemBinding.inflate(layoutInflater, parent, false)
            return RoomHolder(ui)
        }

        override fun onBindViewHolder(holder: RoomHolder, position: Int) {
            val room = rooms[position]
            holder.ui.txtRoomName.text = room.name
            holder.ui.txtRoomType.text = room.type
            holder.ui.txtRoomSummary.text = "Tap to view details"
            if (room.id == newlyDuplicatedRoomId) {
                holder.ui.lblNew.visibility = android.view.View.VISIBLE
                holder.itemView.setBackgroundColor(getColor(R.color.teal_very_light))
            } else {
                holder.ui.lblNew.visibility = android.view.View.GONE
                holder.itemView.setBackgroundColor(getColor(android.R.color.white))
            }

            holder.ui.btnDeleteRoom.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Room")
                    .setMessage("Are you sure you want to delete ${room.name}? This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        val currentPosition = holder.adapterPosition
                        if (currentPosition == RecyclerView.NO_ID.toInt()) return@setPositiveButton

                        // Delete all windows for this room
                        db.collection("windows").whereEqualTo("roomId", room.id).get()
                            .addOnSuccessListener { windows ->
                                for (doc in windows) doc.reference.delete()
                            }

                        // Delete all floors for this room
                        db.collection("floors").whereEqualTo("roomId", room.id).get()
                            .addOnSuccessListener { floors ->
                                for (doc in floors) doc.reference.delete()
                            }

                        // Delete the room itself
                        roomsCollection.document(room.id!!).delete()
                        rooms.removeAt(currentPosition)
                        notifyItemRemoved(currentPosition)
                        this@HouseDetailActivity.ui.btnGenerateQuote.isEnabled = rooms.isNotEmpty()
                        if (rooms.isEmpty()) {
                            this@HouseDetailActivity.ui.emptyRoomState.visibility = android.view.View.VISIBLE
                            this@HouseDetailActivity.ui.lblRoomStatus.visibility = android.view.View.GONE
                            this@HouseDetailActivity.ui.lblDuplicateHint.visibility = android.view.View.GONE
                        } else {
                            this@HouseDetailActivity.ui.lblRoomStatus.text = "${rooms.size} room(s)"
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                    .also { dialog ->
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(getColor(R.color.teal_primary))
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(android.graphics.Color.RED)
                    }
            }
            holder.ui.btnEditRoom.setOnClickListener {
                val intent = Intent(this@HouseDetailActivity, AddEditRoomActivity::class.java)
                intent.putExtra(HOUSE_ID, houseId)
                intent.putExtra(ROOM_ID, room.id)
                startActivity(intent)
            }

            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Duplicate Room")
                    .setMessage("A copy of '${room.name}' will be created with all its windows and floor spaces.")
                    .setPositiveButton("Duplicate") { _, _ ->
                        duplicateRoom(room)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                    .also { dialog ->
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(android.graphics.Color.RED)
                    }
                true
            }
            holder.itemView.setOnClickListener {
                val intent = Intent(this@HouseDetailActivity, RoomDetailActivity::class.java)
                intent.putExtra(ROOM_ID, room.id)
                intent.putExtra(HOUSE_ID, houseId)
                intent.putExtra("ROOM_NAME", room.name)
                intent.putExtra("PHOTO_URL", room.photoUrl)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityHouseDetailBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        houseId = intent.getStringExtra(HOUSE_ID)

        loadHouseDetails()
        loadRooms()

        ui.btnAddRoom.setOnClickListener {
            val intent = Intent(this, AddEditRoomActivity::class.java)
            intent.putExtra(HOUSE_ID, houseId)
            startActivity(intent)
        }

        ui.btnGenerateQuote.setOnClickListener {
            val intent = Intent(this, QuoteActivity::class.java)
            intent.putExtra(HOUSE_ID, houseId)
            intent.putExtra("HOUSE_NAME", ui.txtHouseName.text.toString())
            startActivity(intent)
        }
    }

    private fun loadHouseDetails() {
        houseId?.let {
            db.collection("houses").document(it).get().addOnSuccessListener { doc ->
                val house = doc.toObject(House::class.java)
                house?.let { h ->
                    title = h.customerName
                    ui.txtHouseName.text = h.customerName
                    ui.txtHouseAddress.text = "${h.address}, ${h.suburb}"
                }
            }
        }
    }

    private fun loadRooms() {
        ui.lblRoomStatus.text = "Loading..."
        houseId?.let {
            roomsCollection.whereEqualTo("houseId", it).get().addOnSuccessListener { result ->
                items.clear()
                for (doc in result) {
                    val room = doc.toObject(Room::class.java)
                    room.id = doc.id
                    items.add(room)
                }
                ui.roomList.layoutManager = LinearLayoutManager(this)
                ui.roomList.adapter = RoomAdapter(items)
                ui.lblDuplicateHint.visibility = if (items.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                ui.btnGenerateQuote.isEnabled = items.isNotEmpty()
                if (items.isEmpty()) {
                    ui.emptyRoomState.visibility = android.view.View.VISIBLE
                    ui.lblRoomStatus.visibility = android.view.View.GONE
                    ui.lblDuplicateHint.visibility = android.view.View.GONE
                } else {
                    ui.emptyRoomState.visibility = android.view.View.GONE
                    ui.lblRoomStatus.visibility = android.view.View.VISIBLE
                    ui.lblRoomStatus.text = "${items.size} room(s)"
                    ui.lblDuplicateHint.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    private fun duplicateRoom(room: Room) {
        val newRoom = Room(
            name = "${room.name} (Copy)",
            type = room.type,
            houseId = room.houseId
        )
        roomsCollection.add(newRoom).addOnSuccessListener { newRoomRef ->
            val newRoomId = newRoomRef.id

            db.collection("windows").whereEqualTo("roomId", room.id).get()
                .addOnSuccessListener { windows ->
                    for (doc in windows) {
                        val window = doc.toObject(WindowSpace::class.java)
                        window.id = null
                        val newWindow = WindowSpace(
                            name = window.name,
                            widthMm = window.widthMm,
                            heightMm = window.heightMm,
                            roomId = newRoomId,
                            productId = window.productId,
                            productName = window.productName,
                            colorVariant = window.colorVariant,
                            panelCount = window.panelCount,
                            pricePerM2 = window.pricePerM2
                        )
                        db.collection("windows").add(newWindow)
                    }
                }

            db.collection("floors").whereEqualTo("roomId", room.id).get()
                .addOnSuccessListener { floors ->
                    for (doc in floors) {
                        val floor = doc.toObject(FloorSpace::class.java)
                        val newFloor = FloorSpace(
                            name = floor.name,
                            widthMm = floor.widthMm,
                            depthMm = floor.depthMm,
                            roomId = newRoomId,
                            productId = floor.productId,
                            productName = floor.productName,
                            colorVariant = floor.colorVariant,
                            pricePerM2 = floor.pricePerM2
                        )
                        db.collection("floors").add(newFloor)
                    }
                }

            newlyDuplicatedRoomId = newRoomRef.id
            loadRooms()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadRooms()
    }
}