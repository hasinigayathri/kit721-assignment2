package com.example.interiorquoter

import com.google.firebase.firestore.Exclude

class Room (
    @get:Exclude var id : String? = null,
    var houseId : String? = null,
    var name : String? = null,
    var type : String? = null,
    var photoUrl : String? = null,
    var notes : String? = null
)