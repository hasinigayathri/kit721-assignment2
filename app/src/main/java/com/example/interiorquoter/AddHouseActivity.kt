package com.example.interiorquoter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.interiorquoter.databinding.ActivityAddHouseBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddHouseActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddHouseBinding
    private val db = Firebase.firestore
    private val housesCollection get() = db.collection("houses")
    private var houseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddHouseBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        houseId = intent.getStringExtra(HOUSE_ID)

        if (houseId != null) {
            title = "Edit House"
            housesCollection.document(houseId!!).get().addOnSuccessListener { doc ->
                val house = doc.toObject(House::class.java)
                house?.let {
                    ui.etName.setText(it.customerName)
                    ui.etAddress.setText(it.address)
                    ui.etSuburb.setText(it.suburb)
                    ui.etPhone.setText(it.phone)
                }
            }
        } else {
            title = "Add House"
        }

        ui.btnSave.setOnClickListener {
            val name = ui.etName.text.toString().trim()
            val address = ui.etAddress.text.toString().trim()
            val suburb = ui.etSuburb.text.toString().trim()
            val phone = ui.etPhone.text.toString().trim()

            if (name.isEmpty()) {
                ui.etName.error = "Name is required"
                return@setOnClickListener
            }
            if (address.isEmpty()) {
                ui.etAddress.error = "Address is required"
                return@setOnClickListener
            }

            val house = House(
                customerName = name,
                address = address,
                suburb = suburb,
                phone = phone
            )

            if (houseId != null) {
                housesCollection.document(houseId!!).set(house).addOnSuccessListener {
                    finish()
                }
            } else {
                housesCollection.add(house).addOnSuccessListener {
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