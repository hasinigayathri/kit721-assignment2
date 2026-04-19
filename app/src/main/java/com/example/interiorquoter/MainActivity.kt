package com.example.interiorquoter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
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
                ui.lblStatus.text = "No houses yet\nTap '+ Add House' to create your first quote"
            } else {
                ui.lblStatus.text = "${items.size} house(s)"
            }
        }
    }
}