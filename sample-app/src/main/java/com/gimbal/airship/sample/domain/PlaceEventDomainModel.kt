package com.gimbal.airship.sample.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class PlaceEventDomainModel(
    val placeName: String,
    val isArrival: Boolean,
    val time: Instant
) {
    val formattedTime: String
        get() = formatter.format(time)

    companion object {
        var formatter: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd hh:mm:ss")
            .withZone(ZoneId.systemDefault())
    }
}
