package com.example.interiorquoter

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.interiorquoter.databinding.ActivityAddEditRoomBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import java.io.FileOutputStream

const val ROOM_ID = "ROOM_ID"

class AddEditRoomActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddEditRoomBinding
    private val db = Firebase.firestore
    private val roomsCollection get() = db.collection("rooms")
    private var roomId: String? = null
    private var houseId: String? = null
    private var selectedPhotoUri: Uri? = null
    private var existingPhotoUrl: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedPhotoUri = it
            ui.imgRoomPhoto.visibility = View.VISIBLE
            Glide.with(this).load(it).into(ui.imgRoomPhoto)
        }
    }

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
                    if (!it.photoUrl.isNullOrEmpty()) {
                        existingPhotoUrl = it.photoUrl
                        ui.imgRoomPhoto.visibility = View.VISIBLE
                        Glide.with(this).load(File(it.photoUrl)).into(ui.imgRoomPhoto)
                    }
                }
            }
        } else {
            title = "Add Room"
        }

        ui.btnSelectPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        ui.btnSaveRoom.setOnClickListener {
            val name = ui.etRoomName.text.toString().trim()
            val type = ui.etRoomType.text.toString().trim()

            if (name.isEmpty()) {
                ui.etRoomName.error = "Room name is required"
                return@setOnClickListener
            }

            if (selectedPhotoUri != null) {
                val localPath = saveImageLocally(selectedPhotoUri!!)
                saveRoom(name, type, localPath)
            } else {
                saveRoom(name, type, existingPhotoUrl)
            }
        }
    }

    private fun saveImageLocally(uri: Uri): String {
        val fileName = "room_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, fileName)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    private fun saveRoom(name: String, type: String, photoUrl: String?) {
        val room = Room(
            name = name,
            type = type,
            houseId = houseId ?: "",
            photoUrl = photoUrl
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}