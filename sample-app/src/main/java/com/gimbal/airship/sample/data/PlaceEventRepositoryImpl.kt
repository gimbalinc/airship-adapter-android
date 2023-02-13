package com.gimbal.airship.sample.data

import com.gimbal.airship.AirshipAdapter
import com.gimbal.airship.sample.domain.PlaceEventRepository
import com.gimbal.airship.sample.mapper.toDomainModel
import com.gimbal.airship.sample.mapper.toLocalDataModel
import com.gimbal.android.Visit
import com.urbanairship.analytics.CustomEvent
import com.urbanairship.analytics.location.RegionEvent
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class PlaceEventRepositoryImpl @Inject constructor(
    private val airship: AirshipAdapter,
    private val placeEventDao: PlaceEventDao
) : PlaceEventRepository {
    override fun startAirship() {
        airship.setShouldTrackCustomEntryEvent(true)
        airship.setShouldTrackCustomExitEvent(true)
        airship.start("[YOUR AIRSHIP KEY]")
        airship.restore()
        airship.addListener(object : AirshipAdapter.Listener {
            override fun onRegionEntered(event: RegionEvent, visit: Visit) {
                Timber.i("region entered")
                placeEventDao.insertPlaceEvent(visit.toLocalDataModel)
            }

            override fun onRegionExited(event: RegionEvent, visit: Visit) {
                Timber.i("region exited")
                placeEventDao.insertPlaceEvent(visit.toLocalDataModel)
            }

            override fun onCustomRegionEntry(event: CustomEvent, visit: Visit) {
                Timber.i("custom region entered")
                placeEventDao.insertPlaceEvent(visit.toLocalDataModel)
            }

            override fun onCustomRegionExit(event: CustomEvent, visit: Visit) {
                Timber.i("custom region exited")
                placeEventDao.insertPlaceEvent(visit.toLocalDataModel)
            }
        })
    }

    override fun getPlaceEvents() = placeEventDao.getPlaceEvents().map { placeEvents ->
        placeEvents.map {
            it.toDomainModel
        }
    }

    override suspend fun clearPlaceEvents() {
        placeEventDao.deleteAllPlaceEvents()
    }
}