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

            holder.ui.btnDeleteRoom.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Room")
                    .setMessage("Are you sure you want to delete ${room.name}? This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        roomsCollection.document(room.id!!).delete()
                        rooms.removeAt(position)
                        notifyItemRemoved(position)
                        ui.btnGenerateQuote.isEnabled = rooms.isNotEmpty()
                        if (rooms.isEmpty()) {
                            ui.lblRoomStatus.text = "No rooms added yet\nTap '+ Add Room' to add your first room"
                        } else {
                            ui.lblRoomStatus.text = "${rooms.size} room(s)"
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            holder.ui.btnEditRoom.setOnClickListener {
                val intent = Intent(this@HouseDetailActivity, AddEditRoomActivity::class.java)
                intent.putExtra(HOUSE_ID, houseId)
                intent.putExtra(ROOM_ID, room.id)
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
                ui.btnGenerateQuote.isEnabled = items.isNotEmpty()
                if (items.isEmpty()) {
                    ui.lblRoomStatus.text = "No rooms added yet\nTap '+ Add Room' to add your first room"
                } else {
                    ui.lblRoomStatus.text = "${items.size} room(s)"
                }
            }
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