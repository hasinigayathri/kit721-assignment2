package com.example.interiorquoter

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.interiorquoter.databinding.ActivityAddEditFloorBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import java.net.URL
import kotlin.concurrent.thread

const val FLOOR_ID = "FLOOR_ID"

class AddEditFloorActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddEditFloorBinding
    private val db = Firebase.firestore
    private val floorsCollection get() = db.collection("floors")
    private var floorId: String? = null
    private var roomId: String? = null
    private var productList = listOf<Product>()
    private var selectedProduct: Product? = null

    data class Product(
        val id: String,
        val name: String,
        val pricePerM2: Double,
        val description: String,
        val colours: List<String>,
        val imageUrl: String = ""

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddEditFloorBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        roomId = intent.getStringExtra(ROOM_ID)
        floorId = intent.getStringExtra(FLOOR_ID)

        title = if (floorId != null) "Edit Floor Space" else "Add Floor Space"

        fetchProducts()

        if (floorId != null) {
            floorsCollection.document(floorId!!).get().addOnSuccessListener { doc ->
                val floor = doc.toObject(FloorSpace::class.java)
                floor?.let {
                    ui.etFloorName.setText(it.name)
                    ui.etFloorWidth.setText(it.widthMm.toString())
                    ui.etFloorDepth.setText(it.depthMm.toString())
                }
            }
        }

        ui.btnSaveFloor.setOnClickListener {
            val name = ui.etFloorName.text.toString().trim()
            val widthStr = ui.etFloorWidth.text.toString().trim()
            val depthStr = ui.etFloorDepth.text.toString().trim()

            if (name.isEmpty()) {
                ui.etFloorName.error = "Name is required"
                return@setOnClickListener
            }
            if (widthStr.isEmpty()) {
                ui.etFloorWidth.error = "Width is required"
                return@setOnClickListener
            }
            if (depthStr.isEmpty()) {
                ui.etFloorDepth.error = "Depth is required"
                return@setOnClickListener
            }

            val width = widthStr.toDouble()
            val depth = depthStr.toDouble()
            val colour = ui.spinnerColour.selectedItem?.toString() ?: ""
            val product = selectedProduct

            val floor = FloorSpace(
                name = name,
                widthMm = width,
                depthMm = depth,
                roomId = roomId ?: "",
                productId = product?.id ?: "",
                productName = product?.name ?: "",
                colorVariant = colour,
                pricePerM2 = product?.pricePerM2 ?: 0.0
            )

            if (floorId != null) {
                floorsCollection.document(floorId!!).set(floor).addOnSuccessListener {
                    finish()
                }
            } else {
                floorsCollection.add(floor).addOnSuccessListener {
                    finish()
                }
            }
        }
    }

    private fun fetchProducts() {
        thread {
            try {
                val url = "https://utasbot.dev/kit305_2026/product?category=floor"
                val response = URL(url).readText()
                val jsonObject = org.json.JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("data")
                val products = mutableListOf<Product>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val colours = mutableListOf<String>()
                    val coloursArray = obj.optJSONArray("variants")
                    if (coloursArray != null) {
                        for (j in 0 until coloursArray.length()) {
                            colours.add(coloursArray.getString(j))
                        }
                    }
                    products.add(Product(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        pricePerM2 = obj.getDouble("price_per_sqm"),
                        description = obj.optString("description", ""),
                        colours = colours,
                        imageUrl = obj.optString("imageUrl", "")
                    ))
                }

                productList = products
                runOnUiThread { setupProductSpinner() }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to load products: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupProductSpinner() {
        val names = productList.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ui.spinnerProduct.adapter = adapter

        ui.spinnerProduct.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedProduct = productList[position]
                ui.txtProductPrice.text = "Price: $${selectedProduct!!.pricePerM2}/m²"
                ui.txtProductDescription.text = "Description: ${selectedProduct!!.description}"
                val colourAdapter = ArrayAdapter(this@AddEditFloorActivity, android.R.layout.simple_spinner_item, selectedProduct!!.colours)
                colourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                ui.spinnerColour.adapter = colourAdapter
                ui.imgProductPreview.visibility = android.view.View.VISIBLE
                Glide.with(this@AddEditFloorActivity)
                    .load(selectedProduct!!.imageUrl)
                    .into(ui.imgProductPreview)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}