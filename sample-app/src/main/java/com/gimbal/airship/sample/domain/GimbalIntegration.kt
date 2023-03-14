package com.gimbal.airship.sample.domain

import com.gimbal.airship.AirshipAdapter
import com.urbanairship.UAirship
import timber.log.Timber
import javax.inject.Inject

class GimbalIntegration @Inject constructor(
    private val airshipAdapter: AirshipAdapter,
) {

    fun enableGimbal() {
        UAirship.shared() {
            it.pushManager.userNotificationsEnabled = true
        }
        airshipAdapter.setShouldTrackCustomEntryEvent(true)
        airshipAdapter.setShouldTrackCustomExitEvent(true)
        airshipAdapter.start("[YOUR GIMBAL API KEY]")
        Timber.i("Enabling Gimbal place monitoring w/ Airship custom events")
    }
}
