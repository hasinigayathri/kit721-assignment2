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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddHouseBinding.inflate(layoutInflater)
        setContentView(ui.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Add House"

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

            housesCollection.add(house).addOnSuccessListener {
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}