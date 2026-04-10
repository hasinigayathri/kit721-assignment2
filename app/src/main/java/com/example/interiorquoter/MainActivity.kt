package com.example.interiorquoter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.interiorquoter.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

const val FIREBASE_TAG = "FirebaseLogging"

class MainActivity : AppCompatActivity()
{
    private lateinit var ui : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // test Firebase connection
        val db = Firebase.firestore
        Log.d(FIREBASE_TAG, "Firebase connected: ${db.app.name}")
    }
}