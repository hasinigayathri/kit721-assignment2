package com.example.interiorquoter

import com.google.firebase.firestore.Exclude

class House (
    @get:Exclude var id : String? = null,
    var customerName : String? = null,
    var address : String? = null,
    var suburb : String? = null,
    var phone : String? = null
)