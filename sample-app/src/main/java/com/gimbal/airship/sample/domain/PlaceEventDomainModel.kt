package com.gimbal.airship.sample.domain

data class PlaceEventDomainModel(
    val place: String,
    val isArrival: Boolean,
    val time: String,
)