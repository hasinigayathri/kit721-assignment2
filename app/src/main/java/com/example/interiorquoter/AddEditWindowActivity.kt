package com.example.interiorquoter

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.interiorquoter.databinding.ActivityAddEditWindowBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.ceil

const val WINDOW_ID = "WINDOW_ID"

class AddEditWindowActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddEditWindowBinding
    private val db = Firebase.firestore
    private val windowsCollection get() = db.collection("windows")
    private var windowId: String? = null
    private var roomId: String? = null
    private var productList = listOf<WindowProduct>()
    private var selectedProduct: WindowProduct? = null
    private var isConstraintValid = false

    data class WindowProduct(
        val id: String,
        val name: String,
        val pricePerM2: Double,
        val description: String,
        val colours: List<String>,
        val minWidth: Double,
        val maxWidth: Double,
        val minHeight: Double,
        val maxHeight: Double,
        val maxPanels: Int,
        val imageUrl: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddEditWindowBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        roomId = intent.getStringExtra(ROOM_ID)
        windowId = intent.getStringExtra(WINDOW_ID)
        title = if (windowId != null) "Edit Window" else "Add Window"

        fetchProducts()

        if (windowId != null) {
            windowsCollection.document(windowId!!).get().addOnSuccessListener { doc ->
                val window = doc.toObject(WindowSpace::class.java)
                window?.let {
                    ui.etWindowName.setText(it.name)
                    ui.etWindowWidth.setText(it.widthMm.toString())
                    ui.etWindowHeight.setText(it.heightMm.toString())
                }
            }
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { checkConstraints() }
        }
        ui.etWindowWidth.addTextChangedListener(watcher)
        ui.etWindowHeight.addTextChangedListener(watcher)

        ui.btnSaveWindow.setOnClickListener {
            val name = ui.etWindowName.text.toString().trim()
            val widthStr = ui.etWindowWidth.text.toString().trim()
            val heightStr = ui.etWindowHeight.text.toString().trim()

            if (name.isEmpty()) { ui.etWindowName.error = "Name is required"; return@setOnClickListener }
            if (widthStr.isEmpty()) { ui.etWindowWidth.error = "Width is required"; return@setOnClickListener }
            if (heightStr.isEmpty()) { ui.etWindowHeight.error = "Height is required"; return@setOnClickListener }
            if (!isConstraintValid) { Toast.makeText(this, "Please fix constraint errors", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val width = widthStr.toDouble()
            val height = heightStr.toDouble()
            val product = selectedProduct
            val panels = calculatePanels(width, product)
            val colour = ui.spinnerColour.selectedItem?.toString() ?: ""

            val window = WindowSpace(
                name = name,
                widthMm = width.toInt(),
                heightMm = height.toInt(),
                roomId = roomId ?: "",
                productId = product?.id ?: "",
                productName = product?.name ?: "",
                colorVariant = colour,
                panelCount = panels,
                pricePerM2 = product?.pricePerM2 ?: 0.0
            )

            if (windowId != null) {
                windowsCollection.document(windowId!!).set(window).addOnSuccessListener { finish() }
            } else {
                windowsCollection.add(window).addOnSuccessListener { finish() }
            }
        }
    }

    private fun calculatePanels(width: Double, product: WindowProduct?): Int {
        if (product == null) return 1
        if (width <= product.maxWidth) return 1
        return ceil(width / product.maxWidth).toInt()
    }

    private fun checkConstraints() {
        val product = selectedProduct ?: return
        val widthStr = ui.etWindowWidth.text.toString().trim()
        val heightStr = ui.etWindowHeight.text.toString().trim()
        if (widthStr.isEmpty() || heightStr.isEmpty()) return

        val width = widthStr.toDoubleOrNull() ?: return
        val height = heightStr.toDoubleOrNull() ?: return

        // Constraint 1: Height check
        if (height < product.minHeight || height > product.maxHeight) {
            showError("Height must be between ${product.minHeight.toInt()}mm and ${product.maxHeight.toInt()}mm")
            return
        }

        // Constraint 2: Direct fit
        if (width <= product.maxWidth && width >= product.minWidth) {
            showSuccess("Direct Fit — Width within ${product.minWidth.toInt()}–${product.maxWidth.toInt()}mm range")
            return
        }

        // Constraint 3 & 4: Panel splitting
        if (width > product.maxWidth) {
            val panels = ceil(width / product.maxWidth).toInt()
            val panelWidth = width / panels

            if (panels > product.maxPanels) {
                showError("Exceeds maximum ${product.maxPanels} panels for this product")
                return
            }

            if (panelWidth < product.minWidth || panelWidth > product.maxWidth) {
                showError("Width exceeds max ${product.maxWidth.toInt()}mm — cannot split into valid panels")
                return
            }

            showSuccess("${panels} panels x ${panelWidth.toInt()}mm each within ${product.minWidth.toInt()}–${product.maxWidth.toInt()}mm range")
            return
        }

        // Width too small
        if (width < product.minWidth) {
            showError("Width must be at least ${product.minWidth.toInt()}mm")
        }
    }

    private fun showError(message: String) {
        ui.txtConstraintMessage.text = message
        ui.txtConstraintMessage.setTextColor(getColor(android.R.color.holo_red_dark))
        ui.btnSaveWindow.isEnabled = false
        isConstraintValid = false
    }

    private fun showSuccess(message: String) {
        ui.txtConstraintMessage.text = message
        ui.txtConstraintMessage.setTextColor(getColor(android.R.color.holo_green_dark))
        ui.btnSaveWindow.isEnabled = true
        isConstraintValid = true
    }

    private fun fetchProducts() {
        thread {
            try {
                val url = "https://utasbot.dev/kit305_2026/product?category=window"
                val response = URL(url).readText()
                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("data")
                val products = mutableListOf<WindowProduct>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val colours = mutableListOf<String>()
                    val variantsArray = obj.optJSONArray("variants")
                    if (variantsArray != null) {
                        for (j in 0 until variantsArray.length()) {
                            colours.add(variantsArray.getString(j))
                        }
                    }
                    products.add(WindowProduct(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        pricePerM2 = obj.getDouble("price_per_sqm"),
                        description = obj.optString("description", ""),
                        colours = colours,
                        minWidth = obj.optDouble("min_width", 0.0),
                        maxWidth = obj.optDouble("max_width", Double.MAX_VALUE),
                        minHeight = obj.optDouble("min_height", 0.0),
                        maxHeight = obj.optDouble("max_height", Double.MAX_VALUE),
                        maxPanels = obj.optInt("max_panels", 1),
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
                val colourAdapter = ArrayAdapter(this@AddEditWindowActivity, android.R.layout.simple_spinner_item, selectedProduct!!.colours)
                colourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                ui.spinnerColour.adapter = colourAdapter
                checkConstraints()
                ui.imgProductPreview.visibility = android.view.View.VISIBLE
                Glide.with(this@AddEditWindowActivity)
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