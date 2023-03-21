package com.gimbal.airship.sample.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "place_events")
data class PlaceEventDataModel(
    @PrimaryKey
    val id: UUID,
    val placeName: String,
    val isArrival: Boolean,
    val timestamp: Long
)
