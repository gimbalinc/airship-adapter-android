package com.gimbal.airship.sample.mapper

import com.gimbal.airship.sample.data.PlaceEventDataModel
import com.gimbal.airship.sample.domain.PlaceEventDomainModel
import com.gimbal.android.Visit
import java.time.Instant
import java.util.*

val Visit.toDomainModel: PlaceEventDomainModel
    get() = PlaceEventDomainModel(
        placeName = this.place.name,
        isArrival = this.departureTimeInMillis == 0L,
        timestamp = if (this.departureTimeInMillis == 0L)
            Instant.ofEpochMilli(this.arrivalTimeInMillis)
        else Instant.ofEpochMilli(this.departureTimeInMillis)
    )

val PlaceEventDomainModel.toLocalDataModel: PlaceEventDataModel
    get() = PlaceEventDataModel(
        id = UUID.randomUUID(),
        placeName = this.placeName,
        isArrival = this.isArrival,
        timestamp = this.timestamp.toEpochMilli()
   )

val PlaceEventDataModel.toDomainModel: PlaceEventDomainModel
    get() = PlaceEventDomainModel(
        placeName = this.placeName,
        isArrival = this.isArrival,
        timestamp = Instant.ofEpochMilli(this.timestamp)
    )
