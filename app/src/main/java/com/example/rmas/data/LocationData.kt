package com.example.rmas.data

import com.google.firebase.Timestamp
import java.io.Serializable

data class LocationData(
    var id: String,
    var name: String,
    var address: String,
    val longitude: Double,
    val latitude: Double,
    var photos: String,
    val timeCreated: Timestamp,
    val author: String,
    val authorId: String,
    var likes: Int = 0
) : Serializable {

    // Custom property to store timeCreated as Long
    val timeCreatedMillis: Long
        get() = timeCreated.seconds * 1000 + timeCreated.nanoseconds / 1000000

    constructor() : this("", "",  "", 0.0, 0.0,   "", Timestamp.now(), "", "",0)

    }
