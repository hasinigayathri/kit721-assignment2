package com.example.interiorquoter

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.interiorquoter.databinding.ActivityRoomDetailBinding
import com.example.interiorquoter.databinding.MyWindowItemBinding
import com.example.interiorquoter.databinding.MyFloorItemBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.bumptech.glide.Glide

class RoomDetailActivity : AppCompatActivity() {

    private lateinit var ui: ActivityRoomDetailBinding
    private val db = Firebase.firestore
    private val windowsCollection get() = db.collection("windows")
    private val floorsCollection get() = db.collection("floors")
    private val windowItems = mutableListOf<WindowSpace>()
    private val floorItems = mutableListOf<FloorSpace>()
    private var roomId: String? = null
    private var houseId: String? = null

    inner class WindowHolder(var ui: MyWindowItemBinding) : RecyclerView.ViewHolder(ui.root)
    inner class FloorHolder(var ui: MyFloorItemBinding) : RecyclerView.ViewHolder(ui.root)

    inner class WindowAdapter(private val windows: MutableList<WindowSpace>) : RecyclerView.Adapter<WindowHolder>() {
        override fun getItemCount() = windows.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowHolder {
            val ui = MyWindowItemBinding.inflate(layoutInflater, parent, false)
            return WindowHolder(ui)
        }
        override fun onBindViewHolder(holder: WindowHolder, position: Int) {
            val window = windows[position]
            holder.ui.txtWindowName.text = window.name
            holder.ui.txtWindowSize.text = "Size: ${window.widthMm}mm x ${window.heightMm}mm"
            holder.ui.txtWindowProduct.text = "Product: ${window.productName ?: "None"}"

            holder.ui.btnEditWindow.setOnClickListener {
                val intent = Intent(this@RoomDetailActivity, AddEditWindowActivity::class.java)
                intent.putExtra(ROOM_ID, roomId)
                intent.putExtra(WINDOW_ID, window.id)
                startActivity(intent)
            }

            holder.ui.btnDeleteWindow.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Window")
                    .setMessage("Are you sure you want to delete ${window.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        windowsCollection.document(window.id!!).delete()
                        windows.removeAt(position)
                        notifyItemRemoved(position)
                        ui.lblWindows.text = "Windows [${windows.size}]"
                        ui.lblNoWindows.visibility = if (windows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                        updateQuoteButton()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    inner class FloorAdapter(private val floors: MutableList<FloorSpace>) : RecyclerView.Adapter<FloorHolder>() {
        override fun getItemCount() = floors.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloorHolder {
            val ui = MyFloorItemBinding.inflate(layoutInflater, parent, false)
            return FloorHolder(ui)
        }
        override fun onBindViewHolder(holder: FloorHolder, position: Int) {
            val floor = floors[position]
            holder.ui.txtFloorName.text = floor.name
            holder.ui.txtFloorSize.text = "Size: ${floor.widthMm}mm x ${floor.depthMm}mm"
            holder.ui.txtFloorProduct.text = "Product: ${floor.productName ?: "None"}"

            holder.ui.btnEditFloor.setOnClickListener {
                val intent = Intent(this@RoomDetailActivity, AddEditFloorActivity::class.java)
                intent.putExtra(ROOM_ID, roomId)
                intent.putExtra(FLOOR_ID, floor.id)
                startActivity(intent)
            }

            holder.ui.btnDeleteFloor.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Floor Space")
                    .setMessage("Are you sure you want to delete ${floor.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        floorsCollection.document(floor.id!!).delete()
                        floors.removeAt(position)
                        notifyItemRemoved(position)
                        ui.lblFloors.text = "Floor Spaces [${floors.size}]"
                        ui.lblNoFloors.visibility = if (floors.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                        updateQuoteButton()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityRoomDetailBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        roomId = intent.getStringExtra(ROOM_ID)
        houseId = intent.getStringExtra(HOUSE_ID)
        title = intent.getStringExtra("ROOM_NAME") ?: "Room Detail"

        val photoUrl = intent.getStringExtra("PHOTO_URL")
        if (!photoUrl.isNullOrEmpty()) {
            ui.imgRoomPhoto.visibility = android.view.View.VISIBLE
            Glide.with(this).load(java.io.File(photoUrl)).into(ui.imgRoomPhoto)
        }

        ui.btnAddWindow.setOnClickListener {
            val intent = Intent(this, AddEditWindowActivity::class.java)
            intent.putExtra(ROOM_ID, roomId)
            startActivity(intent)
        }

        ui.btnAddFloor.setOnClickListener {
            val intent = Intent(this, AddEditFloorActivity::class.java)
            intent.putExtra(ROOM_ID, roomId)
            startActivity(intent)
        }

        ui.btnGenerateQuote.setOnClickListener {
            val intent = Intent(this, QuoteActivity::class.java)
            intent.putExtra(HOUSE_ID, houseId)
            intent.putExtra("HOUSE_NAME", title.toString())
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
                ui.windowList.adapter = WindowAdapter(windowItems)
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
                ui.floorList.adapter = FloorAdapter(floorItems)
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