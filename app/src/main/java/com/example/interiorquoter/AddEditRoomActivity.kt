package com.example.interiorquoter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.interiorquoter.databinding.ActivityAddEditRoomBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

const val ROOM_ID = "ROOM_ID"

class AddEditRoomActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddEditRoomBinding
    private val db = Firebase.firestore
    private val roomsCollection get() = db.collection("rooms")
    private var roomId: String? = null
    private var houseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddEditRoomBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        houseId = intent.getStringExtra(HOUSE_ID)
        roomId = intent.getStringExtra(ROOM_ID)

        if (roomId != null) {
            title = "Edit Room"
            roomsCollection.document(roomId!!).get().addOnSuccessListener { doc ->
                val room = doc.toObject(Room::class.java)
                room?.let {
                    ui.etRoomName.setText(it.name)
                    ui.etRoomType.setText(it.type)
                }
            }
        } else {
            title = "Add Room"
        }

        ui.btnSaveRoom.setOnClickListener {
            val name = ui.etRoomName.text.toString().trim()
            val type = ui.etRoomType.text.toString().trim()

            if (name.isEmpty()) {
                ui.etRoomName.error = "Room name is required"
                return@setOnClickListener
            }

            val room = Room(
                name = name,
                type = type,
                houseId = houseId ?: ""
            )

            if (roomId != null) {
                roomsCollection.document(roomId!!).set(room).addOnSuccessListener {
                    finish()
                }
            } else {
                roomsCollection.add(room).addOnSuccessListener {
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}