package com.gimbal.airship.sample.mapper

import com.gimbal.airship.sample.data.PlaceEventDataModel
import com.gimbal.airship.sample.domain.PlaceEventDomainModel
import com.gimbal.android.Visit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Visit.toLocalDataModel: PlaceEventDataModel
    get() = PlaceEventDataModel(
        id = "$arrivalTimeInMillis$departureTimeInMillis",
        place = this.place.name,
        arrivalTimeInMillis = this.arrivalTimeInMillis,
        departureTimeInMillis = this.departureTimeInMillis
    )

val PlaceEventDataModel.toDomainModel: PlaceEventDomainModel
    get() = PlaceEventDomainModel(
        place = this.place,
        isArrival = this.departureTimeInMillis == 0L,
        time = if (this.departureTimeInMillis == 0L)
            this.arrivalTimeInMillis.toDateString
        else this.departureTimeInMillis.toDateString
    )

val Long.toDateString: String
    get() {
        val formatter = SimpleDateFormat("M/dd/yy, h:mm a", Locale.getDefault())
        return formatter.format(Date(this))
    }