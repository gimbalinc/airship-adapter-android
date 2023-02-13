package com.gimbal.airship.sample.domain

import kotlinx.coroutines.flow.Flow

interface PlaceEventRepository {
    fun startAirship()
    fun getPlaceEvents(): Flow<List<PlaceEventDomainModel>>
    suspend fun clearPlaceEvents()
}