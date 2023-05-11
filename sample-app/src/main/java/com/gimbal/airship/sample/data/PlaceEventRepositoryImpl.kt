package com.gimbal.airship.sample.data

import com.gimbal.airship.sample.domain.PlaceEventDomainModel
import com.gimbal.airship.sample.domain.PlaceEventRepository
import com.gimbal.airship.sample.mapper.toDomainModel
import com.gimbal.airship.sample.mapper.toLocalDataModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaceEventRepositoryImpl @Inject constructor(
    private val placeEventDao: PlaceEventDao,
) : PlaceEventRepository {

    override fun getPlaceEvents() = placeEventDao.getPlaceEvents().map { placeEvents ->
        placeEvents.map {
            it.toDomainModel
        }
    }

    override fun addPlaceEvent(placeEvent: PlaceEventDomainModel) {
        placeEventDao.insertPlaceEvent(placeEvent.toLocalDataModel)
    }

    override suspend fun clearPlaceEvents() {
        placeEventDao.deleteAllPlaceEvents()
    }
}
