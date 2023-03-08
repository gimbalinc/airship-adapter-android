package com.gimbal.airship.sample.mapper

import com.gimbal.airship.sample.data.PlaceEventDataModel
import com.gimbal.airship.sample.domain.PlaceEventDomainModel
import com.gimbal.android.Visit
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

val Visit.toDomainModel: PlaceEventDomainModel
    get() = PlaceEventDomainModel(
        placeName = this.place.name,
        isArrival = this.departureTimeInMillis == 0L,
        time = if (this.departureTimeInMillis == 0L)
            Instant.ofEpochMilli(this.arrivalTimeInMillis)
        else Instant.ofEpochMilli(this.departureTimeInMillis)
    )

val PlaceEventDomainModel.toLocalDataModel: PlaceEventDataModel
    get() = PlaceEventDataModel(
        id = "$placeName-${time.toEpochMilli()}-$isArrival",
        placeName = this.placeName,
        arrivalTimeInMillis = if (this.isArrival) this.time.toEpochMilli()
            else 0,
        departureTimeInMillis = if (this.isArrival) 0
            else this.time.toEpochMilli()
    )

val PlaceEventDataModel.toDomainModel: PlaceEventDomainModel
    get() = PlaceEventDomainModel(
        placeName = this.placeName,
        isArrival = this.departureTimeInMillis == 0L,
        time = if (this.departureTimeInMillis == 0L)
            Instant.ofEpochMilli(this.arrivalTimeInMillis)
        else Instant.ofEpochMilli(this.departureTimeInMillis)
    )
