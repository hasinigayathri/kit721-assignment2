package com.example.interiorquoter

import com.google.firebase.firestore.Exclude

class FloorSpace (
    @get:Exclude var id : String? = null,
    var roomId : String? = null,
    var name : String? = null,
    var widthMm : Double? = null,
    var depthMm : Double? = null,
    var productId : String? = null,
    var productName : String? = null,
    var colorVariant : String? = null,
    var pricePerM2 : Double? = null
)