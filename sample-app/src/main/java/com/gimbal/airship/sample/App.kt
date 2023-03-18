package com.gimbal.airship.sample

import android.app.Application
import com.gimbal.airship.AirshipAdapter
import com.gimbal.airship.sample.domain.PlaceEventRepository
import com.gimbal.airship.sample.mapper.toDomainModel
import com.gimbal.android.Visit
import com.urbanairship.analytics.CustomEvent
import com.urbanairship.analytics.location.RegionEvent
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var airshipAdapter: AirshipAdapter
    @Inject lateinit var placeEventRepository: PlaceEventRepository

    override fun onCreate() {
        super.onCreate()

        initTimber()

        airshipAdapter.addListener(object : AirshipAdapter.Listener {
            override fun onRegionEntered(event: RegionEvent, visit: Visit) {
                Timber.i("region entered")
                placeEventRepository.addPlaceEvent(visit.toDomainModel)
            }

            override fun onRegionExited(event: RegionEvent, visit: Visit) {
                Timber.i("region exited")
                placeEventRepository.addPlaceEvent(visit.toDomainModel)
            }

            override fun onCustomRegionEntry(event: CustomEvent, visit: Visit) {
                Timber.i("custom region entered")
                placeEventRepository.addPlaceEvent(visit.toDomainModel)
            }

            override fun onCustomRegionExit(event: CustomEvent, visit: Visit) {
                Timber.i("custom region exited")
                placeEventRepository.addPlaceEvent(visit.toDomainModel)
            }
        })
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
