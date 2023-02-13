package com.gimbal.airship.sample.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place_events")
data class PlaceEventDataModel(
    @PrimaryKey
    val id: String,
    val place: String,
    val arrivalTimeInMillis: Long,
    val departureTimeInMillis: Long,
)