package com.example.interiorquoter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.interiorquoter.databinding.ActivityMainBinding
import com.example.interiorquoter.databinding.MyHouseItemBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

const val HOUSE_ID = "HOUSE_ID"

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding
    private val db = Firebase.firestore
    private val housesCollection get() = db.collection("houses")
    private val items = mutableListOf<House>()

    inner class HouseHolder(var ui: MyHouseItemBinding) : RecyclerView.ViewHolder(ui.root)

    inner class HouseAdapter(private val houses: MutableList<House>) : RecyclerView.Adapter<HouseHolder>() {
        override fun getItemCount() = houses.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseHolder {
            val ui = MyHouseItemBinding.inflate(layoutInflater, parent, false)
            return HouseHolder(ui)
        }

        override fun onBindViewHolder(holder: HouseHolder, position: Int) {
            val house = houses[position]
            holder.ui.txtName.text = house.customerName
            holder.ui.txtAddress.text = house.address
            holder.ui.txtSummary.text = "Tap to view rooms"

            holder.ui.btnDelete.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Project")
                    .setMessage("Are you sure you want to delete ${house.customerName}? This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        housesCollection.document(house.id!!).delete()
                        houses.removeAt(position)
                        notifyItemRemoved(position)
                        if (houses.isEmpty()) {
                            this@MainActivity.ui.lblStatus.text = "No houses yet\nTap '+ Add House' to create your first quote"
                        } else {
                            this@MainActivity.ui.lblStatus.text = "${houses.size} house(s)"
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            holder.ui.btnEdit.setOnClickListener {
                val intent = Intent(this@MainActivity, AddHouseActivity::class.java)
                intent.putExtra(HOUSE_ID, house.id)
                startActivity(intent)
            }
            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, HouseDetailActivity::class.java)
                intent.putExtra(HOUSE_ID, house.id)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)

        ui.myList.layoutManager = LinearLayoutManager(this)
        ui.myList.adapter = HouseAdapter(items)

        ui.btnAddHouse.setOnClickListener {
            startActivity(Intent(this, AddHouseActivity::class.java))
        }

        loadHouses()
    }

    override fun onResume() {
        super.onResume()
        loadHouses()
    }

    private fun loadHouses() {
        ui.lblStatus.text = "Loading..."
        housesCollection.get().addOnSuccessListener { result ->
            items.clear()
            for (doc in result) {
                val house = doc.toObject(House::class.java)
                house.id = doc.id
                items.add(house)
            }
            ui.myList.adapter?.notifyDataSetChanged()
            if (items.isEmpty()) {
                ui.emptyState.visibility = android.view.View.VISIBLE
                ui.myList.visibility = android.view.View.GONE
                ui.lblStatus.text = ""
            } else {
                ui.emptyState.visibility = android.view.View.GONE
                ui.myList.visibility = android.view.View.VISIBLE
                ui.lblStatus.text = "${items.size} house(s)"
            }
        }


    }
}