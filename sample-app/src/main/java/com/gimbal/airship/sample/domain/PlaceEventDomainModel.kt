package com.gimbal.airship.sample.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class PlaceEventDomainModel(
    val placeName: String,
    val isArrival: Boolean,
    val timestamp: Instant
) {
    val formattedTime: String
        get() = formatter.format(timestamp)

    companion object {
        var formatter: DateTimeFormatter = DateTimeFormatter
            .ofPattern("MM/dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
    }
}
