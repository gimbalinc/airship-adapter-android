package com.gimbal.airship.sample.domain

import kotlinx.coroutines.flow.Flow

interface PlaceEventRepository {
    fun getPlaceEvents(): Flow<List<PlaceEventDomainModel>>
    fun addPlaceEvent(placeEvent: PlaceEventDomainModel)
    suspend fun clearPlaceEvents()
}
