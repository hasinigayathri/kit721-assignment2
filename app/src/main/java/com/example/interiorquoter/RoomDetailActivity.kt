package com.example.interiorquoter

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.interiorquoter.databinding.ActivityRoomDetailBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RoomDetailActivity : AppCompatActivity() {

    private lateinit var ui: ActivityRoomDetailBinding
    private val db = Firebase.firestore
    private val windowsCollection get() = db.collection("windows")
    private val floorsCollection get() = db.collection("floors")
    private val windowItems = mutableListOf<WindowSpace>()
    private val floorItems = mutableListOf<FloorSpace>()
    private var roomId: String? = null
    private var houseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityRoomDetailBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        roomId = intent.getStringExtra(ROOM_ID)
        houseId = intent.getStringExtra(HOUSE_ID)
        title = intent.getStringExtra("ROOM_NAME") ?: "Room Detail"

        ui.btnAddWindow.setOnClickListener {
            // Wire up later
        }

        ui.btnAddFloor.setOnClickListener {
            val intent = Intent(this, AddEditFloorActivity::class.java)
            intent.putExtra(ROOM_ID, roomId)
            startActivity(intent)
        }

        loadWindows()
        loadFloors()
    }

    private fun loadWindows() {
        roomId?.let {
            windowsCollection.whereEqualTo("roomId", it).get().addOnSuccessListener { result ->
                windowItems.clear()
                for (doc in result) {
                    val window = doc.toObject(WindowSpace::class.java)
                    window.id = doc.id
                    windowItems.add(window)
                }
                ui.lblWindows.text = "Windows [${windowItems.size}]"
                ui.lblNoWindows.visibility = if (windowItems.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                ui.windowList.layoutManager = LinearLayoutManager(this)
                updateQuoteButton()
            }
        }
    }

    private fun loadFloors() {
        roomId?.let {
            floorsCollection.whereEqualTo("roomId", it).get().addOnSuccessListener { result ->
                floorItems.clear()
                for (doc in result) {
                    val floor = doc.toObject(FloorSpace::class.java)
                    floor.id = doc.id
                    floorItems.add(floor)
                }
                ui.lblFloors.text = "Floor Spaces [${floorItems.size}]"
                ui.lblNoFloors.visibility = if (floorItems.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                ui.floorList.layoutManager = LinearLayoutManager(this)
                updateQuoteButton()
            }
        }
    }

    private fun updateQuoteButton() {
        ui.btnGenerateQuote.isEnabled = windowItems.isNotEmpty() || floorItems.isNotEmpty()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadWindows()
        loadFloors()
    }
}